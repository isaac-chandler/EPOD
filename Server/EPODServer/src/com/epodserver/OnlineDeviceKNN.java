package com.epodserver;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.function.IntBinaryOperator;

public class OnlineDeviceKNN {

	private static final int CELL_COUNT = 10;

	private OnlineDevice[][] deviceLocationDevices;
	private double[][] deviceLocations;
	private int[] nextDeviceIndex;
	private double[] nearestDist = new double[10];
	private int[] nearest = new int[20];

	public OnlineDeviceKNN(int initialCapacity) {
		deviceLocationDevices = new OnlineDevice[CELL_COUNT * CELL_COUNT * CELL_COUNT][initialCapacity];
		deviceLocations = new double[CELL_COUNT * CELL_COUNT * CELL_COUNT][3 * initialCapacity];
		nextDeviceIndex = new int[CELL_COUNT * CELL_COUNT * CELL_COUNT];
	}

	private static double distSq(double[] points, int a, int b) {
		double dx = points[a] - points[b];
		double dy = points[a + 1] - points[b + 1];
		double dz = points[a + 2] - points[b + 2];

		return dx * dx + dy * dy + dz * dz;
	}

	private static int getIndex(double value) {
		return Math.max(Math.min((int) ((value + 1) * 0.5 / CELL_COUNT), CELL_COUNT - 1), 0);
	}

	public OnlineDevice createDevice(long deviceId, long timestamp, double latitude, double longitude) {
		double cosLat = Math.cos(latitude * (Math.PI / 180));

		double x = cosLat * Math.cos(longitude * (Math.PI / 180));
		double y = cosLat * Math.sin(longitude * (Math.PI / 180));
		double z = Math.sin(latitude * (Math.PI / 180));

		int cell = getIndex(z) * CELL_COUNT * CELL_COUNT + getIndex(y) * CELL_COUNT + getIndex(x);

		if (nextDeviceIndex[cell] >= deviceLocationDevices[cell].length) {
			deviceLocationDevices[cell] = Arrays.copyOf(deviceLocationDevices[cell], deviceLocationDevices[cell].length * 2);

			deviceLocations[cell] = Arrays.copyOf(deviceLocations[cell], deviceLocations[cell].length * 2);
		}

		int index = nextDeviceIndex[cell]++;

		OnlineDevice device = new OnlineDevice(deviceLocations, cell, index, deviceId, timestamp, latitude, longitude);
		deviceLocationDevices[cell][index] = device;


		return device;
	}

	private static double distSq(double[][] points, int aCell, int a, int bCell, int b) {
		double[] aArr = points[aCell];
		double[] bArr = points[bCell];

		double dx = aArr[a] - bArr[b];
		double dy = aArr[a + 1] - bArr[b + 1];
		double dz = aArr[a + 2] - bArr[b + 2];

		return dx * dx + dy * dy + dz * dz;
	}

	public void removeDevice(OnlineDevice device) {
		int cell = device.getCell();
		int index = device.getIndex();
		nextDeviceIndex[cell]--;

		if (index != nextDeviceIndex[cell]) {
			deviceLocationDevices[cell][index] = deviceLocationDevices[cell][nextDeviceIndex[cell]];
			deviceLocationDevices[cell][index].setIndex(index);
			deviceLocations[cell][index * 3] = deviceLocations[cell][nextDeviceIndex[cell] * 3];
			deviceLocations[cell][index * 3 + 1] = deviceLocations[cell][nextDeviceIndex[cell] * 3 + 1];
			deviceLocations[cell][index * 3 + 2] = deviceLocations[cell][nextDeviceIndex[cell] * 3 + 2];
		}

		deviceLocationDevices[cell][nextDeviceIndex[cell]] = null;
	}

	public void moveDevice(OnlineDevice device) {
		int cell = device.getCell();
		int index = device.getIndex();

		int newCell = getIndex(deviceLocations[cell][index * 3 + 2]) * CELL_COUNT * CELL_COUNT +
				getIndex(deviceLocations[cell][index * 3 + 1]) * CELL_COUNT +
				getIndex(deviceLocations[cell][index * 3]);

		if (cell != newCell) {
			removeDevice(device);

			if (nextDeviceIndex[newCell] >= deviceLocationDevices[newCell].length) {
				deviceLocationDevices[newCell] = Arrays.copyOf(deviceLocationDevices[newCell], deviceLocationDevices[newCell].length * 2);

				deviceLocations[newCell] = Arrays.copyOf(deviceLocations[newCell], deviceLocations[newCell].length * 2);
			}

			int newIndex = nextDeviceIndex[newCell]++;

			device.setIndex(newIndex);
			device.setCell(newCell);
			deviceLocationDevices[newCell][newIndex] = device;
			System.arraycopy(deviceLocations[cell], index * 3, deviceLocations[newCell], newIndex * 3, 3);
		}
	}


