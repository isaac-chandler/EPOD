package epodpack;

import javax.crypto.Cipher;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class locationInput {
	private static final byte[] updateLocationMessage = new byte[] {
			'U', 'P', 'D',
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0
	};
	private static final byte[] message = new byte[] {
			'P', 'O', 'S',
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0
	};
	static JFrame frame = new JFrame("EPOD");
	private static Cipher cipher = null;
	private static PublicKey key = null;
	private static String encFileName = "ping-publickey";
	private JTextArea EPODTextArea1;
	private JButton submitButton;
	private JTextField latIn;
	private JTextField longIn;
	private JTextArea theLongitudeAndLatittudeTextArea;
	private JPanel panel2;
	private String path = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "EPOD";
	private String filePath = path + File.separator + "key.txt";
	private File folder = new File(path);
	private File f = new File(filePath);

	public locationInput() {
		submitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				if (checkValidity()) {
					Double lat = Double.parseDouble(latIn.getText());
					Double lon = Double.parseDouble(longIn.getText());

					if (firstRequest()) {
						try {
							try {
								long keyID = getKey(lat, lon);
								JOptionPane.showMessageDialog(frame, Long.toHexString(keyID), "Key", JOptionPane.PLAIN_MESSAGE);
								writeToFile(keyID);
							} catch (IOException e1) {
								e1.printStackTrace();
								// TODO: take to the error page here
							}
						} catch (Exception a) {

						}

					} else {

						try {
							changeLocation(readFromFile(), lat, lon);
							JOptionPane.showMessageDialog(null, Long.toHexString(readFromFile()), "Key", JOptionPane.PLAIN_MESSAGE);
						} catch (Exception y) {
							y.printStackTrace();
						}
					}

				} else {

					locInError locerror = new locInError();
					frame.dispose();
					locerror.showFrame(true);

				}

			}
		});
	}

	public static void showFrame(boolean b) {

		frame.setContentPane(new locationInput().panel2);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setVisible(true);
		frame.getContentPane().setBackground(Color.DARK_GRAY);

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

	public static long getKey(double latitude, double longitude) throws IOException {
		if (cipher == null) {
			KeyFactory factory;
			try {
				cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				factory = KeyFactory.getInstance("RSA");

				X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(Files.readAllBytes(Paths.get(encFileName))));
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

			//-------------->not localhost <---------
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

	public boolean checkValidity() { //Checks if coordinates were entered correctly and determines if user is taken to error screen

		String latS = latIn.getText();
		String longS = longIn.getText();
		try {
			double lat = Double.parseDouble(latS);
			double lon = Double.parseDouble(longS);
			return true;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean firstRequest() {
		//TODO: Check if file exists
		if (f.exists() && !f.isDirectory()) {
			return false;
		} else {
			return true;
		}
	}

	private void writeToFile(long x) {
		try {
			new File(path).mkdirs();
			FileOutputStream fos = new FileOutputStream(filePath);
			DataOutputStream dos = new DataOutputStream(fos);
			dos.writeLong(x);
			dos.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error in writeToFile method");
		}
	}

	public long readFromFile() {
		try {
			FileInputStream fis = new FileInputStream(filePath);
			DataInputStream dis = new DataInputStream(fis);
			long x = dis.readLong();
			dis.close();
			return x;

		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}


	}


}
