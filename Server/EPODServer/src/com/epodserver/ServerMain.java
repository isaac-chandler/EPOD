package com.epodserver;

import com.epodserver.test.Test;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;

public class ServerMain {
	public static PrivateKey privateKey;
	public static PublicKey publicKey;

	public static OutageDetector detector;
	public static ConnectionManager connectionManager;
	public static HttpRequestServer httpRequestServer;

	public static Map<Long, User> userMap;
	public static DB userDb;

	private static int getIntOption(CommandLine cmd, String name, int defaultValue) {
		String val = cmd.getOptionValue(name);
		if (val == null)
			return defaultValue;

		return Integer.parseInt(val);
	}

	private static String getStringOption(CommandLine cmd, String name, String defaultValue) {
		String val = cmd.getOptionValue(name);
		if (val == null)
			return defaultValue;

		return val;
	}

	public static void main(String[] args) {
		Options options = new Options();

		options.addOption("pp", "pingPort", true, "The TCP port to receive pings on");
		options.addOption("hp", "httpPort", true, "The TCP port to receive http requests on");
		options.addOption("cd", "credentials", true, "Directory with the credentials for the server");
		options.addOption("db", "database", true, "The file name of the user database");
		options.addOption("l", "log", true, "The log file");
		options.addOption("cmd", "command", true, "The file to read startup commands from");

		CommandLineParser commandLineParser = new DefaultParser();
		HelpFormatter helpFormatter = new HelpFormatter();

		CommandLine cmd;

		try {
			cmd = commandLineParser.parse(options, args);
		} catch (ParseException e) {
			helpFormatter.printHelp("EPODServer", options);
			e.printStackTrace();
			return;
		}

		String logFile = getStringOption(cmd, "l", null);

		if (logFile != null) {
			try {
				Log.setFile(logFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		int httpPort = getIntOption(cmd, "hp", 80);
		int pingPort = getIntOption(cmd, "pp", 6241);
		String credentialDirectory = getStringOption(cmd, "cd", "credentials");

		String userDbFile = getStringOption(cmd, "db", "users.db");

		userDb = DBMaker
				.fileDB(userDbFile)
				.fileMmapEnableIfSupported()
				.fileMmapPreclearDisable()
				.make();

		userMap = userDb.hashMap("users", Serializer.LONG, User.SERIALIZER).createOrOpen();

		try {
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(
					Files.readAllBytes(Paths.get(credentialDirectory, "ping-privatekey"))));
			KeyFactory factory = KeyFactory.getInstance("RSA");
			privateKey = factory.generatePrivate(privateKeySpec);

			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(Files.readAllBytes(Paths.get(credentialDirectory, "ping-publickey")))); //Load key from file (no idea on android
			publicKey = factory.generatePublic(publicKeySpec);  // Create our encryption key
		} catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
			System.exit(9);
		}

		System.out.println("Creating outage detector");
		detector = new OutageDetector();
		detector.start();

		System.out.println("Creating connection manager");
		connectionManager = new ConnectionManager(pingPort);
		connectionManager.start();

		System.out.println("Creating http server");
		httpRequestServer = new HttpRequestServer(httpPort);
		httpRequestServer.start();



		boolean run = true;

		String commandFile = getStringOption(cmd, "cmd", null);

		if (commandFile != null) {
			try {
				BufferedReader in = new BufferedReader(new FileReader(commandFile));

				String line;
				while (run && (line = in.readLine()) != null) {
					System.out.println("> " + line);
					String[] command = line.split("\\s");

					if (doCommand(command)) {
						run = false;
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			Scanner in = new Scanner(System.in);

			while (run) {
				System.out.print("> ");
				String[] command = in.nextLine().split("\\s");

				if (doCommand(command)) {
					run = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Test.stopTest();

		connectionManager.done();
		detector.done();
		httpRequestServer.done();

		try {
			connectionManager.join();
			detector.join();
			httpRequestServer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		Log.close();
		userDb.close();
	}

	private static boolean doCommand(String[] command) {
		try {
			if (command.length > 0) {
				switch (command[0].toLowerCase()) {
					case "stop":
						return true;
					case "usercount":
						System.out.println("There are " + userMap.size() + " users stored");
						break;
					case "testusers":
						Test.createTestUsers(command[1], command[2]);
						break;
					case "loadtestusers":
						Test.loadTestUsers(command[1]);
						break;
					case "localoutage":
						Test.localOutage();
						break;
					case "trackeddevices":
						System.out.println(detector.devices.size());
						break;
					case "runtest":
						Test.runTest(Integer.parseInt(command[1]));
						break;
					case "stoptest":
						Test.stopTest();
						break;
					case "puid":
						System.out.printf("%016X\n", userMap.keySet().iterator().next());
						break;
					default:
						System.out.println("Unknown command: " + command[0]);
				}
			}
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return false;
		}

		return false;
	}
}
