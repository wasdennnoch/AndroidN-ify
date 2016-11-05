package tk.wasdennnoch.androidn_ify.misc;

import android.view.View;

import tk.wasdennnoch.androidn_ify.XposedHook;

/**
 * An implementation of {@link View.OnClickListener} which catches
 * any exception thrown in {@link #onClickSafe(View)}.
 */
@SuppressWarnings("WeakerAccess")
public abstract class SafeOnClickListener implements View.OnClickListener {

    private String mLogTag;
    private String mLogMessage;

    /**
     * Constructs a {@code SafeOnClickListener} which eats any Throwable without logging it.
     */
    public SafeOnClickListener() {
    }

    /**
     * Constructs a {@code SafeOnClickListener} which logs thrown Throwables.
     *
     * @param logTag     The tag used to log
     * @param logMessage The message to log before the stack trace
     */
    public SafeOnClickListener(String logTag, String logMessage) {
        mLogTag = logTag;
        mLogMessage = logMessage;
    }

    /**
     * Override this method instead of {@link #onClick(View)} to have a crash protection.
     */
    public abstract void onClickSafe(View v);

    /**
     * DON'T OVERRIDE THIS! Override {@link #onClickSafe(View)} instead.
     */
    @Override
    public void onClick(View v) {
        try {
            onClickSafe(v);
        } catch (Throwable t) {
            if (mLogTag != null) XposedHook.logE(mLogTag, mLogMessage, t);
        }
    }

}
