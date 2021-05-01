package epod.platform;

import javax.swing.*;

public class JobScheduler implements AutoCloseable {
	public static final int NO_TIMEOUT = 0xFFFFFFFF;

	private static native long CreateWaitableTimer(boolean autoReset);

	private static native void CloseHandle(long handle);

	private static native int SetWaitableTimer(long handle, long dueTimeMillis, long periodMillis, boolean wake);

	private static native void CancelWaitableTimer(long handle);

	private static native boolean IsSystemResumeAutomatic();

	private static native boolean SetSuspendState(boolean hibernate, boolean disableWakeupEvents);

	private static native void WaitForSingleObject(long handle, int timeoutMillis);

	private static native boolean RequestPermissionToSleepIfNotGranted();

	private Job job;
	private long handle;
	private long periodMillis;
	private boolean wake;
	private boolean returnToSleep;

	private boolean started = false;

	public interface Job {
		boolean run();
	}

	public JobScheduler(Job job, long periodMillis, boolean wake, boolean returnToSleep) {
		handle = CreateWaitableTimer(true);
		setJob(job);
		setReturnToSleep(returnToSleep);
		setTimerInfo(periodMillis, wake);

	}

	public void setReturnToSleep(boolean returnToSleep) {
		this.returnToSleep = returnToSleep;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public void setPeriodMillis(long periodMillis) {
		this.periodMillis = periodMillis;

		if (started) {
			restartTimer();
		}
	}

	public void setWake(boolean wake) {
		this.wake = wake;

		if (started) {
			restartTimer();
		}
	}

	public void setTimerInfo(long periodMillis, boolean wake) {
		this.periodMillis = periodMillis;
		this.wake = wake;

		if (started) {
			restartTimer();
		}
	}

	private void restartTimer() {
		assert handle != 0;

		if (handle != 0) {
			SetWaitableTimer(handle, periodMillis, periodMillis, wake);
		}
	}

	public void start() {
		if (!started && handle != 0) {
			started = true;
			restartTimer();

			while (true) {
				WaitForSingleObject(handle, NO_TIMEOUT);

				boolean isSystemResumeAutomatic = IsSystemResumeAutomatic();

				if (!job.run()) {
					break;
				}


				if (returnToSleep && hasPermissionToSleep() && isSystemResumeAutomatic) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					SetSuspendState(false, false);
				}
			}

			CancelWaitableTimer(handle);
			started = false;

			if (returnToSleep && hasPermissionToSleep() && IsSystemResumeAutomatic()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				SetSuspendState(false, false);
			}
		}
	}

	private static boolean permissionToSleep;

	public static boolean hasPermissionToSleep() {
		return permissionToSleep;
	}

	@Override
	public void close() throws Exception {
		if (handle != 0)
			CloseHandle(handle);
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

}
