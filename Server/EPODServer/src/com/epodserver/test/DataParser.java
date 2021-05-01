package com.epodserver.test;

import com.epodserver.AABB;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class DataParser {

	private static class Outage {
		public Outage(AABB aabb, double time) {
			this.aabb = aabb;
			this.time = time;
		}

		public AABB aabb;

		public double time;

		public double startLatency = -1;
		public double stopLatency = -1;

		public boolean contains(double latitude, double longitude) {
			return longitude >= aabb.minX && longitude <= aabb.maxX && latitude >= aabb.minY && latitude <= aabb.maxY;
		}

		public boolean isNearFalsePositive(double latitude, double longitude) {
			double latDiff = Math.min(Math.abs(latitude - aabb.minY), Math.abs(latitude - aabb.maxY));
			double lonDiff = Math.min(Math.abs(longitude - aabb.minX), Math.abs(longitude - aabb.maxX));

			return latDiff < 0.25 * (aabb.maxY - aabb.minY) && lonDiff < 0.25 * (aabb.maxX - aabb.minX);
		}
	}

	public static void main(String[] args) {
		ArrayList<Outage> outages = new ArrayList<>();

		int otherFalsePositives = 0;
		int nearFalsePositives = 0;

		try (Scanner in = new Scanner(new FileInputStream(new Scanner(System.in).nextLine()))) {
			while (in.hasNextLine()) {
				String[] parts = in.nextLine().split("\\|");

				double time = Long.parseLong(parts[0]) * 1E-9;

				if (parts[1].equals("outage started")) {
					outages.add(new Outage(new AABB(Double.parseDouble(parts[3]), Double.parseDouble(parts[5]),
							Double.parseDouble(parts[2]), Double.parseDouble(parts[4])), time));
				} else if (parts[1].equals("outage start detected")) {
					double latitude = Double.parseDouble(parts[2]);
					double longitude = Double.parseDouble(parts[3]);

					if (outages.size() >= 1) {
						Outage outage = outages.get(outages.size() - 1);
						if (outage.contains(latitude, longitude)) {
							if (outage.startLatency < 0) {
								outage.startLatency = time - outage.time;
							}
						} else if (outages.size() >= 2) {
							outage = outages.get(outages.size() - 2);
							if (outage.contains(latitude, longitude)) {
								if (outage.startLatency < 0) {
									outage.startLatency = time - outage.time;
								}
							} else {
								if (outage.isNearFalsePositive(latitude, longitude)) {
									nearFalsePositives++;
								} else {
									otherFalsePositives++;
								}
							}
						} else {
							if (outage.isNearFalsePositive(latitude, longitude)) {
								nearFalsePositives++;
							} else {
								otherFalsePositives++;
							}
						}
					} else {
						otherFalsePositives++;
					}
				} else if (parts[1].equals("outage stop detected")) {
					double latitude = Double.parseDouble(parts[2]);
					double longitude = Double.parseDouble(parts[3]);

					if (outages.size() >= 2) {
						Outage outage = outages.get(outages.size() - 2);
						if (outage.contains(latitude, longitude)) {
							if (outage.stopLatency < 0) {
								outage.stopLatency = time - outage.time;
							}
						} else if (outages.size() >= 3) {
							outage = outages.get(outages.size() - 3);

							if (outage.contains(latitude, longitude)) {
								if (outage.stopLatency < 0) {
									outage.stopLatency = time - outage.time;
								}
							}
						}
					}
				}
			}

			int totalOutageStarts = 0, outageStartDetected = 0;
			int totalOutageStops = 0, outageStopDetected = 0;

			double totalStartLatency = 0;
			double totalStopLatency = 0;

			Outage previousOutage = null;

			for (int i = 0; i < outages.size() - 1; i++) {
				Outage outage = outages.get(i);

				if (outage.startLatency < 0) {
					if (previousOutage == null || !previousOutage.aabb.contains(outage.aabb)) {
						totalOutageStarts++;
					}
				} else {
					totalOutageStarts++;
					outageStartDetected++;

					totalStartLatency += outage.startLatency;
				}

				if (outage.stopLatency < 0) {
					if (previousOutage == null || !previousOutage.aabb.contains(outage.aabb)) {
						totalOutageStops++;
					}
				} else {
					totalOutageStops++;
					outageStopDetected++;

					totalStopLatency += outage.startLatency;
				}

				previousOutage = outage;
			}

			System.out.println("Total outages: " + (outages.size() - 1));
			System.out.println("Near false positives: " + nearFalsePositives);
			System.out.println("Near false positive rate" + (double) nearFalsePositives / (outages.size() - 1));
			System.out.println("Other false positives: " + otherFalsePositives);
			System.out.println("Start detection rate: " + ((double) outageStartDetected / totalOutageStarts));
			System.out.println("Average start latency: " + (totalStartLatency / outageStartDetected));
			System.out.println("Stop detection rate: " + ((double) outageStopDetected / totalOutageStops));
			System.out.println("Average stop latency: " + (totalStopLatency / outageStopDetected));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
