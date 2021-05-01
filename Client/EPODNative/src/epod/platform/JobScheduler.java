package epod.platform;

import javax.swing.*;

public class JobScheduler implements AutoCloseable {
	public static final int NO_TIMEOUT = 0xFFFFFFFF;

	private native long CreateWaitableTimer(long periodMillis, boolean wake);

	private static native void DestroyWaitableTimer(long handle);

	private static native void SetSuspendState();

	private static native boolean RequestPermissionToSleepIfNotGranted();

	private Job job;
	private long handle;
	private long periodMillis;
	private boolean wake;
	private boolean returnToSleep;

	public interface Job {
		boolean run();
	}

	public JobScheduler(Job job, long periodMillis, boolean wake, boolean returnToSleep) {
		this.job = job;
		this.returnToSleep = returnToSleep;
		this.periodMillis = periodMillis;
		this.wake = wake;

	}


	public void start() {
		if (handle == 0) {
			handle = CreateWaitableTimer(periodMillis, wake);
		}
	}

	private boolean exec(boolean isSystemResumeAutomatic) {
		boolean done = false;

		if (!job.run()) {
			done = true;
			DestroyWaitableTimer(handle);
			handle = 0;
		}

		System.out.println(isSystemResumeAutomatic);

		if (returnToSleep && hasPermissionToSleep() && isSystemResumeAutomatic) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			SetSuspendState();
		}

		return done;
	}

	private static boolean permissionToSleep;

	public static boolean hasPermissionToSleep() {
		return permissionToSleep;
	}

	@Override
	public void close() throws Exception {
		if (handle != 0)
			handle = 0;
			DestroyWaitableTimer(handle);
	}

	static  {
		if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
			JOptionPane.showMessageDialog(null, "Only Windows is supported!", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(5);
		}

		String arch = System.getProperty("os.arch");

		if (arch.contains("64")) {
			System.loadLibrary("bin/win64/EPODWin");
		} else if (arch.contains("86")) {
			System.loadLibrary("bin/win32/EPODWin");
		} else {
			JOptionPane.showMessageDialog(null, "Unsupported architecture!", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(6);
		}

		permissionToSleep = RequestPermissionToSleepIfNotGranted();
	}

	public static void main(String[] args) {
		System.out.println(Integer.parseInt("\u0966"));
		System.out.println((byte) (5 * -9521));
	}
}