	Deque<Integer> searchCells = new ArrayDeque<>();
	HashSet<Integer> searchedCells = new HashSet<>();


	public boolean knn(OnlineDevice device, OnlineDevice[] nearestDevices) {
		if (nearestDevices.length != nearestDist.length) {
			nearest = new int[nearestDevices.length * 2];
			nearestDist = new double[nearestDevices.length];
		}

		searchCells.clear();
		searchedCells.clear();

		final int pointCell = device.getCell();
		int cellZ = pointCell / 100;
		int cellY = (pointCell / 10) % 10;
		int cellX = pointCell % 10;


		int point = device.getIndex();

		int totalPoints = 0;
		loop:
		for (int i = 0; i < CELL_COUNT; i++) {
			int fromZ = Math.max(cellZ - i, 0), toZ = Math.min(cellZ + i, CELL_COUNT - 1);
			int fromY = Math.max(cellY - i, 0), toY = Math.min(cellY + i, CELL_COUNT - 1);
			int fromX = Math.max(cellX - i, 0), toX = Math.min(cellX + i, CELL_COUNT - 1);
			for (int z = fromZ; z <= toZ; z++) {
				for (int y = fromY; y <= toY; y++) {
					for (int x = fromX; x <= toX; x++) {
						if (z == fromZ || z == toZ || y == fromY || y == toY || x == fromX || x == toX) {
							int idx = z * CELL_COUNT * CELL_COUNT + y * CELL_COUNT + x;
							if (nextDeviceIndex[idx] != 0) {
								searchCells.addFirst(idx);
								if ((totalPoints += nextDeviceIndex[idx]) > nearestDist.length)
									break loop;
							}
						}
					}
				}
			}

			return false;
		}

		int searching = searchCells.getLast();

		int i = 0;

		for (int j = 0; j < nearest.length; j += 2) {
			if (i == nextDeviceIndex[searching] * 3 && j != nearest.length - 2) {
				i = 0;
				searchedCells.add(searchCells.removeLast());
				searching = searchCells.getLast();
			}

			if (searching == pointCell && i == point && (i += 3) == nextDeviceIndex[searching] * 3 && j != nearest.length - 2) {
				i = 0;
				searchedCells.add(searchCells.removeLast());
				searching = searchCells.getLast();
			}

			nearest[j] = searching;
			nearest[j + 1] = i;
			i += 3;


		}

		for (int a = 0; a < nearest.length; a += 2) {
			for (int b = 2; b < nearest.length - a; b += 2) {
				if (distSq(deviceLocations, pointCell, point, nearest[b - 2], nearest[b - 1]) < distSq(deviceLocations, pointCell, point, nearest[b], nearest[b + 1])) {
					int temp = nearest[b - 2];
					nearest[b - 2] = nearest[b];
					nearest[b] = temp;

					temp = nearest[b - 1];
					nearest[b - 1] = nearest[b + 1];
					nearest[b + 1] = temp;
				}
			}
		}

		Arrays.setAll(nearestDist, idx -> distSq(deviceLocations, pointCell, point, nearest[idx * 2], nearest[idx * 2 + 1]));

		while (!searchCells.isEmpty()) {
			searching = searchCells.removeLast();

			for (; i < nextDeviceIndex[searching] * 3; i += 3) {
				if (searching == pointCell && i == point)
					continue;


				double dist = distSq(deviceLocations, pointCell, point, searching, i);

				if (dist >= nearestDist[0])
					continue;
				for (int j = 1; j < nearestDist.length; j += 1) {
					int jj  = j * 2;
					if (dist < nearestDist[j]) {
						if (j == nearestDist.length - 1) {
							System.arraycopy(nearest, 2, nearest, 0, jj);
							System.arraycopy(nearestDist, 1, nearestDist, 0, j);
							nearest[jj] = searching;
							nearest[jj + 1] = i;
							nearestDist[j] = dist;
							break;
						}
					} else {
						System.arraycopy(nearest, 2, nearest, 0, jj - 2);
						System.arraycopy(nearestDist, 1, nearestDist, 0, j - 1);
						nearest[jj - 2] = searching;
						nearest[jj - 1] = i;
						nearestDist[j - 1] = dist;
						break;
					}
				}
			}

			i = 0;

			searchedCells.add(searching);
		}

		double dist = Math.sqrt(nearestDist[0]);

		int fromZ = getIndex(deviceLocations[nearest[0]][nearest[1] + 2] - dist),
				toZ = getIndex(deviceLocations[nearest[0]][nearest[1] + 2] + dist);
		int fromY = getIndex(deviceLocations[nearest[0]][nearest[1] + 1] - dist),
				toY = getIndex(deviceLocations[nearest[0]][nearest[1] + 1] + dist);
		int fromX = getIndex(deviceLocations[nearest[0]][nearest[1]] - dist),
				toX = getIndex(deviceLocations[nearest[0]][nearest[1]] + dist);

		for (int z = fromZ; z <= toZ; z++) {
			for (int y = fromY; y <= toY; y++) {
				for (int x = fromX; x <= toX; x++) {
					int idx = z * CELL_COUNT * CELL_COUNT + y * CELL_COUNT + x;
					if (nextDeviceIndex[idx] != 0 && !searchedCells.contains(idx))
						searchCells.addLast(idx);
				}
			}
		}

		while (!searchCells.isEmpty()) {
			i = 0;
			searching = searchCells.removeLast();

			for (; i < nextDeviceIndex[searching] * 3; i += 3) {
				if (searching == pointCell && i == point)
					continue;

				dist = distSq(deviceLocations, pointCell, point, searching, i);
				if (dist >= nearestDist[0])
					continue;
				for (int j = 1; j < nearestDist.length; j += 1) {
					int jj  = j * 2;
					if (dist < nearestDist[j]) {
						if (j == nearestDist.length - 1) {
							System.arraycopy(nearest, 2, nearest, 0, jj);
							System.arraycopy(nearestDist, 1, nearestDist, 0, j);
							nearest[jj] = searching;
							nearest[jj + 1] = i;
							nearestDist[j] = dist;
							break;
						}
					} else {
						System.arraycopy(nearest, 2, nearest, 0, jj - 2);
						System.arraycopy(nearestDist, 1, nearestDist, 0, j - 1);
						nearest[jj - 2] = searching;
						nearest[jj - 1] = i;
						nearestDist[j - 1] = dist;
						break;
					}
				}
			}
		}

		for (int k = 0; k < nearestDevices.length; k++) {
			nearestDevices[k] = deviceLocationDevices[nearest[k * 2]][nearest[k * 2 + 1] / 3];
		}

		return true;
	}

