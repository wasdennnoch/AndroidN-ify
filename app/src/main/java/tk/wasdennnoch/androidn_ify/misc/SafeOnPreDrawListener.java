package tk.wasdennnoch.androidn_ify.misc;

import android.view.ViewTreeObserver;

import tk.wasdennnoch.androidn_ify.XposedHook;

/**
 * An implementation of {@link ViewTreeObserver.OnPreDrawListener} which catches
 * any exception thrown in {@link #onPreDraw()}.
 */
@SuppressWarnings("WeakerAccess")
public abstract class SafeOnPreDrawListener implements ViewTreeObserver.OnPreDrawListener {

    private String mLogTag;
    private String mLogMessage;

    /**
     * Constructs a {@code SafeOnPreDrawListener} which eats any Throwable without logging it.
     */
    public SafeOnPreDrawListener() {
    }

    /**
     * Constructs a {@code SafeOnPreDrawListener} which logs thrown Throwables.
     *
     * @param logTag     The tag used to log
     * @param logMessage The message to log before the stack trace
     */
    public SafeOnPreDrawListener(String logTag, String logMessage) {
        mLogTag = logTag;
        mLogMessage = logMessage;
    }

    /**
     * Override this method instead of {@link #onPreDraw()} to have a crash protection.
     */
    public abstract boolean onPreDrawSafe();

    /**
     * DON'T OVERRIDE THIS! Override {@link #onPreDrawSafe()} instead.
     */
    @Override
    public boolean onPreDraw() {
        try {
            return onPreDrawSafe();
        } catch (Throwable t) {
            if (mLogTag != null) XposedHook.logE(mLogTag, mLogMessage, t);
            return false;
        }
    }

}
