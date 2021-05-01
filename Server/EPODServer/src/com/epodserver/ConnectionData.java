package com.epodserver;

public class ConnectionData {

	private long lastPingTime;
	private User user;
	private long id;
	private OutageDetector.Request statusUpdate = new OutageDetector.Request();

	public ConnectionData(long id, User user) {
		this.id = id;
		this.user = user;
		statusUpdate.latitude = user.latitude;
		statusUpdate.longitude = user.longitude;

		statusUpdate.timeout = false;
		statusUpdate.deviceId = id;

		pingReceived();
		statusUpdate.timestamp = lastPingTime;
		ServerMain.detector.queueUpdate(statusUpdate);
	}

	public void pingReceived() {
		lastPingTime = Time.getSeconds();
	}

	public void positionChanged(User user) {
		this.user = user;
		statusUpdate.latitude = user.latitude;
		statusUpdate.longitude = user.longitude;

		ServerMain.detector.queueUpdate(statusUpdate);
	}

	public boolean checkTimedOut() {
		long time = Time.getSeconds();
		boolean timedOut = lastPingTime < time - OutageDetector.CONNECTION_TIMEOUT_TIME;

		if (timedOut) {
			statusUpdate.timeout = true;

			statusUpdate.timestamp = time;
			ServerMain.detector.queueUpdate(statusUpdate);
		}

		return timedOut;
	}

	@Override
	public String toString() {
		return String.format("%016X", id);
	}

	public User getUser() {
		return user;
	}
}
