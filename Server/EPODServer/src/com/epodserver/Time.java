package com.epodserver;

import java.time.Instant;

public class Time {
	public static long getSeconds() {
		return Instant.now().getEpochSecond();
	}
}
