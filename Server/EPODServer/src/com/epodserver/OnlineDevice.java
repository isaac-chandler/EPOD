package com.epodserver;

public class OnlineDevice {

	private final long deviceId;
	public Status status = Status.ONLINE;
	public long timestamp;
	public double x, y;
	public double latitude, longitude;
	public Quadtree quadtreeNode;
	private double[][] deviceLocations;
	private int cell, index;

	public OnlineDevice(double[][] deviceLocations, int cell, int index, long deviceId, long timestamp, double latitude, double longitude) {
		this.cell = cell;
		this.index = index;
		this.deviceLocations = deviceLocations;
		this.deviceId = deviceId;
		this.timestamp = timestamp;

		setPolar(latitude, longitude);
	}

	public boolean setPolar(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;

		latitude *= MathUtil.TO_RADIANS;
		longitude *= MathUtil.TO_RADIANS;

		double cosLat = Math.cos(latitude);

		double newX = (longitude + Math.PI) / (2.0 * Math.PI);
		double newY = (1.0 - Math.log(Math.tan(latitude) + (1.0 / cosLat)) / Math.PI) * 0.5;

		boolean change = newX != this.x || newY != this.y;
		this.x = newX;
		this.y = newY;

		deviceLocations[cell][index * 3] = cosLat * Math.cos(longitude);
		deviceLocations[cell][index * 3 + 1] = cosLat * Math.sin(longitude);
		deviceLocations[cell][index * 3 + 2] = Math.sin(latitude);

		return change;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getCell() {
		return cell;
	}

	public void setCell(int cell) {
		this.cell = cell;
	}

	public double dist(OnlineDevice device) {
		double dx = deviceLocations[cell][index * 3] - device.deviceLocations[device.cell][device.index * 3];
		double dy = deviceLocations[cell][index * 3 + 1] - device.deviceLocations[device.cell][device.index * 3 + 1];
		double dz = deviceLocations[cell][index * 3 + 2] - device.deviceLocations[device.cell][device.index * 3 + 2];

		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof OnlineDevice && deviceId == ((OnlineDevice) obj).deviceId;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(deviceId);
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return String.format("%016X", deviceId);
	}

	public long getDeviceId() {
		return deviceId;
	}

	public enum Status {
		ONLINE(OutageDetector.ONLINE_WEIGHT),
		LOST(OutageDetector.LOST_WEIGHT),
		OUTAGE(OutageDetector.OUTAGE_WEIGHT);

		public double weight;

		Status(double weight) {
			this.weight = weight;
		}
	}
}
