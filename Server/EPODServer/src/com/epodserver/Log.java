package com.epodserver;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Log {

	private static PrintStream stream;

	public static void setFile(String file) throws FileNotFoundException {
		stream = new PrintStream(new FileOutputStream(file));
	}

	public static void close() {
		if (stream != null)
			stream.close();
	}

	public static void write(String format, Object... args) {
		if (stream != null)
			synchronized (stream) {
				stream.printf(format, args);
			}
	}
}
