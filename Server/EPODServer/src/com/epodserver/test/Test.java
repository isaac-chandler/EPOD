package com.epodserver.test;

import com.epodserver.ServerMain;
import com.epodserver.User;

import javax.crypto.Cipher;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Test {

	private static HashMap<Long, User> testUsers = new HashMap<>();

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

	private static void fromDouble(byte[] arr, int offset, double value) {
		long long_ = Double.doubleToLongBits(value);

		arr[offset] = (byte) long_;
		arr[offset + 1] = (byte) (long_ >>> 8L);
		arr[offset + 2] = (byte) (long_ >>> 16L);
		arr[offset + 3] = (byte) (long_ >>> 24L);
		arr[offset + 4] = (byte) (long_ >>> 32L);
		arr[offset + 5] = (byte) (long_ >>> 40L);
		arr[offset + 6] = (byte) (long_ >>> 48L);
		arr[offset + 7] = (byte) (long_ >>> 56L);
	}

	private static Cipher cipher = null;
	private static PublicKey key = null;
	private static final byte[] message = new byte[] {
			'P', 'O', 'S',
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0
	};

	private static final byte[] updateLocationMessage = new byte[] {
				'U', 'P', 'D',
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0
	};

	private static void fromLong(byte[] arr, int offset, long value) {
		arr[offset] = (byte) value;

		arr[offset + 1] = (byte) (value >>> 8L);
		arr[offset + 2] = (byte) (value >>> 16L);
		arr[offset + 3] = (byte) (value >>> 24L);
		arr[offset + 4] = (byte) (value >>> 32L);
		arr[offset + 5] = (byte) (value >>> 40L);
		arr[offset + 6] = (byte) (value >>> 48L);
		arr[offset + 7] = (byte) (value >>> 56L);
	}

	private static DatagramSocket udpSocket;
	private static final byte[] pingMessage = new byte[] {
			'P', 'N', 'G',
			0, 0, 0, 0, 0, 0, 0, 0
	};
	private static DatagramPacket udpPacket = new DatagramPacket(pingMessage, pingMessage.length);

	public static void sendPing(long userId) {
		try {
			if (udpSocket == null) {
				udpSocket = new DatagramSocket();
				// Need I say it ------------------------V
				udpSocket.connect(InetAddress.getByName("localhost"), 6241);
			}

			fromLong(pingMessage, 3, userId);
			udpSocket.send(udpPacket);
		} catch (IOException e) {

		}
	}

	public static void changeLocation(long userId, double latitude, double longitude) throws IOException {
		if (cipher == null) {
			KeyFactory factory;
			try {
				cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				factory = KeyFactory.getInstance("RSA");

				X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(Files.readAllBytes(Paths.get("whaterver you called the public key file"))));
				key = factory.generatePublic(publicKeySpec);
			} catch (GeneralSecurityException e) {
				throw new IOException("This shouldn't happen", e);
			}
		}

		if (Double.isNaN(latitude) || Double.isNaN(longitude) || latitude > 85.0 || latitude < -85.0 || longitude > 180.0 || longitude < -180.0) {
			throw new IOException("The latitude and longitude must be less than 85\u00B0N/S and 180\u00B0E/W.");
		}

		fromDouble(updateLocationMessage, 3, latitude);
		fromDouble(updateLocationMessage, 11, longitude);
		fromLong(updateLocationMessage, 19, userId);

		try {
			cipher.init(Cipher.ENCRYPT_MODE, key);

			//--------------> Obviously not localhost <---------
			URL url = new URL("http://localhost/updatelocation?pos=" + new String(Base64.getUrlEncoder().encode(cipher.doFinal(updateLocationMessage)), "UTF-8"));
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			int status = con.getResponseCode();

			if (status != 200) {
				con.disconnect();

				throw new IOException("Request failed");
			} else {
				con.disconnect();
			}
		} catch (GeneralSecurityException e) {
			throw new IOException("This shouldn't happen", e);
		}
	}

	public static long getKey(double latitude, double longitude) throws IOException {
		if (cipher == null) {
			KeyFactory factory;
			try {
				cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				factory = KeyFactory.getInstance("RSA");

				X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(Files.readAllBytes(Paths.get("whaterver you called the public key file"))));
				key = factory.generatePublic(publicKeySpec);
			} catch (GeneralSecurityException e) {
				throw new IOException("This shouldn't happen", e);
			}
		}

		if (Double.isNaN(latitude) || Double.isNaN(longitude) || latitude > 85.0 || latitude < -85.0 || longitude > 180.0 || longitude < -180.0) {
			throw new IOException("The latitude and longitude must be less than 85\u00B0N/S and 180\u00B0E/W.");
		}

		fromDouble(message, 3, latitude);
		fromDouble(message, 11, longitude);

		try {
			cipher.init(Cipher.ENCRYPT_MODE, key);

			//--------------> Obviously not localhost <---------
			URL url = new URL("http://localhost/newuser?pos=" + new String(Base64.getUrlEncoder().encode(cipher.doFinal(message)), "UTF-8"));
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			int status = con.getResponseCode();

			if (status != 200) {
				con.disconnect();

				throw new IOException("Request failed");
			} else {
				long id = toLong(con.getInputStream().readAllBytes(), 0);
				con.disconnect();

				return id;
			}
		} catch (GeneralSecurityException e) {
			throw new IOException("This shouldn't happen", e);
		}

	}

	public static void loadTestUsers(String inputFile) {
		try (DataInputStream input = new DataInputStream(new FileInputStream(inputFile))) {
			try {
				while (true) {
					double latitude = input.readDouble();
					double longitude = input.readDouble();
					long userId = input.readLong();

					testUsers.put(userId, new User("192.168.0.118", latitude, longitude));
				}


			} catch (EOFException e) {
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.out.println(-1L & 0x3fffffffffL);
	}

	private static TestThread testThread;

	public static void runTest(int divisor) {
		if (testThread == null) {
			testThread = new TestThread(testUsers, divisor);
			testThread.start();
		}
	}

	public static void stopTest() {
		if (testThread != null) {
			testThread.done();
			try {
				testThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			testThread = null;
		}
	}

	public static void localOutage() {
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("localhost"), 6241);

			byte[] ping = new byte[] {
					'P', 'N', 'G',
					0, 0, 0, 0, 0, 0, 0, 0
			};

			DatagramPacket packet = new DatagramPacket(ping, ping.length);

			for (Map.Entry<Long, User> e : testUsers.entrySet()) {
				User user = e.getValue();

				if (user.longitude >= 173.930902 && user.longitude <= 174.129069 && user.latitude >= -35.237559 && user.latitude <= -35.107696) {
					fromLong(ping, 3, e.getKey());
					socket.send(packet);
					try {
						Thread.sleep(5);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void createTestUsers(String buildingFile, String outputFile) {
		try (DataInputStream input = new DataInputStream(new FileInputStream(buildingFile));
			 DataOutputStream output = new DataOutputStream(new FileOutputStream(outputFile))) {

			Cipher ciper = Cipher.getInstance("RSA/ECB/PKCS1Padding");

			byte[] message = new byte[] {
					'P', 'O', 'S',
					0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0
			};

			int i = 0;
			try {
				while (true) {
					double latitude = input.readDouble();
					double longitude = input.readDouble();

					fromDouble(message, 3, latitude);
					fromDouble(message, 11, longitude);

					ciper.init(Cipher.ENCRYPT_MODE, ServerMain.publicKey);

					URL url = new URL("http://localhost/newuser?pos=" + new String(Base64.getUrlEncoder().encode(ciper.doFinal(message)), "UTF-8"));
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setRequestMethod("GET");
					int status = con.getResponseCode();

					if (status != 200) {
						System.out.println("Failed " + i);
						con.disconnect();
					} else {
						long id = toLong(con.getInputStream().readAllBytes(), 0);
						con.disconnect();

						System.out.printf("Test user %016X\n", id);

						output.writeDouble(latitude);
						output.writeDouble(longitude);
						output.writeLong(id);
					}

					i++;
					if (i % 100 == 0) {
						System.out.println(i);
					}
				}


			} catch (EOFException e) {
			}

			System.out.println(i);
		} catch (IOException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
}