	public static class ArraySort {
		public static void sort(int[] array, IntBinaryOperator comp) {
			sort(array, 0, array.length, comp);
		}

		private static void sort(int array[], int offset, int length, IntBinaryOperator op) {
			if (length < 7) {
				for (int i = offset; i < length + offset; i++)

					for (int j = i; j > offset && (op.applyAsInt(array[j - 1], array[j]) > 0); j--)
						swap(array, j, j - 1);
				return;
			}

			int m = offset + (length >> 1);
			if (length > 7) {
				int l = offset;
				int n = offset + length - 1;
				if (length > 40) {
					int s = length / 8;
					l = med3(array, l, l + s, l + 2 * s);
					m = med3(array, m - s, m, m + s);
					n = med3(array, n - 2 * s, n - s, n);
				}
				m = med3(array, l, m, n);
			}

			int v = array[m];

			int a = offset, b = a, c = offset + length - 1, d = c;
			while (true) {

				while (b <= c && (op.applyAsInt(array[b], v) <= 0)) {
					if (array[b] == v)
						swap(array, a++, b);
					b++;
				}

				while (c >= b && (op.applyAsInt(array[c], v) >= 0)) {
					if (array[c] == v)
						swap(array, c, d--);
					c--;
				}
				if (b > c)
					break;
				swap(array, b++, c--);
			}

			int s, n = offset + length;
			s = Math.min(a - offset, b - a);
			multiswap(array, offset, b - s, s);
			s = Math.min(d - c, n - d - 1);
			multiswap(array, b, n - s, s);

			if ((s = b - a) > 1)
				sort(array, offset, s, op);
			if ((s = d - c) > 1)
				sort(array, n - s, s, op);
		}

		private static void swap(int array[], int a, int b) {
			int t = array[a];
			array[a] = array[b];
			array[b] = t;
		}

		private static void multiswap(int array[], int a, int b, int n) {
			for (int i = 0; i < n; i++, a++, b++)
				swap(array, a, b);
		}

		private static int med3(int array[], int a, int b, int c) {
			return (array[a] < array[b] ?
					(array[b] < array[c] ? b : array[a] < array[c] ? c : a) :
					(array[b] > array[c] ? b : array[a] > array[c] ? c : a));
		}

	}

}
