package com.epodserver;

import java.util.Arrays;

//replace this with a circular buffer or priority queue
public class TimeoutList {

//	public static void main(String[] args) {
//		double[][] device = new double[1][3];
//
//		TimeoutList list = new TimeoutList();
//
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 3, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 3, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 3, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 7, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 3, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//		list.add(new OnlineDevice(device, 0, 0, 0, 4, 0, 0));
//
//		for (int i = 0; i < list.size(); i++) {
//			System.out.println(i + " " + list.get(i).timestamp);
//		}
//
//		System.out.println(list.find(3));
//		System.out.println(list.find(4));
//	}

	private OnlineDevice[] array = new OnlineDevice[8];

	private int size = 0;

	public OnlineDevice get(int i) {
		return array[i];
	}

	public int size() {
		return size;
	}

	public void add(OnlineDevice device) {
		insert(find(device.getTimestamp()), device);
	}

	public int find(long timestamp) {
		int left = 0, right = size;

		while (left < right) {
			int mid = (left + right) >> 1;

			if (array[mid].getTimestamp() < timestamp) {
				right = mid;
			} else {
				left = mid + 1;
			}
		}

		while (left + 1 < size && array[left + 1].getTimestamp() == timestamp)
			left++;

		return left;
	}

	private void insert(int i, OnlineDevice device) {
		if (i >= size) {
			if (i >= array.length) {
				array = Arrays.copyOf(array, Math.max(array.length * 2, i + 1));
			}

			array[i] = device;
			size = i + 1;
		} else {
			if (size + 1 >= array.length) {
				array = Arrays.copyOf(array, Math.max(array.length * 2, size + 1));
			}

			System.arraycopy(array, i, array, i + 1, size - i);
			array[i] = device;
			size++;
		}
	}

	public void trim(int i) {
		Arrays.fill(array, i, size, null);
		size = i;
	}

	public void remove(OnlineDevice device) {
		for (int i = 0; i < size; i++)
			if (array[i] == device) {
				if (i != size - 1) {
					System.arraycopy(array, i + 1, array, i, size - i - 1);
				}

				size--;
				array[size] = null;

				return;
			}
	}
}
