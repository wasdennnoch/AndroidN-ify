package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.views.SensitiveFilterButton;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.SettingsUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;

public class SensitiveNotificationFilter {

    private static final String TAG = "SensitiveNotificationFilter";

    private static final String CLASS_BASE_STATUS_BAR = "com.android.systemui.statusbar.BaseStatusBar";
    private static final String CLASS_EXPANDABLE_NOTIFICATION_ROW = "com.android.systemui.statusbar.ExpandableNotificationRow";
    private static final String CLASS_TUNER_SERVICE = "com.android.systemui.tuner.TunerService";
    private static final String CLASS_TUNABLE = CLASS_TUNER_SERVICE + "$Tunable";

    private static final String SENSITIVE_FILTER = "sysui_sensitive_notifications_filter";

    private Context mContext;
    private Object mTunable;

    private List<String> mPkgs = new ArrayList<>();
    private List<WeakReference<SensitiveFilterListener>> mListeners = new ArrayList<>();
    private LayoutInflater mLayoutInflater;
    private int mButtonWidth = 0;

    private String mTogglePackage;
    private Runnable mTogglePackageRunnable = new Runnable() {
        @Override
        public void run() {
            togglePackageInternal(mTogglePackage);
        }
    };

    public void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.notifications().filter_sensitive_notifications) {
                Class<?> classBaseStatusBar = XposedHelpers.findClass(CLASS_BASE_STATUS_BAR, classLoader);
                Class<?> classExpandableNotificationRow = XposedHelpers.findClass(CLASS_EXPANDABLE_NOTIFICATION_ROW, classLoader);
                final Class<?> classTunerService = XposedHelpers.findClass(CLASS_TUNER_SERVICE, classLoader);
                Class<?> classTunable = XposedHelpers.findClass(CLASS_TUNABLE, classLoader);

                createTunable(classTunable, classLoader);

                XposedHelpers.findAndHookMethod(classBaseStatusBar, "start", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                        Object tunerService = XposedHelpers.callStaticMethod(classTunerService, "get", XposedHelpers.getObjectField(param.thisObject, "mContext"));
                        XposedHelpers.callMethod(tunerService, "addTunable", mTunable, SENSITIVE_FILTER);
                    }
                });

                XposedHelpers.findAndHookMethod(classBaseStatusBar, "bindGuts", classExpandableNotificationRow, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object row = param.args[0];
                        Object sbn = XposedHelpers.getObjectField(row, "mStatusBarNotification");
                        View guts = (View) XposedHelpers.getObjectField(row, "mGuts");
                        if (sbn == null || guts == null) return;

                        SensitiveFilterButton filterButton = (SensitiveFilterButton) guts.findViewById(R.id.notification_sensitive_filter);
                        if (filterButton == null) return;
                        filterButton.init(SensitiveNotificationFilter.this, sbn, row);
                    }
                });

                XposedHelpers.findAndHookMethod(classExpandableNotificationRow, "setSensitive", boolean.class, XC_MethodReplacement.DO_NOTHING);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking", t);
        }
    }

    public void hookRes(XC_InitPackageResources.InitPackageResourcesParam resparam) {
        try {
            if (ConfigUtils.notifications().filter_sensitive_notifications) {
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "notification_guts", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout layout = (LinearLayout) ((ViewGroup) liparam.view).getChildAt(0);
                        Context context = layout.getContext();

                        if (mButtonWidth == 0) {
                            mButtonWidth = ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.notification_filter_button_width);
                        }

                        SensitiveFilterButton button = (SensitiveFilterButton) getLayoutInflater(context)
                                .inflate(ResourceUtils.getInstance(context).getLayout(R.layout.notification_filter_button), null);

                        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(mButtonWidth, MATCH_PARENT);
                        buttonLp.weight = 0;

                        layout.addView(button, 2, buttonLp);
                    }
                });
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking res", t);
        }
    }

    private void createTunable(Class<?> classTunable, ClassLoader classLoader) {
        mTunable = Proxy.newProxyInstance(classLoader, new Class<?>[]{classTunable}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                switch (method.getName()) {
                    case "onTuningChanged":
                        onTuningChanged((String) args[0], (String) args[1]);
                        return null;
                }
                return null;
            }
        });
    }

    public void addListener(SensitiveFilterListener listener, String pkgName) {
        mListeners.add(new WeakReference<>(listener));
        listener.onPackageChanged(pkgName, mPkgs.contains(pkgName));
    }

    private void onTuningChanged(String key, String newValue) {
        if (!SENSITIVE_FILTER.equals(key)) return;
        loadPackagesFromList(newValue);
    }

    private void loadPackagesFromList(String sensitiveList) {
        if (sensitiveList == null)
            return;
        mPkgs.clear();
        for (String tile : sensitiveList.split(",")) {
            tile = tile.trim();
            if (tile.isEmpty()) continue;
            mPkgs.add(tile);
        }
    }

    public void togglePackage(String pkg) {
        mTogglePackage = pkg;
        SystemUIHooks.startRunnableDismissingKeyguard(mTogglePackageRunnable);
    }

    public void togglePackageInternal(String pkg) {
        if (mPkgs.contains(pkg))
            removePackage(pkg);
        else
            addPackage(pkg);
    }

    public void addPackage(String pkg) {
        mPkgs.add(pkg);
        saveChanges();
        notifyListeners(pkg, true);
    }

    public void removePackage(String pkg) {
        mPkgs.remove(pkg);
        saveChanges();
        notifyListeners(pkg, false);
    }

    private void notifyListeners(String pkg, boolean enabled) {
        int count = mListeners.size();
        for (int i = 0; i < count; i++) {
            SensitiveFilterListener listener = mListeners.get(i).get();
            if (listener == null) {
                mListeners.remove(i);
                i--;
                count--;
            } else {
                listener.onPackageChanged(pkg, enabled);
            }
        }
    }

    private void saveChanges() {
        String sensitiveList = TextUtils.join(",", mPkgs);
        XposedHook.logD(TAG, "saveChanges called with pkgs: " + sensitiveList);
        SettingsUtils.putStringForCurrentUser(mContext.getContentResolver(), SENSITIVE_FILTER, sensitiveList);
    }

    private LayoutInflater getLayoutInflater(Context context) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(context);
            mLayoutInflater.setFactory2(new LayoutInflater.Factory2() {
                @Override
                public View onCreateView(String name, Context context, AttributeSet attrs) {
                    if (name.equals(SensitiveFilterButton.class.getCanonicalName())) {
                        return new SensitiveFilterButton(context, attrs);
                    } else return null;
                }

                @Override
                public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                    return onCreateView(name, context, attrs);
                }
            });
        }
        return mLayoutInflater;
    }

    public interface SensitiveFilterListener {

        void onPackageChanged(String pkg, boolean enabled);
    }
}
