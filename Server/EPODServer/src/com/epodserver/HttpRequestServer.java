package com.epodserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Cipher;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;

public class HttpRequestServer extends Thread {

	private static final int MIN_ZOOM_LEVEL = 12;
	private static final int MAX_ZOOM_LEVEL = 27;
	private static final double MAX_DATA_REQUEST_AREA = 1E-6;
	private static final byte[] websiteString = "EPOD works by utilizing a network of computers. These computers send information to our server. When they stop sending information to our server at the time as other computers in the network, if these computers are close together, it is likely to be a power outage. This information is then added to our power outage map which can be accessed by the 'outage map' button.".getBytes(StandardCharsets.UTF_8);
	private final Random random;
	private int port;
	private Object stopLock = new Object();
	private ThreadLocal<OutageDetector.Request> request = ThreadLocal.withInitial(() -> {
		OutageDetector.Request request = new OutageDetector.Request();
		request.type = OutageDetector.Request.Type.OUTAGE_DATA_REQUEST;
		request.aabb = new AABB(0, 0, 0, 0);
		request.devices = new ArrayList<>();
		return request;
	});
	private Cipher cipher;

	public HttpRequestServer(int port) {
		this.port = port;
		random = new Random(new SecureRandom().nextLong());
		try {
			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, ServerMain.privateKey);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	private static boolean isInRange(double value, double min, double max) {
		return value >= min && value <= max;
	}

	private static long toLong(byte[] arr, int offset) {
		return Byte.toUnsignedLong(arr[offset]) |
				(Byte.toUnsignedLong(arr[offset + 1]) << 8L) |
				(Byte.toUnsignedLong(arr[offset + 2]) << 16L) |
				(Byte.toUnsignedLong(arr[offset + 3]) << 24L) |
				(Byte.toUnsignedLong(arr[offset + 4]) << 32L) |
				(Byte.toUnsignedLong(arr[offset + 5]) << 40L) |
				(Byte.toUnsignedLong(arr[offset + 6]) << 48L) |
				(Byte.toUnsignedLong(arr[offset + 7]) << 56L);
	}

	private static double toDouble(byte[] arr, int offset) {
		return Double.longBitsToDouble(toLong(arr, offset));
	}

	private static byte[] fromLong(long value) {
		return new byte[] {
				(byte) value,
				(byte) (value >>> 8),
				(byte) (value >>> 16),
				(byte) (value >>> 24),
				(byte) (value >>> 32),
				(byte) (value >>> 40),
				(byte) (value >>> 48),
				(byte) (value >>> 56)
		};
	}

	private static boolean parseQuery(HttpExchange exchange, QueryHandle func) throws IOException {
		String query = exchange.getRequestURI().getRawQuery();

		if (query == null) {
			exchange.sendResponseHeaders(400, 0);
			exchange.getResponseBody().close();
		}
		int last = 0, next, length = query.length();


		while (last < length) {
			next = query.indexOf("&", last);
			if (next == -1)
				next = length;

			if (next > last) {
				int eqPos = query.indexOf('=', last);
				if (eqPos < 0 || eqPos > next) {
					exchange.sendResponseHeaders(400, 0);
					exchange.getResponseBody().close();
					return false;
				} else {
					try {
						String key = URLDecoder.decode(query.substring(last, eqPos), StandardCharsets.UTF_8);
						String value = URLDecoder.decode(query.substring(eqPos + 1, next), StandardCharsets.UTF_8);

						if (!func.apply(key, value)) {
							exchange.sendResponseHeaders(400, 0);
							exchange.getResponseBody().close();

							return false;
						}
					} catch (IllegalArgumentException e) {
						exchange.sendResponseHeaders(400, 0);
						exchange.getResponseBody().close();

						return false;
					}
				}
			}
			last = next + 1;
		}

		return true;
	}

	private static boolean validateCoordinate(HttpExchange exchange, double latitude, double longitude) throws IOException {
		if (!isInRange(latitude, -85, 85) || !isInRange(longitude, -180, 180)) {
			byte[] response = "The latitude and longitude must be less than 85\u00B0N/S and 180\u00B0E/W.".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
			exchange.sendResponseHeaders(400, response.length);
			exchange.getResponseBody().write(response);
			exchange.getResponseBody().close();
			return false;
		}

		return true;
	}

	private static boolean checkValid(HttpExchange exchange, String valid) throws IOException {
		URI uri = exchange.getRequestURI();

		if (uri.getPath().equals(valid)) {
			if (exchange.getRequestMethod().equalsIgnoreCase("get")) {
				return true;
			} else {
				exchange.sendResponseHeaders(405, -1);
				exchange.getResponseBody().close();
			}
		} else {
			exchange.sendResponseHeaders(404, -1);
			exchange.getResponseBody().close();
		}


		return false;
	}

	private void handleDataRequest(HttpExchange exchange) throws IOException {
		if (!checkValid(exchange, "/data"))
			return;

		DataQueryHandle handle = new DataQueryHandle();

		if (!parseQuery(exchange, handle))
			return;

		if (!isInRange(handle.nex, 0, 1) || !isInRange(handle.ney, 0, 1) ||
				!isInRange(handle.swx, 0, 1) || !isInRange(handle.swy, 0, 1) || handle.ney > handle.swy) {
			exchange.sendResponseHeaders(400, 0);
			exchange.getResponseBody().close();
			return;
		}


		if (handle.nex < handle.swx) {
			if ((handle.swy - handle.ney) * (1.0 - handle.swx + handle.nex) > MAX_DATA_REQUEST_AREA) {
				exchange.sendResponseHeaders(400, 0);
				exchange.getResponseBody().close();
				return;
			}
		} else if ((handle.swy - handle.ney) * (handle.nex - handle.swx) > MAX_DATA_REQUEST_AREA) {
			exchange.sendResponseHeaders(400, 0);
			exchange.getResponseBody().close();
			return;
		}

		OutageDetector.Request request = this.request.get();

		request.aabb.minX = handle.swx;
		request.aabb.maxX = handle.nex;
		request.aabb.minY = handle.ney;
		request.aabb.maxY = handle.swy;
		request.devices.clear();

		ServerMain.detector.queueUpdate(request);


		while (request.pending.get()) {
			Thread.onSpinWait();
		}

		exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
		exchange.sendResponseHeaders(200, 4 + 16 * request.devices.size());

		DataOutput out = new DataOutputStream(exchange.getResponseBody());
		out.writeInt(request.devices.size());

		for (OnlineDevice device : request.devices) {
			out.writeDouble(device.latitude);
			out.writeDouble(device.longitude);
		}

		exchange.getResponseBody().close();
	}

	private void handleNewUserRequest(HttpExchange exchange) throws IOException {
		if (!checkValid(exchange, "/newuser"))
			return;

		PosQueryHandle handle = new PosQueryHandle();

		if (!parseQuery(exchange, handle))
			return;


		if (handle.decrypted == null) {
			exchange.sendResponseHeaders(400, 0);
			exchange.getResponseBody().close();
			return;
		}


		if (handle.decrypted.length != 19 || handle.decrypted[0] != 'P' || handle.decrypted[1] != 'O' || handle.decrypted[2] != 'S') {
			exchange.sendResponseHeaders(400, 0);
			exchange.getResponseBody().close();
			return;
		}


		double latitude = toDouble(handle.decrypted, 3);
		double longitude = toDouble(handle.decrypted, 11);

		if (!validateCoordinate(exchange, latitude, longitude))
			return;


		long id = random.nextLong();

		while (id == 0 || ServerMain.userMap.containsKey(id))
			id = random.nextLong();

		ServerMain.userMap.put(id, new User(Network.getIpAddress(exchange.getRemoteAddress()), latitude, longitude));
		ServerMain.userDb.commit();

		System.out.printf("Created user %016X\n", id);

		byte[] response = fromLong(id);
		exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
		exchange.sendResponseHeaders(200, 8);
		exchange.getResponseBody().write(response);
		exchange.getResponseBody().close();
		exchange.close();

	}

	private void handleManual(HttpExchange exchange) throws IOException {
		if (!checkValid(exchange, "/manual"))
			return;

		PosQueryHandle handle = new PosQueryHandle();

		if (!parseQuery(exchange, handle))
			return;


		double latitude = toDouble(handle.decrypted, 3);
		double longitude = toDouble(handle.decrypted, 11);

		if (!validateCoordinate(exchange, latitude, longitude))
			return;

		OutageDetector.Request request = new OutageDetector.Request();
		request.timestamp = Time.getSeconds();
		request.latitude = latitude;
		request.longitude = longitude;
		request.type = OutageDetector.Request.Type.MANUAL_REPORT;

		ServerMain.detector.queueUpdate(request);


		byte[] response = new byte[] {'O', 'K'};
		exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
		exchange.sendResponseHeaders(200, 8);
		exchange.getResponseBody().write(response);
		exchange.getResponseBody().close();
		exchange.close();

	}

	private void handleCheckValidRequest(HttpExchange exchange) throws IOException {
		if (!checkValid(exchange, "/checkvalid"))
			return;

		final long[] ids = new long[] {0};

		if (!parseQuery(exchange, (k, v) -> {
			if (k.equals("id")) {
				try {
					ids[0] = Long.parseUnsignedLong(v, 16);
				} catch (NumberFormatException e) {
					return false;
				}
			}

			return true;
		}))
			return;


		long id = ids[0];

		if (id == 0) {
			exchange.sendResponseHeaders(400, 0);
			exchange.getResponseBody().close();
			return;
		}

		byte[] response;

		if (ServerMain.userMap.containsKey(id)) {
			response = "true".getBytes(StandardCharsets.UTF_8);
		} else {
			response = "false".getBytes(StandardCharsets.UTF_8);
		}

		exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
		exchange.sendResponseHeaders(200, response.length);
		exchange.getResponseBody().write(response);
		exchange.getResponseBody().close();
	}

	private void handleUpdateLocationRequest(HttpExchange exchange) throws IOException {
		if (!checkValid(exchange, "/updatelocation"))
			return;

		PosQueryHandle handle = new PosQueryHandle();

		if (!parseQuery(exchange, handle))
			return;

		if (handle.decrypted == null || handle.decrypted.length != 27 || handle.decrypted[0] != 'U' || handle.decrypted[1] != 'P' || handle.decrypted[2] != 'D') {
			exchange.sendResponseHeaders(400, 0);
			exchange.getResponseBody().close();
			return;
		}

		double latitude = toDouble(handle.decrypted, 3);
		double longitude = toDouble(handle.decrypted, 11);
		long id = toLong(handle.decrypted, 19);

		if (!validateCoordinate(exchange, latitude, longitude)) {
			return;
		} else if (!ServerMain.userMap.containsKey(id)) {
			byte[] response = "The supplied user id doesn't exist".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
			exchange.sendResponseHeaders(400, response.length);
			exchange.getResponseBody().write(response);
			exchange.getResponseBody().close();
			return;
		}


		if (ServerMain.userMap.containsKey(id)) {
			User user = new User(Network.getIpAddress(exchange.getRemoteAddress()), latitude, longitude);
			ServerMain.userMap.put(id, user);
			ServerMain.userDb.commit();
			System.out.printf("Changed location of user %016X\n", id);


			ServerMain.connectionManager.userPositionUpdate(id, user);


			byte[] response = fromLong(id);
			exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
			exchange.sendResponseHeaders(200, 8);
			exchange.getResponseBody().write(response);
			exchange.getResponseBody().close();
		} else {
			exchange.sendResponseHeaders(400, 0);
			exchange.getResponseBody().close();
		}


	}

	@Override
	public void run() {
		System.out.println("Successfully started http server on port " + port);
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

			server.createContext("/newuser", this::handleNewUserRequest);
			server.createContext("/updatelocation", this::handleUpdateLocationRequest);
			server.createContext("/checkvalid", this::handleCheckValidRequest);
			server.createContext("/data", this::handleDataRequest);
			server.createContext("/manual", this::handleManual);
			server.createContext("/", this::handleGeneric);

			server.start();

			while (true)
				synchronized (stopLock) {
					try {
						stopLock.wait();
						break;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			server.stop(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void handleGeneric(HttpExchange exchange) throws IOException {
		if (!checkValid(exchange, "/"))
			return;

		exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
		exchange.sendResponseHeaders(200, websiteString.length);

		DataOutput out = new DataOutputStream(exchange.getResponseBody());
		out.write(websiteString);

		exchange.getResponseBody().close();
	}

	public void done() {
		System.out.println("Stopping http server");
		synchronized (stopLock) {
			stopLock.notifyAll();
		}
	}

	private interface QueryHandle {
		boolean apply(String key, String value);
	}

	private static class DataQueryHandle implements QueryHandle {
		public double nex = Double.NaN, ney = Double.NaN, swx = Double.NaN, swy = Double.NaN;

		@Override
		public boolean apply(String key, String value) {
			double d;
			try {
				d = Double.longBitsToDouble(Long.parseUnsignedLong(value, 16));
			} catch (NumberFormatException e) {
				return false;
			}

			if (key.equals("nex")) {
				nex = d;
			} else if (key.equals("ney")) {
				ney = d;
			} else if (key.equals("swx")) {
				swx = d;
			} else if (key.equals("swy")) {
				swy = d;
			}

			return true;
		}
	}

	private class PosQueryHandle implements QueryHandle {
		public byte[] decrypted = null;


		@Override
		public boolean apply(String key, String value) {
			if (key.equals("pos")) {
				byte[] encrypted;
				try {
					encrypted = Base64.getUrlDecoder().decode(value);
				} catch (IllegalArgumentException e) {
					return false;
				}

				synchronized (cipher) {
					try {
						decrypted = cipher.doFinal(encrypted);
					} catch (GeneralSecurityException e) {
						e.printStackTrace();
						return false;
					}
				}
			}

			return true;
		}
	}
}
