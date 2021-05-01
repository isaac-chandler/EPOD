package com.epodserver.test;

import com.epodserver.AABB;
import com.epodserver.Log;
import com.epodserver.ServerMain;
import com.epodserver.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestThread extends Thread {

	private volatile boolean run = true;


	private TestDevice[] devices;

	public TestThread(HashMap<Long, User> users, int densityDivisor) {
		Random random = new Random();

		devices = new TestDevice[users.size() / densityDivisor];
		int i = 0;

		for (Map.Entry<Long, User> entry : users.entrySet()) {
			devices[i++] = new TestDevice(entry.getKey(), entry.getValue(), random);

			if (i >= devices.length)
				break;
		}
	}

	@Override
	public void run() {
		System.out.println("Starting test with " + devices.length + " users");
		Random random = new Random();

		long lastTime = System.nanoTime();
		AABB currentOutage = null;
		int currentDevice = 0;

		long nextOutage = lastTime + 120_000_000_000L;
		while (run) {
			long time = System.nanoTime();

			if (time - lastTime >= 60_000_000_000L / devices.length) {
				lastTime = time;

				if (time > nextOutage) {
					if (currentOutage != null) {
						Log.write("%d|outage stopped\n", time);
					}

					TestDevice center = devices[random.nextInt(devices.length)];

					double size = random.nextDouble() * 0.135 + 0.0045;

					currentOutage = new AABB(center.longitude - size, center.longitude + size, center.latitude - size, center.latitude + size);
					Log.write("%d|outage started|%.5f|%.5f|%.5f|%.5f\n", time, currentOutage.minY, currentOutage.minX, currentOutage.maxY, currentOutage.maxX);

					nextOutage = time + 300_000_000_000L;
				}

				devices[currentDevice++].process(time, currentOutage, random);

				if (currentDevice >= devices.length)
					currentDevice = 0;
			}
		}

		System.out.println("Stopping test");
	}

	public void done() {
		run = false;
	}

	private static class TestDevice {
		private final long userId;

		private final boolean isOutageIndependent;

		private final double latitude, longitude;
		private Status status = Status.ON;
		private long startupTime = -1;

		public TestDevice(long id, User user, Random random) {
			this.latitude = user.latitude;
			this.longitude = user.longitude;
			this.userId = id;
			this.isOutageIndependent = random.nextInt(50) == 0;
		}

		private boolean isInOutage(AABB outage) {
			return outage != null && longitude >= outage.minX && longitude <= outage.maxX && latitude >= outage.minY && latitude <= outage.maxY;
		}

		public void process(long time, AABB outage, Random random) {
			if (status == Status.OFF) {
				if (!isInOutage(outage)) {
					if (startupTime == -1) {
						startupTime = time + random.nextLong() & 0x1fffffffffL;
					} else if (time > startupTime) {
						status = Status.ON;
						startupTime = -1;
					}
				}
			}

			if (status == Status.ON) {
				if ((!isOutageIndependent && isInOutage(outage)) || random.nextInt(500) == 0) {
					status = Status.OFF;
				} else if (random.nextInt(40) != 0) {
					ServerMain.connectionManager.simulatePing(userId);
				}
			}
		}

		private enum Status {
			ON, OFF
		}
	}
}
