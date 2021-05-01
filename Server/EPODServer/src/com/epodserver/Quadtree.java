package com.epodserver;

import java.util.Collection;

public class Quadtree {
	private static final int CAPACITY = 4;

	private OnlineDevice[] devices = new OnlineDevice[CAPACITY];
	private byte nextDeviceIndex = 0;

	private Quadtree nw, ne, sw, se;
	private AABB aabb;

	public void add(OnlineDevice device) {
		if (nextDeviceIndex < CAPACITY) {
			devices[nextDeviceIndex] = device;
			nextDeviceIndex++;
		} else {
			if (nw == null) {
				double centerX = (aabb.minX + aabb.maxX) * 0.5;
				double centerY = (aabb.minY + aabb.maxY) * 0.5;

				nw = new Quadtree(new AABB(aabb.minX, centerX, aabb.minY, centerY));
				ne = new Quadtree(new AABB(centerX, aabb.maxX, aabb.minY, centerY));
				sw = new Quadtree(new AABB(aabb.minX, centerX, centerY, aabb.maxY));
				se = new Quadtree(new AABB(centerX, aabb.maxX, centerY, aabb.maxY));
			}

			if (nw.aabb.contains(device)) nw.add(device);
			else if (ne.aabb.contains(device)) ne.add(device);
			else if (sw.aabb.contains(device)) sw.add(device);
			else if (se.aabb.contains(device)) se.add(device);
		}

	}

	public Quadtree(AABB aabb) {
		this.aabb = aabb;
	}

	public static void remove(OnlineDevice device) {
		if (device.quadtreeNode != null) {
			device.quadtreeNode.removeImpl(device);
			device.quadtreeNode = null;
		}
	}

	private void removeImpl(OnlineDevice device) {
		for (int i = 0; i < nextDeviceIndex; i++) {
			if (devices[i] == device) {
				nextDeviceIndex--;

				if (i != nextDeviceIndex) {
					devices[i] = devices[nextDeviceIndex];
				}

				devices[nextDeviceIndex + 1] = null;
				break;
			}
		}
	}

	public void getWithin(Collection<OnlineDevice> found, AABB bounds) {
		if (bounds.contains(aabb)) {
			addAll(found);
		} else {
			for (int i = 0; i < nextDeviceIndex; i++) {
				if (bounds.contains(devices[i]))
					found.add(devices[i]);
			}

			if (nw != null) {
				if (bounds.intersects(nw.aabb)) nw.getWithin(found, bounds);
				else if (bounds.intersects(ne.aabb)) ne.getWithin(found, bounds);
				else if (bounds.intersects(sw.aabb)) sw.getWithin(found, bounds);
				else if (bounds.intersects(se.aabb)) se.getWithin(found, bounds);
			}
		}
	}

	public void getWithin(Collection<OnlineDevice> found, AABB bounds, OnlineDevice.Status status) {
		if (bounds.contains(aabb)) {
			addAll(found, status);
		} else {
			for (int i = 0; i < nextDeviceIndex; i++) {
				if (devices[i].status == status && bounds.contains(devices[i]))
					found.add(devices[i]);
			}

			if (nw != null) {
				if (bounds.intersects(nw.aabb)) nw.getWithin(found, bounds, status);
				if (bounds.intersects(ne.aabb)) ne.getWithin(found, bounds, status);
				if (bounds.intersects(sw.aabb)) sw.getWithin(found, bounds, status);
				if (bounds.intersects(se.aabb)) se.getWithin(found, bounds, status);
			}
		}
	}

	private void addAll(Collection<OnlineDevice> found) {
		for (int i = 0; i < nextDeviceIndex; i++) {
			found.add(devices[i]);
		}

		if (nw != null) {
			nw.addAll(found);
			ne.addAll(found);
			sw.addAll(found);
			se.addAll(found);
		}
	}

	private void addAll(Collection<OnlineDevice> found, OnlineDevice.Status status) {
		for (int i = 0; i < nextDeviceIndex; i++) {
			if (devices[i].status == status)
				found.add(devices[i]);
		}

		if (nw != null) {
			nw.addAll(found, status);
			ne.addAll(found, status);
			sw.addAll(found, status);
			se.addAll(found, status);
		}
	}
}
