package epodpack;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class sendping {

	private static final byte[] pingMessage = new byte[] {
			'P', 'N', 'G',
			0, 0, 0, 0, 0, 0, 0, 0
	};
	private static DatagramSocket udpSocket;
	private static DatagramPacket udpPacket = new DatagramPacket(pingMessage, pingMessage.length);

	public static void sendPing(long userId) {
		System.out.println("Job Scheduler active");
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
}
