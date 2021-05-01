#include "epod_platform_JobScheduler.h"
#include <Windows.h>
#include <powrprof.h>

VOID CALLBACK onTimer(LPVOID lpArg, DWORD dwTimerLowValue, DWORD dwTimerHighValue);

class TimerInfo {
public:
	TimerInfo(JNIEnv *env, jobject scheduler, jlong periodMillis, bool wake) :
		env(env), scheduler(scheduler) {
		handle = CreateWaitableTimer(NULL, FALSE, NULL);

		LARGE_INTEGER _dueTime;
		_dueTime.QuadPart = (LONG)(periodMillis * -10000LL);

		jclass clazz = env->GetObjectClass(scheduler);
		exec = env->GetMethodID(clazz, "exec", "(Z)Z");

		SetWaitableTimer(handle, &_dueTime, (LONG)periodMillis, onTimer, this, wake);
		SleepEx(INFINITE, TRUE);
	}

	~TimerInfo() {
		CloseHandle(handle);
	}

	bool call(bool automatic) {
		return (bool)env->CallBooleanMethod(scheduler, exec, (jboolean)automatic);
	}

private:
	HANDLE handle;
	JNIEnv *env;
	jobject scheduler;
	jmethodID exec;


};

VOID CALLBACK onTimer(LPVOID lpArg, DWORD dwTimerLowValue, DWORD dwTimerHighValue) {
	bool automatic = IsSystemResumeAutomatic();

	UNREFERENCED_PARAMETER(dwTimerLowValue);
	UNREFERENCED_PARAMETER(dwTimerHighValue);
	
	if (!((TimerInfo*)lpArg)->call(automatic)) {
		SleepEx(INFINITE, TRUE);
	}

}

JNIEXPORT jlong JNICALL Java_epod_platform_JobScheduler_CreateWaitableTimer
(JNIEnv *env, jobject p_this, jlong periodMillis, jboolean wake) {
	return (jlong)new TimerInfo(env, p_this, periodMillis, wake);
}

JNIEXPORT void JNICALL Java_epod_platform_JobScheduler_DestroyWaitableTimer
(JNIEnv *, jclass, jlong handle) {
	delete (TimerInfo*)handle;
}

JNIEXPORT void JNICALL Java_epod_platform_JobScheduler_SetSuspendState
(JNIEnv *, jclass, jboolean hibernate, jboolean disableWakeupEvents) {
	SetSuspendState(FALSE, TRUE, FALSE);
}

JNIEXPORT jboolean JNICALL Java_epod_platform_JobScheduler_RequestPermissionToSleepIfNotGranted
(JNIEnv *, jclass) {
	HANDLE hToken;
	TOKEN_PRIVILEGES tkp;

	// Get a token for this process. 

	if (!OpenProcessToken(GetCurrentProcess(),
		TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &hToken))
		return(FALSE);

	BOOL privilegeResult;
	PRIVILEGE_SET requiredPriveleges;
	requiredPriveleges.Control = PRIVILEGE_SET_ALL_NECESSARY;
	requiredPriveleges.PrivilegeCount = 1;
	LookupPrivilegeValue(NULL, SE_SHUTDOWN_NAME,
		&requiredPriveleges.Privilege[0].Luid);
	requiredPriveleges.Privilege[0].Attributes = SE_PRIVILEGE_ENABLED;


	PrivilegeCheck(hToken, &requiredPriveleges, &privilegeResult);
	if (privilegeResult)
		return true;

	// Get the LUID for the shutdown privilege. 

	LookupPrivilegeValue(NULL, SE_SHUTDOWN_NAME,
		&tkp.Privileges[0].Luid);

	tkp.PrivilegeCount = 1;  // one privilege to set    
	tkp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;

	// Get the shutdown privilege for this process. 
	GetLastError();

	AdjustTokenPrivileges(hToken, FALSE, &tkp, 0,
		(PTOKEN_PRIVILEGES)NULL, 0);

	if (GetLastError() != ERROR_SUCCESS) {
		return false;
	}

	return true;
}