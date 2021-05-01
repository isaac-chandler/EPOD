package com.epodserver;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Random;
import java.util.function.IntBinaryOperator;

public class TestKNN {

	public static class ArraySort {
		public static void sort(int[] array, IntBinaryOperator comp) {
			sort(array, 0, array.length, comp);
		}

		private static void sort(int array[], int offset, int length, IntBinaryOperator op) {
			if (length < 7) {
				for (int i = offset; i < length + offset; i++)

					for (int j = i; j > offset && (op.applyAsInt(array[j - 1], array[j]) > 0); j--)
						swap(array, j, j-1);
				return;
			}

			int m = offset + (length >> 1);
			if (length > 7) {
				int l = offset;
				int n = offset + length - 1;
				if (length > 40) {
					int s = length/8;
					l = med3(array, l, l + s, l + 2 * s);
					m = med3(array, m - s,   m, m + s);
					n = med3(array, n - 2 * s, n - s, n);
				}
				m = med3(array, l, m, n);
			}

			int v = array[m];

			int a = offset, b = a, c = offset + length - 1, d = c;
			while(true) {

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
			s = Math.min(a - offset, b - a  );  multiswap(array, offset, b - s, s);
			s = Math.min(d - c,   n - d - 1);  multiswap(array, b,   n - s, s);

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

	private static double distSq(double[] points, int a, int b) {
		double dx = points[a ] - points[b];
		double dy = points[a + 1] - points[b + 1];
		double dz = points[a + 2] - points[b + 2];

		return dx * dx + dy * dy + dz * dz;
	}

	private interface knn {
		double run(double[][] rawPoints, int cycles, int k, Random random);
	}

	private static void runTest(double[][] points, int iterations, String name, knn method, Random random) {
//		System.out.printf("%s:%d\n", name, points.length);

		for (int i = 0; i < 5; i++) {
			double time = method.run(points, iterations, 10, random);
			System.out.printf("%2$.2e ", time, time / iterations);
		}

		System.out.println();
	}

	public static void main(String[] args) {
//		bench(10_000_000, 1_00_000, 10);

		Random random = new Random(new SecureRandom().nextLong());

		double[][] points100 = genPoints(100, random);
		double[][] points1000 = genPoints(1_000, random);
		double[][] points10000 = genPoints(10_000, random);
		double[][] points100000 = genPoints(100_000, random);
		double[][] points1000000 = genPoints(1_000_000, random);
		double[][] points10000000 = genPoints(10_000_000, random);

		runTest(points100, 2_000_000, "Object", TestKNN::KNNBasic, random);
		runTest(points1000, 1_000_000, "Object", TestKNN::KNNBasic, random);
		runTest(points10000, 100_000, "Object", TestKNN::KNNBasic, random);
		runTest(points100000, 10_000, "Object", TestKNN::KNNBasic, random);
		runTest(points1000000, 1_000, "Object", TestKNN::KNNBasic, random);
	}

	private static double[][] genPoints(int points, Random random) {
		double[][] rawPoints = new double[points][3];

		for (int i = 0; i < rawPoints.length; i++) {
			rawPoints[i][0] = random.nextDouble();
			rawPoints[i][1] = random.nextDouble();
			rawPoints[i][2] = random.nextDouble();
		}

		return rawPoints;
}

	private static void bench(int points, int cycles, int k) {
		Random random = new Random(0);
		long pointRandomSeed = random.nextInt();
		double[][] rawPoints = new double[points][3];

		for (int i = 0; i < rawPoints.length; i++) {
			rawPoints[i][0] = random.nextDouble();
			rawPoints[i][1] = random.nextDouble();
			rawPoints[i][2] = random.nextDouble();
		}

		KNNCell(rawPoints, cycles, k ,new Random(pointRandomSeed));
		KNNBasic(rawPoints, cycles, k, new Random(pointRandomSeed));
		KNNSorted(rawPoints, cycles, k, new Random(pointRandomSeed));
	}

	private static class Vec3 {
		public double x, y, z;

		public Vec3(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public double distSq(Vec3 other) {
			double dx = x - other.x, dy = y - other.y, dz = z - other.z;

			return dx * dx + dy * dy + dz * dz;
		}

		public double dist(Vec3 other) {
			return Math.sqrt(distSq(other));
		}
	}

	private static double KNNBad(double[][] rawPoints, int cycles, int k, Random random) {
		Arrays.sort(rawPoints, Comparator.comparingDouble(x -> x[0]));

		Vec3[] points = new Vec3[rawPoints.length];

		for (int i = 0; i < rawPoints.length; i++) {
			points[i] = new Vec3(rawPoints[i][0], rawPoints[i][1], rawPoints[i][2]);
		}

		long start = System.nanoTime();

		for (int aaa = 0; aaa < cycles; aaa++) {
			int[] nearest = new int[k];
			int point = random.nextInt(points.length);


			int i = 0;

			for (int j = 0; j < nearest.length; j++) {
				if (i == point) {
					i++;
				}

				nearest[j] = i;
				i++;
			}

			ArraySort.sort(nearest, (a, b) -> -Double.compare(points[a].distSq(points[point]), points[b].distSq(points[point])));
			double[] nearestDist = new double[nearest.length];
			Arrays.setAll(nearestDist, idx -> points[nearest[idx]].distSq(points[point]));

			for (; i < points.length; i++) {
				if (i == point)
					continue;


				double dist = points[i].distSq(points[point]);
				if (dist >= nearestDist[0])
					continue;
				for (int j = 1; j < nearest.length; j++) {
					if (dist < nearestDist[j]) {
						if (j == nearest.length - 1) {
							System.arraycopy(nearest, 1, nearest, 0, j);
							System.arraycopy(nearestDist, 1, nearestDist, 0, j);
							nearest[j] = i;
							nearestDist[j] = dist;
							break;
						}
					} else {
						System.arraycopy(nearest, 1, nearest, 0, j - 1);
						System.arraycopy(nearestDist, 1, nearestDist, 0, j - 1);
						nearest[j - 1] = i;
						nearestDist[j - 1] = dist;
						break;
					}
				}
			}
		}

		return (System.nanoTime() - start) / 1e9;
	}

	private static double KNNVeryBad(double[][] rawPoints, int cycles, int k, Random random) {
		Arrays.sort(rawPoints, Comparator.comparingDouble(x -> x[0]));

		Vec3[] points = new Vec3[rawPoints.length];

		long start = System.nanoTime();
		for (int i = 0; i < rawPoints.length; i++) {
			points[i] = new Vec3(rawPoints[i][0], rawPoints[i][1], rawPoints[i][2]);
		}


		for (int aaa = 0; aaa < cycles; aaa++) {
			int[] nearest = new int[k];
			int point = random.nextInt(points.length);


			int i = 0;

			for (int j = 0; j < nearest.length; j++) {
				if (i == point) {
					i++;
				}

				nearest[j] = i;
				i++;
			}

			ArraySort.sort(nearest, (a, b) -> -Double.compare(points[a].dist(points[point]), points[b].dist(points[point])));
			double[] nearestDist = new double[nearest.length];
			Arrays.setAll(nearestDist, idx -> points[nearest[idx]].dist(points[point]));

			for (; i < points.length; i++) {
				if (i == point)
					continue;


				double dist = points[i].dist(points[point]);
				if (dist >= nearestDist[0])
					continue;
				for (int j = 1; j < nearest.length; j++) {
					if (dist < nearestDist[j]) {
						if (j == nearest.length - 1) {
							System.arraycopy(nearest, 1, nearest, 0, j);
							System.arraycopy(nearestDist, 1, nearestDist, 0, j);
							nearest[j] = i;
							nearestDist[j] = dist;
							break;
						}
					} else {
						System.arraycopy(nearest, 1, nearest, 0, j - 1);
						System.arraycopy(nearestDist, 1, nearestDist, 0, j - 1);
						nearest[j - 1] = i;
						nearestDist[j - 1] = dist;
						break;
					}
				}
			}
		}

		return (System.nanoTime() - start) / 1e9;
	}

	private static double KNNBasic(double[][] rawPoints, int cycles, int k, Random random) {
		Arrays.sort(rawPoints, Comparator.comparingDouble(x -> x[0]));

		double[] points = new double[3 * rawPoints.length];

		for (int i = 0, j = 0; j < rawPoints.length; j++, i += 3) {
			System.arraycopy(rawPoints[j], 0, points, i, 3);
		}

		long start = System.nanoTime();

		for (int aaa = 0; aaa < cycles; aaa++) {
			int[] nearest = new int[k];
			int point = random.nextInt(points.length / 3) * 3;


			int i = 0;

			for (int j = 0; j < nearest.length; j++) {
				if (i == point) {
					i += 3;
				}

				nearest[j] = i;
				i += 3;
			}

			ArraySort.sort(nearest, (a, b) -> -Double.compare(distSq(points, a, point), distSq(points, a, point)));
			double[] nearestDist = new double[nearest.length];
			Arrays.setAll(nearestDist, idx -> distSq(points, nearest[idx], point));

			for (; i < points.length; i += 3) {
				if (i == point)
					continue;


				double dist = distSq(points, i, point);
				if (dist >= nearestDist[0])
					continue;
				for (int j = 1; j < nearest.length; j++) {
					if (dist < nearestDist[j]) {
						if (j == nearest.length - 1) {
							System.arraycopy(nearest, 1, nearest, 0, j);
							System.arraycopy(nearestDist, 1, nearestDist, 0, j);
							nearest[j] = i;
							nearestDist[j] = dist;
							break;
						}
					} else {
						System.arraycopy(nearest, 1, nearest, 0, j - 1);
						System.arraycopy(nearestDist, 1, nearestDist, 0, j - 1);
						nearest[j - 1] = i;
						nearestDist[j - 1] = dist;
						break;
					}
				}
			}
		}

		return (System.nanoTime() - start) / 1e9;
	}

	private static double KNNSorted(double[][] rawPoints, int cycles, int k, Random random) {
		Arrays.sort(rawPoints, Comparator.comparingDouble(x -> x[0]));

		double[] points = new double[3 * rawPoints.length];

		for (int i = 0, j = 0; j < rawPoints.length; j++, i += 3) {
			System.arraycopy(rawPoints[j], 0, points, i, 3);
		}

		long start = System.nanoTime();

		for (int aaa = 0; aaa < cycles; aaa++) {
			int[] nearest = new int[k];
			int point = random.nextInt(points.length / 3) * 3;

			int i = 0;

			for (int j = 0; j < nearest.length; j++) {
				if (i == point) {
					i += 3;
				}

				nearest[j] = i;
				i += 3;
			}

			ArraySort.sort(nearest, (a, b) -> -Double.compare(distSq(points, a, point), distSq(points, a, point)));
			double[] nearestDist = new double[nearest.length];
			Arrays.setAll(nearestDist, idx -> distSq(points, nearest[idx], point));


			int limit = points.length;

			for (; i < limit; i += 3) {
				if (i == point)
					continue;


				double dist = distSq(points, i, point);
				if (dist > nearestDist[0])
					continue;


				for (int j = 1; j < nearest.length; j++) {
					if (dist < nearestDist[j]) {
						if (j == nearest.length - 1) {
							System.arraycopy(nearest, 1, nearest, 0, j);
							System.arraycopy(nearestDist, 1, nearestDist, 0, j);
							nearest[j] = i;
							nearestDist[j] = dist;

							int d = 49152;
							while (d >= 48) {

								if (limit > i + d) {
									double dx = points[limit - d] - points[point];
									if (dx * dx > nearestDist[0]) {
										limit -= d;
									}
								}

								d >>= 1;
							}
							break;
						}
					} else {
						System.arraycopy(nearest, 1, nearest, 0, j - 1);
						System.arraycopy(nearestDist, 1, nearestDist, 0, j - 1);
						nearest[j - 1] = i;
						nearestDist[j - 1] = dist;

						int d = 49152;
						while (d >= 48) {

							if (limit > i + d) {
								double dx = points[limit - d] - points[point];
								if (dx * dx > nearestDist[0]) {
									limit -= d;
								}
							}

							d >>= 1;
						}
						break;
					}
				}
			}
		}

		return (System.nanoTime() - start) / 1e9;
	}

	public static double KNNCell(double[][] rawPoints, int cycles, int k, Random random) {
		final int size = 10;

		Arrays.sort(rawPoints, Comparator.comparingDouble(x -> x[0]));

		double[][] points = new double[size * size * size][30];
		int[] count = new int[size * size * size];

		for (int i = 0, j = 0; j < rawPoints.length; j++, i += 3) {
			int x = Math.min((int) (rawPoints[j][0] * size), size - 1);
			int y = Math.min((int) (rawPoints[j][1] * size), size - 1);
			int z = Math.min((int) (rawPoints[j][2] * size), size - 1);

			int index = z * size * size + y * size + x;

			int sub = count[index]++ * 3;
			if (sub * 3 >= points[index].length) {
				points[index] = Arrays.copyOf(points[index], points[index].length * 2);
			}

			System.arraycopy(rawPoints[j], 0, points[index], sub, 3);
		}

		long start = System.nanoTime();


		Deque<Integer> searchCells = new ArrayDeque<>();
		HashSet<Integer> searchedCells = new HashSet<>();

		for (int aaa = 0; aaa < cycles; aaa++) {
			searchCells.clear();
			searchedCells.clear();
			int[] nearest = new int[k * 2];

			int cellX, cellY, cellZ, _cell;
			do {
				cellX = random.nextInt(size);
				cellY = random.nextInt(size);
				cellZ = random.nextInt(size);
				_cell = cellZ * size * size + cellY * size + cellX;
			} while (count[_cell] == 0);

			final int pointCell = _cell;

			int point = random.nextInt(count[pointCell]) * 3;

			int totalPoints = 0;
			loop:
			for (int i = 0; i < size; i++) {
				int fromZ = Math.max(cellZ - i, 0), toZ = Math.min(cellZ + i, size - 1);
				int fromY = Math.max(cellY - i, 0), toY = Math.min(cellY + i, size - 1);
				int fromX = Math.max(cellX - i, 0), toX = Math.min(cellX + i, size - 1);
				for (int z = fromZ; z <= toZ; z++) {
					for (int y = fromY; y <= toY; y++) {
						for (int x = fromX; x <= toX; x++) {
							if (z == fromZ || z == toZ || y == fromY || y == toY || x == fromX || x == toX) {
								int idx = z * size * size + y * size + x;
								searchCells.addFirst(idx);
								if ((totalPoints += count[idx]) > k)
									break loop;
							}
						}
					}
				}
			}

			int searching = searchCells.getLast();

			int i = 0;

			for (int j = 0; j < nearest.length; j += 2) {
				if (i == count[searching] * 3 && j != nearest.length - 2) {
					i = 0;
					searchedCells.add(searchCells.removeLast());
					searching = searchCells.getLast();
				}

				if (searching == pointCell && i == point && (i += 3) == count[searching] * 3 && j != nearest.length - 2) {
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
					if (distSq(points, pointCell, point, nearest[b - 2], nearest[b - 1]) < distSq(points, pointCell, point, nearest[b], nearest[b + 1])) {
						int temp = nearest[b - 2];
						nearest[b - 2] = nearest[b];
						nearest[b] = temp;

						temp = nearest[b - 1];
						nearest[b - 1] = nearest[b + 1];
						nearest[b + 1] = temp;
					}
				}
			}

			double[] nearestDist = new double[k];
			Arrays.setAll(nearestDist, idx -> distSq(points, pointCell, point, nearest[idx * 2], nearest[idx * 2 + 1]));

			while (!searchCells.isEmpty()) {
				searching = searchCells.removeLast();

				for (; i < count[searching] * 3; i += 3) {
					if (searching == pointCell && i == point)
						continue;


					double dist = distSq(points, pointCell, point, searching, i);

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

			int fromZ = Math.max(Math.min((int) ((points[nearest[0]][nearest[1] + 2] - dist) * size), size - 1), 0),
					toZ = Math.max(Math.min((int) ((points[nearest[0]][nearest[1] + 2] + dist) * size), size - 1), 0);
			int fromY = Math.max(Math.min((int) ((points[nearest[0]][nearest[1] + 1] - dist) * size), size - 1), 0),
					toY = Math.max(Math.min((int) ((points[nearest[0]][nearest[1] + 1] + dist) * size), size - 1), 0);
			int fromX = Math.max(Math.min((int) ((points[nearest[0]][nearest[1]] - dist) * size), size - 1), 0),
					toX = Math.max(Math.min((int) ((points[nearest[0]][nearest[1]] + dist) * size), size - 1), 0);

			for (int z = fromZ; z <= toZ; z++) {
				for (int y = fromY; y <= toY; y++) {
					for (int x = fromX; x <= toX; x++) {
						int idx = z * size * size + y * size + x;
						if (!searchedCells.contains(idx))
							searchCells.addLast(idx);
					}
				}
			}

			while (!searchCells.isEmpty()) {
				i = 0;
				searching = searchCells.removeLast();

				for (; i < count[searching] * 3; i += 3) {
					if (searching == pointCell && i == point)
						continue;

					dist = distSq(points, pointCell, point, searching, i);
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

		}

		return (System.nanoTime() - start) / 1e9;
	}

	private static double distSq(double[][] points, int aCell, int a, int bCell, int b) {
		double[] aArr = points[aCell];
		double[] bArr = points[bCell];

		double dx = aArr[a] - bArr[b];
		double dy = aArr[a + 1] - bArr[b + 1];
		double dz = aArr[a + 2] - bArr[b + 2];

		return dx * dx + dy * dy + dz * dz;
	}
}
