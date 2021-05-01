package com.epodserver;

public class AABB {
	public double minX, maxX, minY, maxY;

	public AABB(double minX, double maxX, double minY, double maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}
	
	public boolean intersects(AABB other) {
		return !(other.minX > maxX
				|| other.maxX < minX
				|| other.minY > maxY
				|| other.maxY < minY);
	}

	public boolean contains(OnlineDevice device) {
		return minX <= device.x && maxX >= device.x && minY <= device.y && maxY >= device.y;
	}

	public boolean contains(AABB other) {
		return other.minX >= minX && other.maxX <= maxX && other.minY >= minY && other.maxY <= maxY;
	}

	@Override
	public String toString() {
		return String.format("%f %f %f %f", minX, minY, maxX, maxY);
	}
}
