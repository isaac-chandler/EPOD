package com.epodserver;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OutageDetector extends Thread {
//	public static final int OUTAGE_START_THRESHOLD = 0;
//	public static final int OUTAGE_STOP_THRESHOLD = 8;
//	public static final int ONLINE_WEIGHT = -5;
//	public static final int LOST_WEIGHT = 2;
//	public static final int OUTAGE_WEIGHT = 4;
//	public static final int NEIGHBOR_COUNT = 10;

	public static final double OUTAGE_START_THRESHOLD = 0.3;
	public static final double OUTAGE_STOP_THRESHOLD = 7;
	public static final double ONLINE_WEIGHT = 1;
	public static final double LOST_WEIGHT = 1;
	public static final double OUTAGE_WEIGHT = 3;
	public static final int NEIGHBOR_COUNT = 10;
	public static final long LOST_TIMEOUT_TIME = 300;
	public static final long CONNECTION_TIMEOUT_TIME = 150;

	public static final long MANUAL_REPORT_VERIFY_TIME = 300;


	public HashMap<Long, OnlineDevice> devices = new HashMap<>();
	private BlockingQueue<Request> updates = new LinkedBlockingQueue<>();
	private TimeoutList timingOut = new TimeoutList();
	private Quadtree quadtree = new Quadtree(new AABB(0, 1, 0, 1));
	private OnlineDeviceKNN deviceKNN = new OnlineDeviceKNN(1000);
	private volatile boolean running = true;
	private OnlineDevice[] nearetDevices = new OnlineDevice[NEIGHBOR_COUNT];

	private Deque<OnlineDevice> manualReportsToVerify = new ArrayDeque<>();

	private long lastCloggingCheck = 0;

	private void checkClogging() {
		if (System.currentTimeMillis() - lastCloggingCheck > 1000) {
			lastCloggingCheck = System.currentTimeMillis();

			if (updates.size() > 10) {
				System.out.println("Update queue clogged: " + updates.size());
			}
		}
	}

	public void queueUpdate(Request update) {
		if (update.pending.getAndSet(true))
			return;

		updates.add(update);

		checkClogging();
	}

	@Override
	public void run() {
		System.out.println("Successfully laucnched outage detector");

		while (running) {
			try {

				Request update = updates.poll(100, TimeUnit.MILLISECONDS);

				int index = timingOut.find(Time.getSeconds() - LOST_TIMEOUT_TIME);

				for (int i = index; i < timingOut.size(); i++) {
					timedOut(timingOut.get(i));
				}

				timingOut.trim(index);

				{
					OnlineDevice manualDevice = manualReportsToVerify.peekFirst();
					while (manualDevice.timestamp - Time.getSeconds() > MANUAL_REPORT_VERIFY_TIME) {
						System.out.println("Verifying manual report");
						manualReportsToVerify.removeFirst();

						processOutageStop(manualDevice);

						manualDevice = manualReportsToVerify.peekFirst();
					}
				}

				if (update != null) {
					if (update.type == Request.Type.DEVICE_STATUS_UPDATE) {
						OnlineDevice device = devices.get(update.deviceId);

						if (Math.abs(update.latitude) > 85 || Math.abs(update.longitude) > 180) { // too large for our system to deal with, kill it with fire
							if (device != null) {
								remove(device);
							}
							return;
						}

						if (device == null) {

							if (update.timeout) {
								System.out.println("Haven't seen " + Long.toHexString(update.deviceId) + " but it's already timed out!");
							} else {

								device = deviceKNN.createDevice(update.deviceId, update.timestamp, update.latitude, update.longitude);
								device.status = OnlineDevice.Status.ONLINE;

								devices.put(update.deviceId, device);
								quadtree.add(device);
								processOutageStop(device);
							}

						} else if (update.timeout) {
							if (device.status == OnlineDevice.Status.LOST || device.status == OnlineDevice.Status.OUTAGE) {
								System.out.println(device.getDeviceId() + " timed out twice");
							} else {
								device.timestamp = update.timestamp;
								device.status = OnlineDevice.Status.LOST;
								timingOut.add(device);

								processOutageStart(device);
							}
						} else {
							if (device.setPolar(update.latitude, update.longitude)) {
								Quadtree.remove(device);
								quadtree.add(device);
								deviceKNN.moveDevice(device);

								device.timestamp = update.timestamp;

								OnlineDevice.Status oldStatus = device.status;
								device.status = OnlineDevice.Status.ONLINE;

								if (oldStatus == OnlineDevice.Status.LOST) {
									timingOut.remove(device);
								}

								processOutageStop(device);
							} else {
								device.timestamp = update.timestamp;

								OnlineDevice.Status oldStatus = device.status;
								device.status = OnlineDevice.Status.ONLINE;

								if (oldStatus == OnlineDevice.Status.LOST) {
									timingOut.remove(device);
									processOutageStop(device);
								} else if (oldStatus == OnlineDevice.Status.OUTAGE) {
									Log.write("%d|outage stop detected|%.5f|%.5f\n", System.nanoTime(), device.latitude, device.longitude);
									processOutageStop(device);
								}
							}
						}
					} else if (update.type == Request.Type.OUTAGE_DATA_REQUEST) {
						if (update.aabb.minX > update.aabb.maxX) {
							double maxX = update.aabb.maxX;
							update.aabb.maxX = 1;

							quadtree.getWithin(update.devices, update.aabb, OnlineDevice.Status.OUTAGE);

							update.aabb.minX = 0;
							update.aabb.maxX = maxX;

							quadtree.getWithin(update.devices, update.aabb, OnlineDevice.Status.OUTAGE);
						} else {
							quadtree.getWithin(update.devices, update.aabb, OnlineDevice.Status.OUTAGE);
						}
					} else if (update.type == Request.Type.MANUAL_REPORT) {
						OnlineDevice device = deviceKNN.createDevice(0, update.timestamp, update.latitude, update.longitude);
						device.status = OnlineDevice.Status.OUTAGE;
						quadtree.add(device);


						manualReportsToVerify.addLast(device);
						processOutageStart(device);
					}

					update.pending.set(false);
				}
			} catch (InterruptedException e) {

			}
		}
	}

	private double calculateMetric(OnlineDevice device, OnlineDevice[] nn) {
		double offlineTotal = 0, onlineTotal = 0;

		for (int i = 0; i < nn.length; i++) {
			if (nn[i].status == OnlineDevice.Status.ONLINE) {
				onlineTotal += nn[i].status.weight / Math.max(3e-5, device.dist(nn[i]));
			} else {
				offlineTotal += nn[i].status.weight / Math.max(3e-5, device.dist(nn[i]));
			}
		}

		return offlineTotal / onlineTotal;
	}

	private void processOutageStart(OnlineDevice device) {
		if (deviceKNN.knn(device, nearetDevices)) {
			double total = calculateMetric(device, nearetDevices);

			if (total >= OUTAGE_START_THRESHOLD) {
				if (device.status == OnlineDevice.Status.LOST) {
					Log.write("%d|outage start detected|%.5f|%.5f\n", System.nanoTime(), device.latitude, device.longitude);
					device.status = OnlineDevice.Status.OUTAGE;
					timingOut.remove(device);
				}

				for (int i = 0; i < nearetDevices.length; i++) {
					if (nearetDevices[i].status == OnlineDevice.Status.LOST) {
						nearetDevices[i].status = OnlineDevice.Status.OUTAGE;
						timingOut.remove(nearetDevices[i]);
						processOutageStart(nearetDevices[i]);
					}
				}
			}
		}
	}

	private void processOutageStop(OnlineDevice device) {
		if (deviceKNN.knn(device, nearetDevices)) {
			double total = calculateMetric(device, nearetDevices);

			if (total <= OUTAGE_STOP_THRESHOLD) {

				if (device.status == OnlineDevice.Status.OUTAGE) {
					device.status = OnlineDevice.Status.LOST;
					device.timestamp = Time.getSeconds();
					timingOut.add(device);
				}

				for (int i = 0; i < nearetDevices.length; i++) {
					if (nearetDevices[i].status == OnlineDevice.Status.OUTAGE) {
						nearetDevices[i].status = OnlineDevice.Status.LOST;
						nearetDevices[i].timestamp = Time.getSeconds();
						timingOut.add(nearetDevices[i]);
						Log.write("%d|outage stop detected|%.5f|%.5f\n", System.nanoTime(), nearetDevices[i].latitude, nearetDevices[i].longitude);
						processOutageStop(nearetDevices[i]);
					}
				}
			}
		}
	}

	private void remove(OnlineDevice device) {
		devices.remove(device.getDeviceId());
		deviceKNN.removeDevice(device);
		timingOut.remove(device);
		Quadtree.remove(device);
	}

	private void timedOut(OnlineDevice device) {
		devices.remove(device.getDeviceId());
		deviceKNN.removeDevice(device);
		Quadtree.remove(device);
	}

	public void done() {
		System.out.println("Stopping outage detector");
		running = false;
	}

	public static class Request {
		public enum Type {
			DEVICE_STATUS_UPDATE,
			OUTAGE_DATA_REQUEST,
			MANUAL_REPORT
		}

		public volatile Type type = Type.DEVICE_STATUS_UPDATE;

		public volatile AABB aabb;
		public volatile List<OnlineDevice> devices;

		public volatile long deviceId;
		public volatile long timestamp;

		public volatile boolean timeout;
		public volatile double latitude;
		public volatile double longitude;
		public AtomicBoolean pending = new AtomicBoolean();
	}
}
