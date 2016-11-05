package tk.wasdennnoch.androidn_ify.misc;

import android.view.View;

import tk.wasdennnoch.androidn_ify.XposedHook;

/**
 * An implementation of {@link View.OnLongClickListener} which catches
 * any exception thrown in {@link #onLongClickSafe(View)}.
 */
@SuppressWarnings("WeakerAccess")
public abstract class SafeOnLongClickListener implements View.OnLongClickListener {

    private String mLogTag;
    private String mLogMessage;

    /**
     * Constructs a {@code SafeOnLongClickListener} which eats any Throwable without logging it.
     */
    public SafeOnLongClickListener() {
    }

    /**
     * Constructs a {@code SafeOnLongClickListener} which logs thrown Throwables.
     *
     * @param logTag     The tag used to log
     * @param logMessage The message to log before the stack trace
     */
    public SafeOnLongClickListener(String logTag, String logMessage) {
        mLogTag = logTag;
        mLogMessage = logMessage;
    }

    /**
     * Override this method instead of {@link #onLongClick(View)} to have a crash protection.
     */
    public abstract boolean onLongClickSafe(View v);

    /**
     * DON'T OVERRIDE THIS! Override {@link #onLongClickSafe(View)} instead.
     */
    @Override
    public boolean onLongClick(View v) {
        try {
            return onLongClickSafe(v);
        } catch (Throwable t) {
            if (mLogTag != null) XposedHook.logE(mLogTag, mLogMessage, t);
            return false;
        }
    }

}
