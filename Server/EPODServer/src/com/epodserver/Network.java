package com.epodserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Network {
	public static String getIpAddress(SocketAddress address) {
		try {
			return ((InetSocketAddress) address).getAddress().toString().split("/")[1];
		} catch (Exception e) {
			return "INV";
		}
	}

	private static void writeMacToFile(List<String> macAddresses, String folder, String file) {
		new File(folder).mkdirs();

		try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
			for (String macAddress : macAddresses) {
				out.println(macAddress);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<String> readMacFromFile(String file) {
		try (Scanner in = new Scanner(new FileInputStream(file))) {
			List<String> macs = new ArrayList<>();

			while (in.hasNextLine()) {
				macs.add(in.nextLine());
			}

			return macs;
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	private static <T> boolean arraysOverlap(List<T> a, List<T> b) {
		for (T i : a) {
			for (T j : b) {
				if (i.equals(j))
					return true;
			}
		}

		return false;
	}


	private static List<String> getMacAddresses() {
		List<String> results = new ArrayList<>();

		try {
			Pattern pattern = Pattern.compile("\\s*Default Gateway.*:\\s*([0-9a-fA-F.:]+)\\s*");

			Process process = Runtime.getRuntime().exec("ipconfig");
			process.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line = reader.readLine();
			while (line != null) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches()) {
					String address = getMacAddress(matcher.group(1));

					if (address != null) {
						results.add(address);
					}
				}

				line = reader.readLine();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return results;
	}

	private static String getMacAddress(String ip) {
		try {
			Pattern pattern = Pattern.compile("\\s*" + ip + "\\s*([0-9a-fA-F-:]+)\\s*.*");

			Process process = Runtime.getRuntime().exec("arp -a " + ip);
			process.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line = reader.readLine();
			while (line != null) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches()) {
					return matcher.group(1);
				}

				line = reader.readLine();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}
}
