package epod.platform;

import java.io.File;
import java.util.Date;

public class Test {
	public static void main(String[] args) {
		JobScheduler jobScheduler = new JobScheduler(Test::run, 60_000, true, true);

		jobScheduler.start();
	}

	private static boolean run() {
		System.out.println(new Date());

		return !new File("stop.txt").exists();
	}
}
