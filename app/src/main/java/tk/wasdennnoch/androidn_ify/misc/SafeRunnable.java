package tk.wasdennnoch.androidn_ify.misc;

import tk.wasdennnoch.androidn_ify.XposedHook;

/**
 * An implementation of {@link Runnable} which catches any exception thrown
 * in {@link #runSafe()}. Useful in handlers to avoid crashing an app.
 */
@SuppressWarnings("WeakerAccess")
public abstract class SafeRunnable implements Runnable {

    private String mLogTag;
    private String mLogMessage;

    /**
     * Constructs a {@code SafeRunnable} which eats any Throwable without logging it.
     */
    public SafeRunnable() {
    }

    /**
     * Constructs a {@code SafeRunnable} which logs thrown Throwables.
     *
     * @param logTag The tag used to log
     * @param logMessage The message to log before the stack trace
     */
    public SafeRunnable(String logTag, String logMessage) {
        mLogTag = logTag;
        mLogMessage = logMessage;
    }

    /**
     * Override this method instead of {@link #run()} to have a crash protection.
     */
    public abstract void runSafe();

    /**
     * DON'T OVERRIDE THIS! Override {@link #runSafe()} instead.
     */
    @Override
    public void run() {
        try {
            runSafe();
        } catch (Throwable t) {
            if (mLogTag != null) XposedHook.logE(mLogTag, mLogMessage, t);
        }
    }

}
