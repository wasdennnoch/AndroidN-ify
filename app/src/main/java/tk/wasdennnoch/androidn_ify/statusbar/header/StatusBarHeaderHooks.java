package tk.wasdennnoch.androidn_ify.statusbar.header;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class StatusBarHeaderHooks {

    private static final String TAG = "StatusBarHeaderHooks";

    private static final String CLASS_STATUS_BAR_HEADER_VIEW = "com.android.systemui.statusbar.phone.StatusBarHeaderView";
    private static final String CLASS_LAYOUT_VALUES = "com.android.systemui.statusbar.phone.StatusBarHeaderView$LayoutValues";

    private static Object mCollapsedValues;
    private static Object mExpandedValues;

    private static AdditionalLayoutValues mAdditionalCollapsedValues = new AdditionalLayoutValues(AdditionalLayoutValues.TYPE_COLLAPSED);
    private static AdditionalLayoutValues mAdditionalExpandedValues = new AdditionalLayoutValues(AdditionalLayoutValues.TYPE_EXPANDED);
    private static AdditionalLayoutValues mAdditionalCurrentValues = new AdditionalLayoutValues(AdditionalLayoutValues.TYPE_CURRENT);

    private static View mSystemIconsSuperContainer;
    private static View mDateGroup;
    private static View mClock;
    private static FrameLayout mMultiUserSwitch;
    private static ImageView mMultiUserAvatar;
    private static TextView mDateCollapsed;
    private static TextView mDateExpanded;
    private static View mSettingsContainer;
    private static TextView mEmergencyCallsOnly;
    private static TextView mAlarmStatus;

    private static XC_MethodHook onFinishInflateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

            try {
                mCollapsedValues = XposedHelpers.getObjectField(param.thisObject, "mCollapsedValues");
                mExpandedValues = XposedHelpers.getObjectField(param.thisObject, "mExpandedValues");
            } catch (Throwable t) {
                // Since we need those objects to determine which value gets captured later on
                // and couldn't find them, abort. The new values wouldn't be applied anyways.
                XposedHook.logE(TAG, "Couldn't find mCollapsedValues or mExpandedValues, aborting", t);
                return;
            }

            try {
                mSystemIconsSuperContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mSystemIconsSuperContainer");
                mDateGroup = (View) XposedHelpers.getObjectField(param.thisObject, "mDateGroup");
                mClock = (View) XposedHelpers.getObjectField(param.thisObject, "mClock");
                mMultiUserSwitch = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mMultiUserSwitch");
                mMultiUserAvatar = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMultiUserAvatar");
                mDateCollapsed = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateCollapsed");
                mDateExpanded = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDateExpanded");
                mSettingsContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mSettingsContainer");
                mEmergencyCallsOnly = (TextView) XposedHelpers.getObjectField(param.thisObject, "mEmergencyCallsOnly");
                mAlarmStatus = (TextView) XposedHelpers.getObjectField(param.thisObject, "mAlarmStatus");
            } catch (Throwable t) {
                // try-catch for every single view would be overkill, I'll sort them by fail count after the release.
                // TODO fail-count sorting in multiple try-catches
                // Another problem would be that I can't move a view when I need the ID of another view that I couldn't find.
                // That would efficiently break most of the repositioning.
                XposedHook.logE(TAG, "Couldn't find required views, aborting", t);
                return;
            }

            mSystemIconsSuperContainer.setVisibility(View.GONE);

            try {
                RelativeLayout.LayoutParams dateParams = (RelativeLayout.LayoutParams) mDateGroup.getLayoutParams();
                dateParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                dateParams.addRule(RelativeLayout.RIGHT_OF, mClock.getId());

                RelativeLayout.LayoutParams clockParams = (RelativeLayout.LayoutParams) mClock.getLayoutParams();
                clockParams.removeRule(RelativeLayout.ABOVE);
                clockParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

                RelativeLayout.LayoutParams switchParams = (RelativeLayout.LayoutParams) mMultiUserSwitch.getLayoutParams();
                switchParams.removeRule(RelativeLayout.ALIGN_PARENT_END);
                switchParams.addRule(RelativeLayout.LEFT_OF, mSettingsContainer.getId());
                //switchParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                //FrameLayout.LayoutParams dateCollapsedParams = (FrameLayout.LayoutParams) mDateCollapsed.getLayoutParams();
                //dateCollapsedParams.removeRule(RelativeLayout.BELOW);

                //FrameLayout.LayoutParams dateExpandedParams = (FrameLayout.LayoutParams) mDateExpanded.getLayoutParams();
                //dateExpandedParams.removeRule(RelativeLayout.BELOW);

                RelativeLayout.LayoutParams settingsParams = (RelativeLayout.LayoutParams) mSettingsContainer.getLayoutParams();
                settingsParams.removeRule(RelativeLayout.START_OF);
                settingsParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                //settingsParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                RelativeLayout.LayoutParams emergencyParams = (RelativeLayout.LayoutParams) mEmergencyCallsOnly.getLayoutParams();
                emergencyParams.addRule(RelativeLayout.START_OF, mMultiUserSwitch.getId());

                // TODO why is this not working?!? The View just stays in the upper left corner no matter what I do
                RelativeLayout.LayoutParams alarmParams = (RelativeLayout.LayoutParams) mAlarmStatus.getLayoutParams();
                alarmParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                //alarmParams.removeRule(RelativeLayout.END_OF);
                //alarmParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                //alarmParams.addRule(RelativeLayout.BELOW, mClock.getId());
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error modifying LayoutParams", t);
            }

        }
    };
    private static XC_MethodHook captureLayoutValuesHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

            Object target = param.args[0];

            if (target == mCollapsedValues) { // TODO move to resources, create ResourceUtils
                mAdditionalCollapsedValues.avatarAlpha = 0f;
                mAdditionalCollapsedValues.avatarTranslationY = -120f;
                mAdditionalCollapsedValues.settingsTranslationY = -120f;
                XposedHelpers.setObjectField(target, "settingsRotation", -90f);
            } else if (target == mExpandedValues) {
                mAdditionalExpandedValues.avatarAlpha = 1f;
                mAdditionalExpandedValues.avatarTranslationY = 0f;
                mAdditionalExpandedValues.settingsTranslationY = 0f;
                XposedHelpers.setObjectField(target, "settingsRotation", 0f);
            }
            XposedHelpers.setObjectField(target, "settingsTranslation", 0f);

            mSystemIconsSuperContainer.setVisibility(View.GONE); // TODO testing here

        }
    };
    private static XC_MethodHook applyLayoutValuesHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

            // Better check if null than flooding the log with an NPE every frame update
            if (mMultiUserAvatar != null) {
                XposedHelpers.callMethod(param.thisObject, "applyAlpha", mMultiUserAvatar, mAdditionalCurrentValues.avatarAlpha);
                mMultiUserAvatar.setTranslationY(mAdditionalCurrentValues.avatarTranslationY);
            }
            if (mSettingsContainer != null)
                mSettingsContainer.setTranslationY(mAdditionalCurrentValues.settingsTranslationY);

        }
    };
    private static XC_MethodHook interpolateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

            mAdditionalCurrentValues.interpolate(mAdditionalCollapsedValues, mAdditionalExpandedValues, (float) param.args[2]);

        }
    };

    public static void hook(ClassLoader classLoader, XSharedPreferences prefs) {
        try {
            if (prefs.getBoolean("enable_notification_tweaks", true)) {

                Class<?> classStatusBarHeaderView = XposedHelpers.findClass(CLASS_STATUS_BAR_HEADER_VIEW, classLoader);

                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "onFinishInflate", onFinishInflateHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "captureLayoutValues", CLASS_LAYOUT_VALUES, captureLayoutValuesHook);
                XposedHelpers.findAndHookMethod(classStatusBarHeaderView, "applyLayoutValues", CLASS_LAYOUT_VALUES, applyLayoutValuesHook);
                // Yes, this is a typo. Not my typo though. A typo in the source code that nobody noticed and that got compiled. ("interpoloate")
                XposedHelpers.findAndHookMethod(CLASS_LAYOUT_VALUES, classLoader, "interpoloate", CLASS_LAYOUT_VALUES, CLASS_LAYOUT_VALUES, float.class, interpolateHook);

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    private static final class AdditionalLayoutValues {

        private static final byte TYPE_COLLAPSED = 1;
        private static final byte TYPE_EXPANDED = 2;
        private static final byte TYPE_CURRENT = 3;
        private byte mType;

        float avatarAlpha;
        float avatarTranslationY;
        float settingsTranslationY;

        public AdditionalLayoutValues(byte type) {
            mType = type;
        }

        // I was so kind to fix the typo here...
        public void interpolate(AdditionalLayoutValues v1, AdditionalLayoutValues v2, float t) {

            avatarTranslationY = v1.avatarTranslationY * (1 - t) + v2.avatarTranslationY * t;
            settingsTranslationY = v1.settingsTranslationY * (1 - t) + v2.settingsTranslationY * t;

            float t3 = Math.max(0, t - 0.7f) / 0.3f;
            avatarAlpha = v1.avatarAlpha * (1 - t3) + v2.avatarAlpha * t3;

        }

        @Override
        public String toString() {
            switch (mType) {
                case TYPE_COLLAPSED:
                    return "COLLAPSED";
                case TYPE_EXPANDED:
                    return "EXPANDED";
                case TYPE_CURRENT:
                    return "CURRENT";
                default:
                    return "UNKNOWN";
            }
        }

    }

}
