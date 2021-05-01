import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class TestDataCreator {

	public static void main(String[] args) {
		try (Scanner scanner = new Scanner(new File("map_raw.xml")); DataOutputStream out = new DataOutputStream(new FileOutputStream("buildings.bin"))) {
			HashMap<Long, double[]> nodes = new HashMap<>();

			String line = scanner.nextLine();
			while (!line.startsWith("  <node")) {
				line = scanner.nextLine();
			}

			while (!line.startsWith("  <way")) {
				if (line.startsWith("  <node")) {
					String[] parts = line.split(" ");

					nodes.put(Long.valueOf(parts[3].substring(4, parts[3].length() - 1)),
							new double[] {
									Double.parseDouble(parts[4].substring(5, parts[4].length() - 1)),
									Double.parseDouble(parts[5].substring(5, parts[5].length() - 3)),
							});
				}
				line = scanner.nextLine();
			}

			System.out.println(nodes.size() + " nodes parsed");

			int buldings = 0;

			while (scanner.hasNextLine()) {
				if (line.startsWith("  <way")) {
					int count = 0;
					double avgLat = 0;
					double avgLon = 0;

					line = scanner.nextLine();

					while (!line.startsWith("  </way")) {
						if (line.startsWith("    <nd")) {
							count++;

							String[] parts = line.split(" ");
							double[] pos = nodes.get(Long.valueOf(parts[5].substring(5, parts[5].length() - 3)));

							avgLat += pos[0];
							avgLon += pos[1];

						}

						line = scanner.nextLine();
					}

					out.writeDouble(avgLat / count);
					out.writeDouble(avgLon / count);
					buldings++;
				}

				line = scanner.nextLine();
			}

			System.out.println(buldings + " buildings located");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
