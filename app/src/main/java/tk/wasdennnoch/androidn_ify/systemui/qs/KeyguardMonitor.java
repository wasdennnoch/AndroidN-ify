package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class KeyguardMonitor {
    private static final String CLASS_KEYGUARD_MONITOR_CALLBACK = "com.android.systemui.statusbar.policy.KeyguardMonitor$Callback";
    private final Object mKeyguardMonitor;
    private final Context mContext;
    private final Object mCallback;

    private boolean mShowing;
    private boolean mSecure;
    private final List<Callback> mCallbacks = new ArrayList<>();
    private boolean mListening = false;

    public KeyguardMonitor(Context context, Object keyguardMonitor) {
        mContext = context;
        mKeyguardMonitor = keyguardMonitor;
        mCallback = createProxy();
    }

    public boolean isShowing() {
        return mShowing;
    }

    public boolean isSecure() {
        return mSecure;
    }

    public void addCallback(Callback callback) {
        synchronized (mCallbacks) {
            if (mCallbacks.contains(callback)) return;
            mCallbacks.add(callback);
            if (mCallbacks.size() != 0 && !mListening) {
                mListening = true;
                XposedHelpers.callMethod(mKeyguardMonitor, "addCallback", mCallback);
            }
        }
    }

    public void removeCallback(Callback callback) {
        synchronized (mCallbacks) {
            if (mCallbacks.remove(callback) && mCallbacks.size() == 0 && mListening) {
                mListening = false;
                XposedHelpers.callMethod(mKeyguardMonitor, "removeCallback", mCallback);
            }
        }
    }

    public Object createProxy() {
        Class<?> classCallback = XposedHelpers.findClass(CLASS_KEYGUARD_MONITOR_CALLBACK, mContext.getClassLoader());
        return Proxy.newProxyInstance(mContext.getClassLoader(), new Class<?>[]{classCallback}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                onKeyguardChanged();
                return null;
            }
        });
    }

    private void onKeyguardChanged() {
        mShowing = XposedHelpers.getBooleanField(mKeyguardMonitor, "mShowing");
        mSecure = XposedHelpers.getBooleanField(mKeyguardMonitor, "mSecure");
        notifyKeyguardChanged();
    }

    private void notifyKeyguardChanged() {
        synchronized (mCallbacks) {
            int size = mCallbacks.size();
            for (int i = 0; i < size; i++) {
                try {
                    Callback callback = mCallbacks.get(i);
                    callback.onKeyguardChanged();
                } catch (Throwable ignore) {

                }
            }
        }
    }

    public interface Callback {
        void onKeyguardChanged();
    }
}
