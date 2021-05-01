package com.epodserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConnectionManager extends Thread {

	private static final int BUFFER_SIZE = 32;
	private ConcurrentLinkedQueue<UserPositionUpdate> positionUpdates = new ConcurrentLinkedQueue<>();
	private Map<Long, ConnectionData> connections;
	private ByteBuffer buffer;
	private DatagramChannel channel;
	private int port;
	private volatile boolean running = true;
	private Queue<Long> simulatedPings = new ConcurrentLinkedQueue<>();
	public ConnectionManager(int port) {
		this.port = port;
		try {
			channel = DatagramChannel.open();
			buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			connections = new HashMap<>();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void userPositionUpdate(long id, User user) {
		positionUpdates.add(new UserPositionUpdate(id, user));
	}

	@Override
	public void run() {
		System.out.println("Successfully launched connection manager on port " + port);

		try {
			channel.bind(new InetSocketAddress(port));
			channel.configureBlocking(false);

			while (running) {
				while (!positionUpdates.isEmpty()) {
					UserPositionUpdate positionUpdate = positionUpdates.remove();

					ConnectionData connection = connections.get(positionUpdate.id);

					if (connection != null) {
						connection.positionChanged(positionUpdate.user);
					}
				}

				Long ping;

				while ((ping = simulatedPings.poll()) != null) {
					ConnectionData connection = connections.get(ping);

					if (connection != null) {
						connection.pingReceived();
					} else {
						User user = ServerMain.userMap.get(ping);

						if (user != null) {
							connection = new ConnectionData(ping, user);
							connections.put(ping, connection);
						}
					}
				}

				acceptPings();

				checkTimeouts();
			}

			closeAllClients();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void checkTimeouts() {
		Iterator<ConnectionData> it = connections.values().iterator();

		if (it.hasNext()) {
			for (ConnectionData connection = it.next(); it.hasNext(); connection = it.next()) {
				if (connection.checkTimedOut()) {
					it.remove();
				}
			}
		}
	}

	public void simulatePing(long userId) {
		simulatedPings.add(userId);
	}

	private void acceptPings() {
		try {
			SocketAddress received;
			while ((received = channel.receive(buffer)) != null) {

				if (received instanceof InetSocketAddress && buffer.position() == 11) {
					try {
						String address = Network.getIpAddress(received);


						buffer.rewind();

						if (buffer.get() == 'P' && buffer.get() == 'N' && buffer.get() == 'G') {
							long userId = buffer.asLongBuffer().get();

							ConnectionData connection = connections.get(userId);

							if (connection != null) {
								connection.pingReceived();
							} else {
								User user = ServerMain.userMap.get(userId);

								if (user != null) {
									connection = new ConnectionData(userId, user);
									connections.put(userId, connection);
								}
							}
						}
					} catch (ArrayIndexOutOfBoundsException e) {
						e.printStackTrace();
					}
				} else if (buffer.position() == 19) {
//					try {
//						buffer.rewind();
//
//						if (buffer.get() == 'M' && buffer.get() == 'A' && buffer.get() == 'N') {
//							DoubleBuffer buf = buffer.asDoubleBuffer();
//							double latitude = buf.get();
//							double longitude = buf.get();
//
//							OutageDetector.Request request = new OutageDetector.Request();
//							request.timestamp = Time.getSeconds();
//							request.latitude = latitude;
//							request.longitude = longitude;
//							request.type = OutageDetector.Request.Type.MANUAL_REPORT;
//
//							ServerMain.detector.queueUpdate(request);
//
//						}
//					} catch (ArrayIndexOutOfBoundsException e) {
//						e.printStackTrace();
//					}
				}
			}

			buffer.rewind();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void closeAllClients() {
		try {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void done() {
		System.out.println("Stopping connection manager");
		running = false;
	}

	private static class UserPositionUpdate {
		public long id;
		public User user;

		public UserPositionUpdate(long id, User user) {
			this.id = id;
			this.user = user;
		}
	}
}
