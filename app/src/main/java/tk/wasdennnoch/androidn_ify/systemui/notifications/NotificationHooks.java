package tk.wasdennnoch.androidn_ify.systemui.notifications;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.graphics.ColorUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DateTimeView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.extracted.systemui.FakeShadowView;
import tk.wasdennnoch.androidn_ify.extracted.systemui.NotificationActionListLayout;
import tk.wasdennnoch.androidn_ify.extracted.systemui.RemoteInputView;
import tk.wasdennnoch.androidn_ify.misc.SafeOnClickListener;
import tk.wasdennnoch.androidn_ify.systemui.SystemUIHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.views.RemoteInputHelper;
import tk.wasdennnoch.androidn_ify.systemui.qs.customize.QSCustomizer;
import tk.wasdennnoch.androidn_ify.systemui.statusbar.StatusBarHooks;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.RemoteMarginLinearLayout;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

@SuppressLint("StaticFieldLeak")
public class NotificationHooks {

    private static final String TAG = "NotificationHooks";

    private static final String PACKAGE_ANDROID = XposedHook.PACKAGE_ANDROID;
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;
    private static final String KEY_EXPAND_CLICK_LISTENER = "expandClickListener";
    public static final String EXTRA_SUBSTITUTE_APP_NAME = "nify.substName";

    private static int mNotificationBgColor;
    private static int mAccentColor = 0;
    private static final Map<String, Integer> mGeneratedColors = new HashMap<>();

    private static Object mPhoneStatusBar;

    public static boolean remoteInputActive = false;
    public static Object statusBarWindowManager = null;
    public static NotificationStackScrollLayoutHooks mStackScrollLayoutHooks;

    private static SensitiveNotificationFilter mSensitiveFilter = new SensitiveNotificationFilter();

    private static final XC_MethodHook inflateViewsHook = new XC_MethodHook() {

        @SuppressWarnings({"deprecation", "UnusedAssignment"})
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!(boolean) param.getResult()) return;

            Object entry = param.args[0];
            Object row = XposedHelpers.getObjectField(entry, "row");
            Object contentContainer = XposedHelpers.getObjectField(row, "mPrivateLayout"); // NotificationContentView
            Object contentContainerPublic = XposedHelpers.getObjectField(row, "mPublicLayout");

            ConfigUtils.notifications().loadBlacklistedApps();
            StatusBarNotification sbn = (StatusBarNotification) XposedHelpers.callMethod(row, "getStatusBarNotification");
            if (ConfigUtils.notifications().blacklistedApps.contains(sbn.getPackageName())) return;

            View privateView = (View) XposedHelpers.callMethod(contentContainer, "getContractedChild");
            View publicView = (View) XposedHelpers.callMethod(contentContainerPublic, "getContractedChild");

            Context context = publicView.getContext();

            // Try to find app label for notifications without public version
            TextView appName = (TextView) publicView.findViewById(R.id.public_app_name_text);
            if (appName == null) {
                // For notifications with public version
                appName = (TextView) publicView.findViewById(R.id.app_name_text);
            }

            View time = publicView.findViewById(context.getResources().getIdentifier("time", "id", PACKAGE_SYSTEMUI));
            if (time != null) {
                publicView.findViewById(R.id.public_time_divider).setVisibility(time.getVisibility());
            }

            // Try to find icon for notifications without public version
            ImageView icon = (ImageView) publicView.findViewById(context.getResources().getIdentifier("icon", "id", PACKAGE_SYSTEMUI));
            if (icon == null) {
                // For notifications with public version
                icon = (ImageView) publicView.findViewById(R.id.notification_icon);
            }
            if (icon == null) {
                icon = (ImageView) publicView.findViewById(android.R.id.icon);
            }
            if (icon != null) {
                icon.setBackgroundResource(0);
                icon.setBackgroundColor(0x00000000);
                icon.setPadding(0, 0, 0, 0);
            }

            TextView privateAppName = (TextView) privateView.findViewById(R.id.app_name_text);
            int color = privateAppName != null ? privateAppName.getTextColors().getDefaultColor() : sbn.getNotification().color;
            if (privateAppName != null) {
                if (appName != null) {
                    appName.setTextColor(privateAppName.getTextColors());
                    appName.setText(privateAppName.getText());
                }
                if (icon != null) {
                    icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                }
            }

            // actions background
            View expandedChild = (View) XposedHelpers.callMethod(contentContainer, "getExpandedChild");
            View headsUpChild = ConfigUtils.M ? (View) XposedHelpers.callMethod(contentContainer, "getHeadsUpChild") : null;
            if (!ConfigUtils.notifications().custom_actions_color) {
                if (expandedChild != null || headsUpChild != null) {
                    int actionsId = context.getResources().getIdentifier("actions", "id", PACKAGE_ANDROID);
                    double[] lab = new double[3];
                    ColorUtils.colorToLAB(mNotificationBgColor, lab);
                    lab[0] = 1.0f - 0.95f * (1.0f - lab[0]);
                    int endColor = ColorUtils.setAlphaComponent(ColorUtils.LABToColor(lab[0], lab[1], lab[2]), Color.alpha(mNotificationBgColor));
                    if (expandedChild != null) {
                        View actionsExpanded = expandedChild.findViewById(actionsId);
                        if (actionsExpanded != null) {
                            actionsExpanded.setBackgroundColor(endColor);
                        }
                    }
                    if (headsUpChild != null) {
                        View actionsHeadsUp = headsUpChild.findViewById(actionsId);
                        if (actionsHeadsUp != null) {
                            actionsHeadsUp.setBackgroundColor(endColor);
                        }
                    }
                }
            }

            if (RemoteInputHelper.DIRECT_REPLY_ENABLED) {
                Notification.Action[] actions = sbn.getNotification().actions;
                if (actions != null) {
                    addRemoteInput(context, expandedChild, actions, color, null, null);
                    if (ConfigUtils.M)
                        addRemoteInput(context, headsUpChild, actions, color, getObjectField(param.thisObject, "mHeadsUpManager"), (String) getObjectField(entry, "key"));
                }
            }
        }
    };

    private static void addRemoteInput(Context context, View child, Notification.Action[] actions, int color, final Object headsUpManager, final String key) {
        if (child == null) {
            return;
        }
        NotificationActionListLayout actionsLayout = (NotificationActionListLayout) child.findViewById(context.getResources().getIdentifier("actions", "id", PACKAGE_ANDROID));
        if (actionsLayout == null) {
            return;
        }
        FrameLayout actionContainer = new FrameLayout(context);

        // Transfer views
        int startMargin = ((ViewGroup.MarginLayoutParams) actionsLayout.getLayoutParams()).getMarginStart();
        ViewGroup parent = (ViewGroup) actionsLayout.getParent();
        parent.removeView(actionsLayout);
        parent.addView(actionContainer, MATCH_PARENT, WRAP_CONTENT);
        actionContainer.addView(actionsLayout);
        ViewUtils.setMarginStart(actionsLayout, startMargin);

        // Add remote input
        if (haveRemoteInput(actions)) {
            LinearLayout riv = RemoteInputView.inflate(context, actionContainer);
            riv.setVisibility(View.INVISIBLE);
            actionContainer.addView(riv, new FrameLayout.LayoutParams(
                    MATCH_PARENT,
                    MATCH_PARENT)
            );
            riv.setBackgroundColor(color);
        }

        for (int i = 0; i < actions.length; i++) {
            final Notification.Action action = actions[i];
            if (actions[i].getRemoteInputs() != null) {
                Button actionButton = (Button) actionsLayout.getChildAt(i);
                final View.OnClickListener old = (View.OnClickListener) getObjectField(XposedHelpers.callMethod(actionButton, "getListenerInfo"), "mOnClickListener");
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        if (ConfigUtils.notifications().allow_direct_reply_on_keyguard) {
                            handleRemoteInput(view);
                        } else {
                            SystemUIHooks.startRunnableDismissingKeyguard(new Runnable() {
                                @Override
                                public void run() {
                                    SystemUIHooks.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            handleRemoteInput(view);
                                        }
                                    });
                                }
                            });
                        }
                    }

                    private void handleRemoteInput(View view) {
                        Object headsUpEntry = headsUpManager != null ? XposedHelpers.callMethod(headsUpManager, "getHeadsUpEntry", key) : null;
                        if (!RemoteInputHelper.handleRemoteInput(view, action.actionIntent, action.getRemoteInputs(), headsUpEntry)) {
                            old.onClick(view);
                        }
                    }
                });
            }
        }
    }

    private static final XC_MethodHook getStandardViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews contentView = (RemoteViews) param.getResult();
            Notification.Builder mBuilder = (Notification.Builder) XposedHelpers.getObjectField(param.thisObject, "mBuilder");

            CharSequence overflowText = (CharSequence) (XposedHelpers.getBooleanField(param.thisObject, "mSummaryTextSet")
                    ? XposedHelpers.getObjectField(param.thisObject, "mSummaryText")
                    : XposedHelpers.getObjectField(mBuilder, "mSubText"));
            if (overflowText != null) {
                contentView.setTextViewText(R.id.header_text, processLegacyText(mBuilder, overflowText));
                contentView.setViewVisibility(R.id.header_text_divider, View.VISIBLE);
                contentView.setViewVisibility(R.id.header_text, View.VISIBLE);
            }
        }
    };

    private static CharSequence processLegacyText(Object notifBuilder, CharSequence text) {
        try {
            return (CharSequence) XposedHelpers.callMethod(notifBuilder, "processLegacyText", text);
        } catch (Throwable t) {
            return (CharSequence) XposedHelpers.callMethod(notifBuilder, "processText",
                    XposedHelpers.callMethod(notifBuilder, "getTextColor", 255), text);
        }
    }

    private static void bindNotificationHeader(RemoteViews contentView, XC_MethodHook.MethodHookParam param) {
        Object builder = param.thisObject;
        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
        Bundle extras = (Bundle) XposedHelpers.getObjectField(builder, "mExtras");
        int color = resolveColor(builder);
        bindSmallIcon(contentView, builder, color);
        bindHeaderAppName(contentView, context, color, extras);
        bindHeaderText(contentView, builder, context);
        bindHeaderChronometerAndTime(contentView, builder);
        bindExpandButton(contentView, color);
    }

    private static void bindHeaderChronometerAndTime(RemoteViews contentView, Object builder) {
        long mWhen = XposedHelpers.getLongField(builder, "mWhen");
        if ((boolean) XposedHelpers.callMethod(builder, "showsTimeOrChronometer")) {
            contentView.setViewVisibility(R.id.time_divider, View.VISIBLE);
            if (XposedHelpers.getBooleanField(builder, "mUseChronometer")) {
                contentView.setViewVisibility(R.id.chronometer, View.VISIBLE);
                contentView.setLong(R.id.chronometer, "setBase",
                        mWhen + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                contentView.setBoolean(R.id.chronometer, "setStarted", true);
            } else {
                contentView.setViewVisibility(R.id.time, View.VISIBLE);
                contentView.setLong(R.id.time, "setTime", mWhen);
            }
        } else {
            contentView.setLong(R.id.time, "setTime", mWhen);
        }
    }

    private static void bindHeaderText(RemoteViews contentView, Object builder, Context context) {
        CharSequence mContentText = (CharSequence) XposedHelpers.getObjectField(builder, "mContentText");
        CharSequence mSubText = (CharSequence) XposedHelpers.getObjectField(builder, "mSubText");
        if (mContentText != null && mSubText != null) {
            contentView.setTextViewText(context.getResources().getIdentifier("text", "id", PACKAGE_ANDROID),
                    processLegacyText(builder, mContentText));
            contentView.setViewVisibility(context.getResources().getIdentifier("text2", "id", PACKAGE_ANDROID), View.GONE);
            contentView.setTextViewText(R.id.header_text, processLegacyText(builder, mSubText));
            contentView.setViewVisibility(R.id.header_text, View.VISIBLE);
            contentView.setViewVisibility(R.id.header_text_divider, View.VISIBLE);
        }
        try {
            // TODO Why it's crashing here
            XposedHelpers.callMethod(builder, "unshrinkLine3Text");
        } catch (Throwable ignore) {

        }
    }

    private static void bindSmallIcon(RemoteViews contentView, Object builder, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Icon mSmallIcon = (Icon) XposedHelpers.getObjectField(builder, "mSmallIcon");
            contentView.setImageViewIcon(R.id.icon, mSmallIcon);
        } else {
            int mSmallIcon = XposedHelpers.getIntField(builder, "mSmallIcon");
            contentView.setImageViewResource(R.id.icon, mSmallIcon);
        }

        processSmallIconColor(contentView, builder, color);
    }

    private static void bindHeaderAppName(RemoteViews contentView, Context context, int color, Bundle extras) {
        contentView.setTextViewText(R.id.app_name_text, loadHeaderAppName(context, extras));
        contentView.setTextColor(R.id.app_name_text, color);
    }

    private static void processSmallIconColor(RemoteViews contentView, Object builder, int resolveColor) {
        boolean colorable = true;
        int color = NotificationHeaderView.NO_COLOR;
        Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
        boolean legacy = false;
        try {
            legacy = (boolean) XposedHelpers.callMethod(builder, "isLegacy");
        } catch (Throwable ignore) {
        }
        if (legacy) {
            Object mColorUtil = XposedHelpers.getObjectField(builder, "mColorUtil");
            Object mSmallIcon = XposedHelpers.getObjectField(builder, "mSmallIcon"); // Icon if Marshmallow, int if Lollipop. So we shouldn't specify which type is this.
            if (!(boolean) XposedHelpers.callMethod(mColorUtil, "isGrayscaleIcon", context, mSmallIcon)) {
                colorable = false;
            }
        }

        if (colorable) {
            color = resolveColor;
            XposedHelpers.callMethod(contentView, "setDrawableParameters", R.id.icon, false, -1, color,
                    PorterDuff.Mode.SRC_ATOP, -1);
        }

        contentView.setInt(R.id.notification_header, "setOriginalIconColor", color);
    }

    private static void bindExpandButton(RemoteViews contentView, int color) {
        XposedHelpers.callMethod(contentView, "setDrawableParameters", R.id.expand_button, false, -1, color,
                PorterDuff.Mode.SRC_ATOP, -1);
        contentView.setInt(R.id.notification_header, "setOriginalIconColor", color);
    }

    private static int resolveColor(Object builder) {
        return (int) XposedHelpers.callMethod(builder, "resolveColor");
    }

    private static String loadHeaderAppName(Context context, Bundle extras) {
        if (extras != null && extras.containsKey(EXTRA_SUBSTITUTE_APP_NAME)) {
            // TODO why this doesn't work
            final String pkg = context.getPackageName();
            final String subName = extras.getString(EXTRA_SUBSTITUTE_APP_NAME);
            if (pkg.equals(XposedHook.PACKAGE_OWN)) {
                return subName;
            }
        }

        CharSequence appname = context.getPackageName();
        if (appname.equals(PACKAGE_SYSTEMUI))
            return context.getString(context.getResources().getIdentifier("android_system_label", "string", PACKAGE_ANDROID));
        try {
            appname = context.getString(context.getApplicationInfo().labelRes);
        } catch (Throwable t) {
            try {
                appname = context.getApplicationInfo().loadLabel(context.getPackageManager());
            } catch (Throwable ignore) {
            }
        }

        return String.valueOf(appname);
    }

    private static void handleProgressBar(boolean hasProgress, RemoteViews contentView, Object builder, Resources res) {
        final int max = XposedHelpers.getIntField(builder, "mProgressMax");
        final boolean ind = XposedHelpers.getBooleanField(builder, "mProgressIndeterminate");
        if (hasProgress && (max != 0 || ind)) {
            CharSequence text = (CharSequence) XposedHelpers.getObjectField(builder, "mContentText");
            contentView.setTextViewText(R.id.text_line_1, text);
            contentView.setViewVisibility(R.id.text_line_1, View.VISIBLE);
            contentView.setViewVisibility(res.getIdentifier("line3", "id", PACKAGE_ANDROID), View.GONE);
        }
    }

    private static final XC_MethodHook applyStandardTemplateHook = new XC_MethodHook() {

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object builder = param.thisObject;
            Context context = (Context) XposedHelpers.getObjectField(builder, "mContext");
            Resources res = context.getResources();
            RemoteViews contentView = (RemoteViews) param.getResult();

            bindNotificationHeader(contentView, param);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Icon mLargeIcon = (Icon) XposedHelpers.getObjectField(builder, "mLargeIcon");
                contentView.setImageViewIcon(res.getIdentifier("right_icon", "id", "android"), mLargeIcon);
            } else {
                Bitmap mLargeIcon = (Bitmap) XposedHelpers.getObjectField(builder, "mLargeIcon");
                contentView.setImageViewBitmap(res.getIdentifier("right_icon", "id", PACKAGE_ANDROID), mLargeIcon);
            }

            if (XposedHelpers.getObjectField(builder, "mLargeIcon") != null) {
                int notificationTextMarginEnd = R.dimen.notification_text_margin_end;
                contentView.setInt(R.id.line1_container, "setMarginEnd", notificationTextMarginEnd);
                contentView.setInt(R.id.line2_container, "setMarginEnd", notificationTextMarginEnd);
                contentView.setInt(R.id.line3_container, "setMarginEnd", notificationTextMarginEnd);
            }

            handleProgressBar((boolean) param.args[1], contentView, builder, res);

            contentView.setInt(res.getIdentifier("right_icon", "id", "android"), "setBackgroundResource", 0);
        }
    };

    private static final XC_MethodHook makeMediaContentViewHook = new XC_MethodHook() {

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mBuilder"), "mContext");
            RemoteViews view = (RemoteViews) param.getResult();

            int endMargin = R.dimen.notification_content_margin_end;
            if (XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mBuilder"), "mLargeIcon") != null) {
                endMargin = R.dimen.notification_content_plus_picture_margin_end;
                view.setInt(R.id.line1_container, "setMarginEnd", R.dimen.zero);
                view.setInt(R.id.line2_container, "setMarginEnd", R.dimen.zero);
                view.setInt(R.id.line3_container, "setMarginEnd", R.dimen.zero);
            }
            view.setInt(context.getResources().getIdentifier("notification_main_column", "id", "android"), "setMarginEnd", endMargin);
        }
    };

    private static XC_MethodHook generateMediaActionButtonHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mBuilder"), "mContext");
            RemoteViews button = (RemoteViews) param.getResult();

            XposedHelpers.callMethod(button, "setDrawableParameters", context.getResources().getIdentifier("action0", "id", PACKAGE_ANDROID), false, -1,
                    XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mBuilder"), "resolveColor"),
                    PorterDuff.Mode.SRC_ATOP, -1);
        }
    };

    private static final XC_MethodHook resetStandardTemplateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews contentView = (RemoteViews) param.args[0];
            contentView.setImageViewResource(R.id.icon, 0);
            contentView.setTextViewText(R.id.app_name_text, null);
            contentView.setViewVisibility(R.id.chronometer, View.GONE);
            contentView.setViewVisibility(R.id.header_text, View.GONE);
            contentView.setTextViewText(R.id.header_text, null);
            contentView.setViewVisibility(R.id.header_text_divider, View.GONE);
            contentView.setViewVisibility(R.id.time_divider, View.GONE);
            contentView.setViewVisibility(R.id.time, View.GONE);
            contentView.setViewVisibility(R.id.text_line_1, View.GONE);
            contentView.setTextViewText(R.id.text_line_1, null);
        }
    };

    private static final XC_MethodHook processSmallIconAsLargeHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
            boolean legacy = false;
            try {
                legacy = ((boolean) XposedHelpers.callMethod(methodHookParam.thisObject, "isLegacy"));
            } catch (Throwable ignore) {
            }
            if (!legacy) {
                RemoteViews contentView = (RemoteViews) methodHookParam.args[1];
                int mColor = (int) XposedHelpers.callMethod(methodHookParam.thisObject, "resolveColor");
                XposedHelpers.callMethod(contentView, "setDrawableParameters",
                        android.R.id.icon,
                        false,
                        -1,
                        mColor,
                        PorterDuff.Mode.SRC_ATOP,
                        -1);
            }
            return null;
        }
    };

    private static final XC_MethodHook applyStandardTemplateWithActionsHook = new XC_MethodHook() {
        @SuppressWarnings("unchecked")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            RemoteViews big = (RemoteViews) param.getResult();
            big.setViewVisibility(context.getResources().getIdentifier("action_divider", "id", PACKAGE_ANDROID), View.GONE);

            ArrayList<Notification.Action> mActions = (ArrayList<Notification.Action>) XposedHelpers.getObjectField(param.thisObject, "mActions");
            big.setViewVisibility(R.id.actions_container, mActions.size() > 0 ? View.VISIBLE : View.GONE);
        }
    };

    private static final XC_MethodHook generateActionButtonHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            int mColor = (int) XposedHelpers.callMethod(param.thisObject, "resolveColor");
            int textViewId = context.getResources().getIdentifier("action0", "id", PACKAGE_ANDROID);
            Notification.Action action = (Notification.Action) param.args[0];

            RemoteViews button = (RemoteViews) param.getResult();
            if (action.title != null && action.title.length() != 0) {
                button.setTextViewCompoundDrawablesRelative(textViewId, 0, 0, 0, 0);
                button.setTextColor(textViewId, mColor);
            } else {
                XposedHelpers.callMethod(button, "setTextViewCompoundDrawablesRelativeColorFilter", textViewId, 0, mColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
    };

    private static final XC_MethodReplacement resolveColorHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mColor = XposedHelpers.getIntField(param.thisObject, "mColor");
            if (mColor != 0) return mColor; // App specified color in notification builder
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            if (mAccentColor == 0) {
                //noinspection deprecation
                mAccentColor = context.getResources().getColor(context.getResources().getIdentifier("notification_icon_bg_color", "color", PACKAGE_ANDROID));
            }
            int c = mAccentColor;
            if (ConfigUtils.notifications().generate_notification_accent_color) {
                String packageName = context.getPackageName();
                if (mGeneratedColors.containsKey(packageName))
                    return mGeneratedColors.get(packageName);
                try {
                    Drawable appIcon = context.getPackageManager().getApplicationIcon(packageName);
                    c = tk.wasdennnoch.androidn_ify.utils.ColorUtils.generateColor(appIcon, mAccentColor);
                    mGeneratedColors.put(packageName, c);
                } catch (PackageManager.NameNotFoundException ignore) {
                }
            }
            return c;
        }
    };

    private static final XC_MethodHook buildHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            ConfigUtils.notifications().loadBlacklistedApps();
            String name = ((Context) getObjectField(param.thisObject, "mContext")).getPackageName();
            if (!RemoteInputHelper.DIRECT_REPLY_ENABLED || ConfigUtils.notifications().blacklistedApps.contains(name)) {
                return;
            }

            Notification.Builder b = (Notification.Builder) param.thisObject;
            @SuppressWarnings("unchecked") List<Notification.Action> actions = (List<Notification.Action>) getObjectField(b, "mActions");
            if (!actions.isEmpty() && haveRemoteInput(actions.toArray(new Notification.Action[actions.size()]))) {
                return;
            }
            final List<Notification.Action> wearableRemoteInputActions = new ArrayList<>();

            final String EXTRA_WEARABLE_EXTENSIONS = (String) XposedHelpers.getStaticObjectField(Notification.WearableExtender.class, "EXTRA_WEARABLE_EXTENSIONS");
            final String KEY_ACTIONS = (String) XposedHelpers.getStaticObjectField(Notification.WearableExtender.class, "KEY_ACTIONS");
            Bundle wearableBundle = b.getExtras().getBundle(EXTRA_WEARABLE_EXTENSIONS);
            if (wearableBundle != null) {
                ArrayList<Notification.Action> wearableActions = wearableBundle.getParcelableArrayList(KEY_ACTIONS);
                if (wearableActions != null) {
                    for (int i = 0; i < wearableActions.size(); i++) {
                        if (hasValidRemoteInput(wearableActions.get(i))) {
                            wearableRemoteInputActions.add(wearableActions.get(i));
                        }
                    }
                }
            }
            if (wearableRemoteInputActions.size() > 0) {
                actions.addAll(0, wearableRemoteInputActions);
                return;
            }

            try {
                RemoteInput carRemoteInput = null;
                PendingIntent carReplyPendingIntent = null;

                final String EXTRA_CAR_EXTENDER = (String) XposedHelpers.getStaticObjectField(Notification.CarExtender.class, "EXTRA_CAR_EXTENDER");
                final String EXTRA_CONVERSATION = (String) XposedHelpers.getStaticObjectField(Notification.CarExtender.class, "EXTRA_CONVERSATION");
                final String KEY_REMOTE_INPUT = (String) XposedHelpers.getStaticObjectField(Notification.CarExtender.UnreadConversation.class, "KEY_REMOTE_INPUT");
                final String KEY_ON_REPLY = (String) XposedHelpers.getStaticObjectField(Notification.CarExtender.UnreadConversation.class, "KEY_ON_REPLY");

                Bundle carBundle = b.getExtras().getBundle(EXTRA_CAR_EXTENDER);
                if (carBundle != null) {
                    Bundle unreadConversation = carBundle.getBundle(EXTRA_CONVERSATION);
                    if (unreadConversation != null) {
                        carRemoteInput = unreadConversation.getParcelable(KEY_REMOTE_INPUT);
                        carReplyPendingIntent = unreadConversation.getParcelable(KEY_ON_REPLY);
                    }
                }
                if (carRemoteInput != null && carReplyPendingIntent != null) {
                    //noinspection deprecation
                    actions.add(0, new Notification.Action.Builder(0, "Reply", carReplyPendingIntent).addRemoteInput(carRemoteInput).build());
                }
            } catch (NoClassDefFoundError e) {
                // Ignore
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error in buildHook (car extender)", t);
            }
        }
    };

    private static final XC_MethodHook calculateTopPaddingHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            param.args[1] = false;
        }
    };

    private static final XC_MethodHook initConstantsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.setBooleanField(param.thisObject, "mScaleDimmed", false);
        }
    };

    private static final XC_MethodHook updateWindowWidthHHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (RomUtils.isOneplusStock()) {
                return;
            }
            Dialog mDialog = (Dialog) XposedHelpers.getObjectField(param.thisObject, "mDialog");
            ViewGroup mDialogView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mDialogView");
            Context context = mDialogView.getContext();
            Window window = mDialog.getWindow();
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mDialogView.getLayoutParams();
            lp.setMargins(0, lp.topMargin, 0, lp.bottomMargin);
            mDialogView.setLayoutParams(lp);
            //noinspection deprecation
            mDialogView.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
            mDialogView.requestLayout(); // Required to apply the new margin
            assert window != null;
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.horizontalMargin = 0;
            window.setAttributes(wlp);
        }
    };
    private static final XC_MethodHook updateWidthHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Dialog mDialog = (Dialog) XposedHelpers.getObjectField(param.thisObject, "mDialog");
            Resources res = mDialog.getContext().getResources();
            Window window = mDialog.getWindow();
            assert window != null;
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.horizontalMargin = 0;
            window.setAttributes(wlp);
            ViewGroup mContentParent = (ViewGroup) XposedHelpers.getObjectField(window, "mContentParent");
            ViewGroup panel = (ViewGroup) mContentParent.findViewById(res.getIdentifier("visible_panel", "id", PACKAGE_SYSTEMUI));
            ViewGroup dialogView = (ViewGroup) panel.getParent();
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) dialogView.getLayoutParams();
            lp.setMargins(0, lp.topMargin, 0, lp.bottomMargin);
            //noinspection deprecation
            dialogView.setBackgroundColor(res.getColor(res.getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
            dialogView.requestLayout(); // Required to apply the new margin
        }
    };

    private static final XC_MethodHook dismissViewButtonConstructorHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (param.thisObject instanceof TextView) {
                TextView button = (TextView) param.thisObject; // It's a TextView on some ROMs

                Drawable mAnimatedDismissDrawable = (Drawable) XposedHelpers.getObjectField(param.thisObject, "mAnimatedDismissDrawable");
                mAnimatedDismissDrawable.setBounds(0, 0, 0, 0);
                Drawable mStaticDismissDrawable = (Drawable) XposedHelpers.getObjectField(param.thisObject, "mStaticDismissDrawable");
                mStaticDismissDrawable.setBounds(0, 0, 0, 0);
                button.setVisibility(View.VISIBLE);
            }
        }
    };

    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        try {
            ConfigUtils config = ConfigUtils.getInstance();

            mSensitiveFilter.hookRes(resparam);

            final XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
            XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);

            if (config.notifications.change_style) {
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_side_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notifications_top_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_padding", ConfigUtils.notifications().enable_notifications_background ? modRes.fwd(R.dimen.notification_divider_height) : zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_padding_dimmed", ConfigUtils.notifications().enable_notifications_background ? modRes.fwd(R.dimen.notification_divider_height) : zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_material_rounded_rect_radius", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "speed_bump_height", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_min_height", modRes.fwd(R.dimen.notification_min_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_mid_height", modRes.fwd(R.dimen.notification_mid_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_max_height", modRes.fwd(R.dimen.notification_max_height));

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "integer", "keyguard_max_notification_count", config.notifications.keyguard_max);

                /*try {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_children_divider_height", zero);
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_children_padding", zero);
                    //resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "z_distance_between_notifications", zero);
                } catch (Throwable ignore) {
                }*/

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "notification_public_default", notification_public_default);
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_no_notifications", status_bar_no_notifications);

                if (ConfigUtils.notifications().experimental) {
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_expanded", new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                            View view = liparam.view;
                            Context context = view.getContext();

                            int containerId = context.getResources().getIdentifier("notification_container_parent", "id", PACKAGE_SYSTEMUI);
                            int stackScrollerId = context.getResources().getIdentifier("notification_stack_scroller", "id", PACKAGE_SYSTEMUI);
                            ViewGroup container = (ViewGroup) view.findViewById(containerId);
                            ViewGroup stackScroller = (ViewGroup) container.findViewById(stackScrollerId);
                            container.removeView(stackScroller);
                            container.addView(stackScroller, 0);
                        }
                    });
                }

                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_notification_row", status_bar_notification_row);
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_notification_keyguard_overflow", status_bar_notification_row);
                try {
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_notification_row_media", status_bar_notification_row);
                } catch (Throwable ignore) {
                }

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg", new XResources.DrawableLoader() {
                    @Override
                    public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                        return getNotificationBackground(xResources);
                    }
                });
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg_dim", new XResources.DrawableLoader() {
                    @Override
                    public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                        return getNotificationBackgroundDimmed(xResources);
                    }
                });

            }

            if (config.notifications.dismiss_button) {
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_notification_dismiss_all", status_bar_notification_dismiss_all);
                try {
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "recents_dismiss_button", status_bar_notification_dismiss_all);
                } catch (Exception ignored) {
                }
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

    @SuppressWarnings("deprecation")
    private static RippleDrawable getNotificationBackground(XResources xRes) {
        mNotificationBgColor = xRes.getColor(xRes.getIdentifier("notification_material_background_color", "color", PACKAGE_SYSTEMUI));
        return new RippleDrawable(
                ColorStateList.valueOf(xRes.getColor(xRes.getIdentifier("notification_ripple_untinted_color", "color", PACKAGE_SYSTEMUI))),
                getBackgroundRippleContent(mNotificationBgColor),
                null);
    }

    @SuppressWarnings("deprecation")
    private static RippleDrawable getNotificationBackgroundDimmed(XResources xRes) {
        return getNotificationBackground(xRes);
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    private static Drawable getBackgroundRippleContent(int color) {
        return new ColorDrawable(color);
    }

    @SuppressWarnings("unused")
    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.notifications().change_style) {

                Class classNotificationBuilder = Notification.Builder.class;
                Class classNotificationStyle = Notification.Style.class;
                Class classNotificationMediaStyle = Notification.MediaStyle.class;
                Class classRemoteViews = RemoteViews.class;

                if (ConfigUtils.M) {
                    XposedHelpers.findAndHookMethod(classNotificationBuilder, "processSmallIconAsLarge", Icon.class, classRemoteViews, processSmallIconAsLargeHook);
                } else {
                    XposedHelpers.findAndHookMethod(classNotificationBuilder, "processSmallIconAsLarge", int.class, classRemoteViews, processSmallIconAsLargeHook);
                }
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyLargeIconBackground", classRemoteViews, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyStandardTemplate", int.class, boolean.class, applyStandardTemplateHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyStandardTemplateWithActions", int.class, applyStandardTemplateWithActionsHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "resetStandardTemplate", RemoteViews.class, resetStandardTemplateHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "generateActionButton", Notification.Action.class, generateActionButtonHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "resolveColor", resolveColorHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "calculateTopPadding", Context.class, boolean.class, float.class, calculateTopPaddingHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "build", buildHook);
                XposedHelpers.findAndHookMethod(NotificationCompat.Builder.class, "build", buildHook);
                XposedHelpers.findAndHookMethod(classNotificationStyle, "getStandardView", int.class, getStandardViewHook);

                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "hideRightIcon", RemoteViews.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "styleText", RemoteViews.class, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "generateMediaActionButton", Notification.Action.class, generateMediaActionButtonHook);
                XposedHelpers.findAndHookMethod(classNotificationMediaStyle, "makeMediaContentView", makeMediaContentViewHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking app", t);
        }
    }

    public static void hookSystemUI(ClassLoader classLoader) {
        try {
            ConfigUtils config = ConfigUtils.getInstance();

            mSensitiveFilter.hook(classLoader);

            if (config.notifications.change_style) {

                StatusBarHooks.create(classLoader);

                if (ConfigUtils.M) // For now
                    mStackScrollLayoutHooks = new NotificationStackScrollLayoutHooks(classLoader);

                Class classBaseStatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.BaseStatusBar", classLoader);
                Class classEntry = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationData.Entry", classLoader);
                Class classStackScrollAlgorithm = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackScrollAlgorithm", classLoader);
                Class classNotificationGuts = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationGuts", classLoader);
                final Class<?> classExpandableNotificationRow = XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableNotificationRow", classLoader);
                final Class<?> classMediaExpandableNotificationRow = getClassMediaExpandableNotificationRow(classLoader);
                Class classPhoneStatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader);
                if (RemoteInputHelper.DIRECT_REPLY_ENABLED) {
                    Class classStatusBarWindowManager = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.StatusBarWindowManager", classLoader);
                    Class classStatusBarWindowManagerState = XposedHelpers.findClass(PACKAGE_SYSTEMUI + ".statusbar.phone.StatusBarWindowManager.State", classLoader);

                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "addStatusBarWindow", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            statusBarWindowManager = XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindowManager");
                        }
                    });

                    XposedHelpers.findAndHookMethod(classStatusBarWindowManager, "applyFocusableFlag", classStatusBarWindowManagerState, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams)
                                    XposedHelpers.getObjectField(param.thisObject, ConfigUtils.L1 ? "mLpChanged" : "mLp");
                            if (remoteInputActive) {
                                windowParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                                windowParams.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
                                param.setResult(null);
                            }
                            windowParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                        }
                    });
                }

                XposedHelpers.findAndHookMethod(classBaseStatusBar, "inflateViews", classEntry, ViewGroup.class, inflateViewsHook);
                XposedHelpers.findAndHookMethod(classStackScrollAlgorithm, "initConstants", Context.class, initConstantsHook);
                XposedHelpers.findAndHookMethod(classNotificationGuts, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Drawable bg = (Drawable) XposedHelpers.getObjectField(param.thisObject, "mBackground");
                        if (bg instanceof ShapeDrawable) {
                            ((ShapeDrawable) bg).getPaint().setPathEffect(null);
                        } else if (bg instanceof GradientDrawable) {
                            ((GradientDrawable) bg).setCornerRadius(0);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(classExpandableNotificationRow, "setIconAnimationRunningForChild", boolean.class, View.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (classMediaExpandableNotificationRow != null && classMediaExpandableNotificationRow.isAssignableFrom(param.thisObject.getClass()))
                            return null;
                        boolean running = (boolean) param.args[0];
                        View child = (View) param.args[1];
                        if (child != null) {
                            ImageView icon = (ImageView) child.findViewById(R.id.icon);
                            if (icon == null)
                                icon = (ImageView) child.findViewById(com.android.internal.R.id.icon);
                            if (icon != null)
                                setIconRunning(param.thisObject, icon, running);
                            ImageView rightIcon = (ImageView) child.findViewById(
                                    com.android.internal.R.id.right_icon);
                            if (rightIcon != null)
                                setIconRunning(param.thisObject, rightIcon, running);
                        }
                        return null;
                    }

                    private void setIconRunning(Object row, ImageView icon, boolean running) {
                        XposedHelpers.callMethod(row, "setIconRunning", icon, running);
                    }
                });

                if (ConfigUtils.M && !ConfigUtils.notifications().enable_notifications_background) {
                    XposedHelpers.findAndHookMethod(classExpandableNotificationRow, "setHeadsUp", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean isHeadsUp = (boolean) param.args[0];
                            FrameLayout row = (FrameLayout) param.thisObject;
                            row.findViewById(R.id.notification_divider).setAlpha(isHeadsUp ? 0 : 1);
                        }
                    });
                }

                XposedHelpers.findAndHookMethod(classPhoneStatusBar, "start", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        mPhoneStatusBar = param.thisObject;
                    }
                });

                XposedHelpers.findAndHookMethod(classPhoneStatusBar, "makeStatusBarView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mNavigationBarView = XposedHelpers.getObjectField(NotificationHooks.mPhoneStatusBar, "mNavigationBarView");
                        if (mNavigationBarView == null) {
                            QSCustomizer qsCustomizer = NotificationPanelHooks.getQsCustomizer();
                            if (qsCustomizer != null)
                                qsCustomizer.setHasNavBar(false);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(classPhoneStatusBar, "onBackPressed", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        QSCustomizer qsCustomizer = NotificationPanelHooks.getQsCustomizer();
                        if (qsCustomizer != null && qsCustomizer.onBackPressed()) {
                            param.setResult(true);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(classPhoneStatusBar, "animateCollapsePanels", int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        QSCustomizer qsCustomizer = NotificationPanelHooks.getQsCustomizer();
                        if (qsCustomizer != null && qsCustomizer.isCustomizing()) {
                            qsCustomizer.hide(true);
                        }
                    }
                });

                if (!ConfigUtils.notifications().enable_notifications_background) {
                    XposedHelpers.findAndHookMethod(classPhoneStatusBar, "updateNotificationShade", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            ViewGroup stack = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mStackScroller");
                            int childCount = stack.getChildCount();
                            boolean firstChild = true;
                            for (int i = 0; i < childCount; i++) {
                                View child = stack.getChildAt(i);
                                if (!classExpandableNotificationRow.isAssignableFrom(child.getClass())) {
                                    continue;
                                }
                                child.findViewById(R.id.notification_divider).setVisibility(firstChild ? View.INVISIBLE : View.VISIBLE);
                                firstChild = false;
                            }
                        }
                    });
                }

                XposedHelpers.findAndHookMethod(classExpandableNotificationRow, "setExpandable", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object row = param.thisObject;
                        boolean expandable = (boolean) param.args[0];
                        Object listener = XposedHelpers.getAdditionalInstanceField(row, KEY_EXPAND_CLICK_LISTENER);
                        View.OnClickListener onClickListener = null;
                        if (listener != null && listener instanceof View.OnClickListener) {
                            onClickListener = (View.OnClickListener) listener;
                        }
                        updateExpandButtons(XposedHelpers.getObjectField(row, "mPublicLayout"), expandable, onClickListener);
                        updateExpandButtons(XposedHelpers.getObjectField(row, "mPrivateLayout"), expandable, onClickListener);
                    }
                });

                XC_MethodHook expandedHook = new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object row = param.thisObject;
                        boolean userExpanded = (boolean) param.args[0];
                        updateChildrenExpanded(XposedHelpers.getObjectField(row, "mPublicLayout"), userExpanded);
                        updateChildrenExpanded(XposedHelpers.getObjectField(row, "mPrivateLayout"), userExpanded);
                    }
                };
                XposedHelpers.findAndHookMethod(classExpandableNotificationRow, "setUserExpanded", boolean.class, expandedHook);
                XposedHelpers.findAndHookMethod(classExpandableNotificationRow, "setSystemExpanded", boolean.class, expandedHook);

                XposedHelpers.findAndHookMethod(classExpandableNotificationRow, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        final Object row = param.thisObject;
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, KEY_EXPAND_CLICK_LISTENER, new SafeOnClickListener(TAG, "Error in notification expand icon click") {
                            @Override
                            public void onClickSafe(View v) {
                                boolean nowExpanded = !(boolean) XposedHelpers.callMethod(row, "isExpanded");
                                XposedHelpers.callMethod(row, "setUserExpanded", nowExpanded);
                                try {
                                    XposedHelpers.callMethod(row, "notifyHeightChanged", true);
                                } catch (Throwable t) {
                                    XposedHelpers.callMethod(row, "notifyHeightChanged");
                                }
                            }
                        });
                    }
                });

                if (ConfigUtils.M) {
                    Class classVolumeDialog = XposedHelpers.findClass("com.android.systemui.volume.VolumeDialog", classLoader);
                    XposedHelpers.findAndHookMethod(classVolumeDialog, "updateWindowWidthH", updateWindowWidthHHook);
                } else {
                    Class classVolumePanel = XposedHelpers.findClass("com.android.systemui.volume.VolumePanel", classLoader);
                    XposedHelpers.findAndHookMethod(classVolumePanel, "updateWidth", updateWidthHook);
                }

                XposedBridge.hookAllMethods(classBaseStatusBar, "applyColorsAndBackgrounds", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object entry = param.args[1];
                            View contentView = ConfigUtils.M ? (View) XposedHelpers.callMethod(entry, "getContentView") : (View) XposedHelpers.getObjectField(entry, "expanded");
                            if (contentView.getId() !=
                                    contentView.getContext().getResources().getIdentifier("status_bar_latest_event_content", "id", PACKAGE_ANDROID)) {
                                // Using custom RemoteViews
                                int targetSdk = XposedHelpers.getIntField(entry, "targetSdk");
                                if (targetSdk >= Build.VERSION_CODES.GINGERBREAD
                                        && targetSdk < Build.VERSION_CODES.LOLLIPOP) {
                                    XposedHelpers.callMethod(XposedHelpers.getObjectField(entry, "row"), "setShowingLegacyBackground", true);
                                    XposedHelpers.setBooleanField(entry, "legacy", true);
                                }
                            }

                            ImageView icon = (ImageView) XposedHelpers.getObjectField(entry, "icon");
                            if (icon != null) {
                                int targetSdk = XposedHelpers.getIntField(entry, "targetSdk");
                                if (Build.VERSION.SDK_INT > 22) {
                                    icon.setTag(icon.getResources().getIdentifier("icon_is_pre_L", "id", PACKAGE_SYSTEMUI), targetSdk < Build.VERSION_CODES.LOLLIPOP);
                                } else {
                                    if (targetSdk >= Build.VERSION_CODES.LOLLIPOP) {
                                        icon.setColorFilter(icon.getResources().getColor(android.R.color.white));
                                    } else {
                                        icon.setColorFilter(null);
                                    }
                                }
                            }
                            param.setResult(null);
                        } catch (Throwable t) {
                            XposedHook.logE(TAG, "Error in applyColorsAndBackgrounds", t);
                        }
                    }
                });

            }

            if (config.notifications.dismiss_button) {
                Class classDismissViewButton;
                try {
                    classDismissViewButton = XposedHelpers.findClass("com.android.systemui.statusbar.DismissViewButton", classLoader);
                } catch (Throwable t) {
                    classDismissViewButton = XposedHelpers.findClass("com.android.systemui.statusbar.DismissViewImageButton", classLoader);
                }
                XposedHelpers.findAndHookConstructor(classDismissViewButton, Context.class, AttributeSet.class, int.class, int.class, dismissViewButtonConstructorHook);
                XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.DismissView", classLoader, "setOnButtonClickListener", View.OnClickListener.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        ((View) XposedHelpers.callMethod(param.thisObject, "findContentView")).setOnClickListener((View.OnClickListener) param.args[0]);
                        return null;
                    }
                });
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    private static void updateChildrenExpanded(Object notificationContentView, boolean expanded) {
        View mExpandedChild = (View) XposedHelpers.getObjectField(notificationContentView, "mExpandedChild");
        View mContractedChild = (View) XposedHelpers.getObjectField(notificationContentView, "mContractedChild");
        View mHeadsUpChild = ConfigUtils.M ? (View) XposedHelpers.getObjectField(notificationContentView, "mHeadsUpChild") : null;
        if (mExpandedChild != null) {
            setExpanded(mExpandedChild, expanded);
        }
        if (mContractedChild != null) {
            setExpanded(mContractedChild, expanded);
        }
        if (mHeadsUpChild != null) {
            setExpanded(mHeadsUpChild, expanded);
        }
    }

    private static void setExpanded(View child, boolean expanded) {
        NotificationHeaderView header = (NotificationHeaderView) child.findViewById(R.id.notification_header);
        if (header != null) {
            header.setExpanded(expanded);
        }
    }

    private static void updateExpandButtons(Object notificationContentView, boolean expandable, View.OnClickListener onClickListener) {
        boolean mIsHeadsUp = false;
        View mExpandedChild = (View) XposedHelpers.getObjectField(notificationContentView, "mExpandedChild");
        View mContractedChild = (View) XposedHelpers.getObjectField(notificationContentView, "mContractedChild");
        View mHeadsUpChild = null;
        if (ConfigUtils.M) {
            mIsHeadsUp = XposedHelpers.getBooleanField(notificationContentView, "mIsHeadsUp");
            mHeadsUpChild = (View) XposedHelpers.getObjectField(notificationContentView, "mHeadsUpChild");
        }
        // if the expanded child has the same height as the collapsed one we hide it.
        if (mExpandedChild != null && mExpandedChild.getHeight() != 0) {
            if ((!mIsHeadsUp || mHeadsUpChild == null)) {
                if (mExpandedChild.getHeight() == mContractedChild.getHeight()) {
                    expandable = false;
                }
            } else if (mExpandedChild.getHeight() == mHeadsUpChild.getHeight()) {
                expandable = false;
            }
        }
        if (mExpandedChild != null) {
            updateExpandability(mExpandedChild, expandable, onClickListener);
        }
        if (mContractedChild != null) {
            updateExpandability(mContractedChild, expandable, onClickListener);
        }
        if (mHeadsUpChild != null) {
            updateExpandability(mHeadsUpChild, expandable, onClickListener);
        }
    }

    private static void updateExpandability(View target, boolean expandable, View.OnClickListener onClickListener) {
        ImageView expandButton = (ImageView) target.findViewById(R.id.expand_button);
        if (expandButton != null) {
            expandButton.setVisibility(expandable ? View.VISIBLE : View.GONE);
            NotificationHeaderView header = (NotificationHeaderView) target.findViewById(R.id.notification_header);
            header.setOnClickListener(expandable ? onClickListener : null);
        }
    }

    private static Class<?> getClassMediaExpandableNotificationRow(ClassLoader classLoader) {
        try {
            return XposedHelpers.findClass("com.android.systemui.statusbar.MediaExpandableNotificationRow", classLoader);
        } catch (Throwable t) {
            XposedHook.logD(TAG, "Class MediaExpandableNotificationRow not found. Skipping media row check.");
        }
        return null;
    }

    private static boolean haveRemoteInput(@NonNull Notification.Action[] actions) {
        for (Notification.Action a : actions) {
            if (a.getRemoteInputs() != null) {
                for (RemoteInput ri : a.getRemoteInputs()) {
                    if (ri.getAllowFreeFormInput()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasValidRemoteInput(Notification.Action action) {
        if ((TextUtils.isEmpty(action.title)) || (action.actionIntent == null)) {
            return false;
        }
        RemoteInput[] remoteInputs = action.getRemoteInputs();
        if (remoteInputs == null || remoteInputs.length == 0) {
            return false;
        }
        for (RemoteInput input : remoteInputs) {
            CharSequence[] choices = input.getChoices();
            if (input.getAllowFreeFormInput() || (choices != null && choices.length != 0)) {
                return true;
            }
        }
        return false;
    }

    public static void hookResAndroid(XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {
        try {

            final XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);

            if (ConfigUtils.notifications().change_style) {

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_action", notification_material_action);
                //resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_icon_group", notification_template_icon_group);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_base", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_media", notification_template_material_media);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_base", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_base", notification_template_material_big_base); // Extra treatment
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_media", notification_template_material_big_media);

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_picture", notification_template_material_big_picture);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_text", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_inbox", notification_template_material_base);

                resparam.res.setReplacement(PACKAGE_ANDROID, "dimen", "notification_title_text_size", modRes.fwd(R.dimen.notification_title_text_size));

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_media_action", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        ImageButton action = (ImageButton) liparam.view;
                        Context context = action.getContext();
                        ResourceUtils res = ResourceUtils.getInstance(context);
                        int width_height = res.getDimensionPixelSize(R.dimen.notification_media_action_width);
                        RelativeLayout.LayoutParams lParams = new RelativeLayout.LayoutParams(width_height, width_height);
                        lParams.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_media_action_margin));
                        int padding = ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.notification_media_action_padding);
                        action.setPadding(0, padding, 0, padding);
                        action.setBackground(res.getDrawable(R.drawable.notification_material_media_action_background));
                    }
                });

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_action_list", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout container = (LinearLayout) liparam.view;
                        Context context = container.getContext();
                        ResourceUtils res = ResourceUtils.getInstance(context);

                        int actionsId = container.getId();
                        container.setId(R.id.actions_container);
                        ViewUtils.setMarginBottom(container, 0);

                        NotificationActionListLayout notificationActionListLayout = new NotificationActionListLayout(context, null);
                        notificationActionListLayout.setId(actionsId);
                        notificationActionListLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, res.getDimensionPixelSize(R.dimen.notification_action_list_height)));
                        container.addView(notificationActionListLayout);
                    }
                });

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line1", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout layout = (LinearLayout) liparam.view;
                        while (layout.getChildCount() > 1) {
                            layout.removeViewAt(1);
                        }

                        Context context = layout.getContext();

                        TextView title = (TextView) layout.findViewById(context.getResources().getIdentifier("title", "id", PACKAGE_ANDROID));
                        LinearLayout.LayoutParams titleLp = (LinearLayout.LayoutParams) title.getLayoutParams();
                        titleLp.width = WRAP_CONTENT;
                        titleLp.weight = 0;

                        layout.removeView(title);

                        LinearLayout container = new RemoteMarginLinearLayout(context);
                        container.setOrientation(LinearLayout.HORIZONTAL);
                        container.setId(R.id.line1_container);

                        TextView textLine1 = new TextView(context);
                        textLine1.setId(R.id.text_line_1);
                        textLine1.setTextAppearance(context, android.R.style.TextAppearance_Material_Notification);
                        textLine1.setGravity(Gravity.START | Gravity.BOTTOM);
                        textLine1.setSingleLine(true);
                        textLine1.setEllipsize(TextUtils.TruncateAt.END);
                        textLine1.setHorizontalFadingEdgeEnabled(true);

                        LinearLayout.LayoutParams textLine1Lp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                        textLine1Lp.setMarginStart(ResourceUtils.getInstance(context).getDimensionPixelSize(R.dimen.notification_text_line1_margin_start));
                        textLine1.setLayoutParams(textLine1Lp);

                        container.addView(title);
                        container.addView(textLine1);

                        layout.addView(container);
                    }
                });

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line2", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        Context context = liparam.view.getContext();
                        ResourceUtils res = ResourceUtils.getInstance(context);

                        int notificationTextMarginEnd = res.getDimensionPixelSize(R.dimen.notification_text_margin_end);

                        ViewStub progress = (ViewStub) liparam.view.findViewById(context.getResources().getIdentifier("progress", "id", PACKAGE_ANDROID));
                        ViewUtils.setMarginEnd(progress, notificationTextMarginEnd);
                    }
                });

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line3", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout layout = (LinearLayout) liparam.view;

                        Context context = layout.getContext();

                        if (layout.getChildAt(1) != null) {
                            layout.removeViewAt(1);
                        }

                        LinearLayout container = new RemoteMarginLinearLayout(context);
                        container.setId(R.id.line3_container);

                        while (layout.getChildCount() > 0) {
                            View view = layout.getChildAt(0);
                            layout.removeView(view);
                            container.addView(view);
                        }

                        layout.addView(container);
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking framework resources", t);
        }
    }

    private static final XC_LayoutInflated notification_template_material_big_base = new XC_LayoutInflated() {

        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notificationTextMarginEnd = res.getDimensionPixelSize(R.dimen.notification_text_margin_end);

            TextView bigText = (TextView) layout.findViewById(context.getResources().getIdentifier("big_text", "id", PACKAGE_ANDROID));
            if (bigText != null) {
                ViewUtils.setMarginEnd(bigText, notificationTextMarginEnd);
            }
        }
    };

    private static final XC_LayoutInflated status_bar_no_notifications = new XC_LayoutInflated() {

        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int height = res.getDimensionPixelSize(R.dimen.notification_min_height);
            int paddingTop = res.getDimensionPixelSize(R.dimen.no_notifications_padding_top);
            int textSize = res.getDimensionPixelSize(R.dimen.no_notifications_text_size);

            TextView textView = (TextView) layout.findViewById(context.getResources().getIdentifier("no_notifications", "id", PACKAGE_SYSTEMUI));
            FrameLayout.LayoutParams textViewLp = (FrameLayout.LayoutParams) textView.getLayoutParams();
            textViewLp.height = height;

            int paddingLeft = textView.getPaddingLeft();
            int paddingRight = textView.getPaddingRight();
            int paddingBottom = textView.getPaddingBottom();

            textView.setLayoutParams(textViewLp);
            textView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    };

    private static final XC_LayoutInflated status_bar_notification_dismiss_all = new XC_LayoutInflated() {

        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int dismissButtonPadding = res.getDimensionPixelSize(R.dimen.notification_dismiss_button_padding);
            int dismissButtonPaddingTop = res.getDimensionPixelSize(R.dimen.notification_dismiss_button_padding_top);

            View buttonView = layout.getChildAt(0);
            if (buttonView instanceof LinearLayout) {
                ((LinearLayout) buttonView).getChildAt(0).setVisibility(View.GONE);
                ((LinearLayout) buttonView).getChildAt(1).setVisibility(View.VISIBLE);
                buttonView = ((LinearLayout) buttonView).getChildAt(1);
                buttonView.setId(context.getResources().getIdentifier("dismiss_text", "id", PACKAGE_SYSTEMUI));
            }
            if (buttonView instanceof ImageButton) {
                layout.removeView(buttonView);
                buttonView = new Button(context);
                buttonView.setId(context.getResources().getIdentifier("dismiss_text", "id", PACKAGE_SYSTEMUI));
                buttonView.setFocusable(true);
                buttonView.setContentDescription(context.getResources().getString(context.getResources().getIdentifier("accessibility_clear_all", "string", PACKAGE_SYSTEMUI)));
                layout.addView(buttonView);
            }
            TextView button = (TextView) buttonView; // It's a TextView on some ROMs
            if (button.getParent() instanceof LinearLayout) { // this is probably only for Xperia devices
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                lp.gravity = Gravity.END;
                button.setLayoutParams(lp);
                LinearLayout parent = (LinearLayout) button.getParent();
                parent.setBackground(null);
                LinearLayout.MarginLayoutParams lp2 = (LinearLayout.MarginLayoutParams) parent.getLayoutParams();
                lp2.setMarginEnd(0);
                parent.setLayoutParams(lp2);
            } else {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                lp.gravity = Gravity.END;
                button.setLayoutParams(lp);
            }
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            button.setTextColor(res.getColor(android.R.color.white));
            button.setAllCaps(true);
            button.setText(context.getString(context.getResources().getIdentifier("clear_all_notifications_text", "string", PACKAGE_SYSTEMUI)));
            button.setBackground(res.getDrawable(R.drawable.ripple_dismiss_all));
            button.setPadding(dismissButtonPadding, dismissButtonPaddingTop, dismissButtonPadding, dismissButtonPadding);
        }
    };

    private static final XC_LayoutInflated notification_template_material_base = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            ViewGroup layout = (ViewGroup) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            layout.removeViewAt(0);
            layout.addView(NotificationHeaderView.newHeader(context), 0);

            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);
            int notificationContentPaddingTop = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);
            int actionsMarginTop = res.getDimensionPixelSize(R.dimen.notification_actions_margin_top);

            LinearLayout notificationMain = (LinearLayout) layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", "android"));
            if (notificationMain == null) { // Some ROMs completely removed the ID
                notificationMain = (LinearLayout) layout.getChildAt(layout.getChildCount() - 1);
            }

            FrameLayout.LayoutParams notificationMainLParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            notificationMainLParams.setMargins(0, notificationContentPaddingTop, 0, 0);
            notificationMain.setLayoutParams(notificationMainLParams);

            ImageView rightIcon = getRightIcon(context);

            layout.addView(rightIcon);

            ViewGroup.LayoutParams params = layout.getLayoutParams();
            if (params == null)
                params = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.height = WRAP_CONTENT;
            layout.setLayoutParams(params);

            boolean isInboxLayout = liparam.resNames.fullName.contains("notification_template_material_inbox");
            boolean isBigTextLayout = liparam.resNames.fullName.contains("notification_template_material_big_text");

            // Margins for every child except actions container
            int actionsId = context.getResources().getIdentifier("actions", "id", PACKAGE_ANDROID);
            int childCount = notificationMain.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = notificationMain.getChildAt(i);
                int id = child.getId();
                if (id == R.id.actions_container) {
                    if (!isInboxLayout) {
                        ViewGroup.MarginLayoutParams childLp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                        childLp.topMargin += actionsMarginTop;
                        child.setLayoutParams(childLp);
                    }
                    if (ConfigUtils.notifications().custom_actions_color) {
                        child.findViewById(actionsId).setBackgroundColor(ConfigUtils.notifications().actions_color);
                    }
                } else {
                    ViewGroup.MarginLayoutParams childLp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                    childLp.leftMargin += notificationContentPadding;
                    childLp.rightMargin += notificationContentPadding;
                    child.setLayoutParams(childLp);
                }
            }

            if (isInboxLayout || isBigTextLayout) {
                int notificationTextMarginEnd = res.getDimensionPixelSize(R.dimen.notification_text_inbox_margin_end);
                if (isInboxLayout) {
                    View inboxText0 = notificationMain.findViewById(context.getResources().getIdentifier("inbox_text0", "id", PACKAGE_ANDROID));
                    ViewUtils.setMarginEnd(inboxText0, notificationTextMarginEnd);
                }
                if (isBigTextLayout) {
                    View bigText = notificationMain.findViewById(context.getResources().getIdentifier("big_text", "id", PACKAGE_ANDROID));
                    ViewUtils.setMarginEnd(bigText, notificationTextMarginEnd);
                }
                // Remove divider
                notificationMain.removeViewAt(notificationMain.getChildCount() - 2);
                // Remove bottom line
                notificationMain.removeViewAt(notificationMain.getChildCount() - 1);
            }
        }
    };

    private static final XC_LayoutInflated status_bar_notification_row = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            FrameLayout row = (FrameLayout) liparam.view;
            Context context = row.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            if (!ConfigUtils.notifications().enable_notifications_background) {

                int dividerHeight = res.getDimensionPixelSize(R.dimen.notification_separator_size);

                FrameLayout.LayoutParams dividerLp = new FrameLayout.LayoutParams(MATCH_PARENT, dividerHeight);
                dividerLp.gravity = Gravity.TOP;

                View divider = new View(context);
                divider.setBackgroundColor(0x1F000000);
                divider.setId(R.id.notification_divider);
                divider.setLayoutParams(dividerLp);

                row.addView(divider);
            }

            if (ConfigUtils.M) {
                FrameLayout.LayoutParams fakeShadowLp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);

                FakeShadowView fakeShadow = new FakeShadowView(context);
                fakeShadow.setId(R.id.fake_shadow);
                fakeShadow.setLayoutParams(fakeShadowLp);

                row.addView(fakeShadow);
            }
        }
    };

    private static final XC_LayoutInflated notification_material_action = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            Button button = (Button) liparam.view;

            ViewGroup.MarginLayoutParams buttonLp = (ViewGroup.MarginLayoutParams) button.getLayoutParams();
            buttonLp.height = MATCH_PARENT;
            buttonLp.width = WRAP_CONTENT;
            button.setLayoutParams(buttonLp);
        }
    };

    @NonNull
    private static ImageView getRightIcon(Context context) {
        ResourceUtils res = ResourceUtils.getInstance(context);

        int rightIconSize = res.getDimensionPixelSize(R.dimen.notification_right_icon_size);
        int rightIconMarginTop = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_top);
        int rightIconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_end);

        ImageView rightIcon = new ImageView(context);

        //noinspection SuspiciousNameCombination
        FrameLayout.LayoutParams rightIconLp = new FrameLayout.LayoutParams(rightIconSize, rightIconSize);
        rightIconLp.setMargins(0, rightIconMarginTop, 0, 0);
        rightIconLp.setMarginEnd(rightIconMarginEnd);
        rightIconLp.gravity = Gravity.TOP | Gravity.END;
        rightIcon.setLayoutParams(rightIconLp);
        rightIcon.setId(context.getResources().getIdentifier("right_icon", "id", "android"));

        return rightIcon;
    }

    @NonNull
    private static ImageView getLargeRightIcon(Context context) {
        ResourceUtils res = ResourceUtils.getInstance(context);

        int rightIconSize = res.getDimensionPixelSize(R.dimen.media_notification_expanded_image_max_size);
        int rightIconMarginBottom = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_bottom);
        int rightIconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_end);

        ImageView rightIcon = new ImageView(context);

        //noinspection SuspiciousNameCombination
        FrameLayout.LayoutParams rightIconLp = new FrameLayout.LayoutParams(rightIconSize, rightIconSize);
        rightIconLp.setMargins(0, 0, 0, rightIconMarginBottom);
        rightIconLp.setMarginEnd(rightIconMarginEnd);
        rightIconLp.gravity = Gravity.BOTTOM | Gravity.END;
        rightIcon.setLayoutParams(rightIconLp);
        rightIcon.setId(context.getResources().getIdentifier("right_icon", "id", "android"));

        return rightIcon;
    }

    private static final XC_LayoutInflated notification_template_material_big_media = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            RelativeLayout oldLayout = (RelativeLayout) liparam.view;
            Context context = oldLayout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            View mediaActions = oldLayout.findViewById(context.getResources().getIdentifier("media_actions", "id", PACKAGE_ANDROID));
            oldLayout.removeAllViews();

            FrameLayout layout = (FrameLayout) LayoutInflater.from(context).inflate(context.getResources().getIdentifier("notification_template_material_base", "layout", PACKAGE_ANDROID), null);
            layout.setId(R.id.dummy_id);
            oldLayout.addView(layout);

            LinearLayout notificationMain = (LinearLayout) layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", "android"));
            notificationMain.setMinimumHeight(res.getDimensionPixelSize(R.dimen.notification_min_content_height));

            ViewUtils.setMarginEnd(notificationMain.findViewById(context.getResources().getIdentifier("title", "id", PACKAGE_ANDROID)), 0);
            ViewUtils.setMarginEnd(notificationMain.findViewById(context.getResources().getIdentifier("text", "id", PACKAGE_ANDROID)), 0);

            LinearLayout contentContainer = new LinearLayout(context);
            contentContainer.setPadding(0, res.getDimensionPixelSize(R.dimen.notification_content_margin_top), 0, 0);
            contentContainer.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams contentContainerLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            contentContainerLp.gravity = Gravity.FILL_VERTICAL;
            contentContainerLp.weight = 1;
            contentContainer.setLayoutParams(contentContainerLp);

            LinearLayout.LayoutParams mediaActionsLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            mediaActionsLp.gravity = Gravity.TOP;
            mediaActionsLp.setMargins(0, 0, 0, res.getDimensionPixelSize(R.dimen.media_actions_margin_bottom));
            mediaActionsLp.setMarginStart(res.getDimensionPixelSize(R.dimen.big_media_actions_margin_start));
            mediaActions.setLayoutParams(mediaActionsLp);

            layout.removeView(layout.findViewById(context.getResources().getIdentifier("right_icon", "id", "android")));
            layout.removeView(notificationMain);

            ViewUtils.setMarginEnd(layout.findViewById(R.id.notification_header), res.getDimensionPixelSize(R.dimen.notification_content_plus_big_picture_margin_end));

            FrameLayout.LayoutParams notificationMainLp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            notificationMainLp.setMargins(0, 0, 0, res.getDimensionPixelSize(R.dimen.notification_content_margin_bottom));
            notificationMainLp.setMarginStart(res.getDimensionPixelSize(R.dimen.notification_content_margin_start));
            notificationMainLp.setMarginEnd(res.getDimensionPixelSize(R.dimen.notification_content_plus_picture_margin_end));
            notificationMain.setLayoutParams(notificationMainLp);

            contentContainer.addView(notificationMain);
            contentContainer.addView(mediaActions);

            layout.addView(contentContainer);
            layout.addView(getLargeRightIcon(context));
        }
    };

    private static final XC_LayoutInflated notification_template_material_media = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            LinearLayout oldLayout = (LinearLayout) liparam.view;
            Context context = oldLayout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            View mediaActions = oldLayout.findViewById(context.getResources().getIdentifier("media_actions", "id", PACKAGE_ANDROID));
            oldLayout.removeAllViews();

            FrameLayout layout = (FrameLayout) LayoutInflater.from(context).inflate(context.getResources().getIdentifier("notification_template_material_base", "layout", PACKAGE_ANDROID), null);
            layout.setId(R.id.dummy_id);
            oldLayout.addView(layout);

            LinearLayout notificationMain = (LinearLayout) layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", "android"));
            notificationMain.setOrientation(LinearLayout.HORIZONTAL);

            ViewUtils.setMarginEnd(notificationMain.findViewById(context.getResources().getIdentifier("title", "id", PACKAGE_ANDROID)), 0);
            ViewUtils.setMarginEnd(notificationMain.findViewById(context.getResources().getIdentifier("text", "id", PACKAGE_ANDROID)), 0);

            LinearLayout contentContainer = new LinearLayout(context);
            contentContainer.setOrientation(LinearLayout.VERTICAL);

            while (notificationMain.getChildCount() > 0) {
                View view = notificationMain.getChildAt(0);
                notificationMain.removeViewAt(0);
                contentContainer.addView(view);
            }

            layout.removeView(notificationMain);

            int notificationMainId = notificationMain.getId();
            notificationMain = new RemoteMarginLinearLayout(context);
            notificationMain.setId(notificationMainId);

            layout.addView(notificationMain, 1);

            FrameLayout.LayoutParams notificationMainLp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            notificationMainLp.setMargins(0, res.getDimensionPixelSize(R.dimen.notification_content_margin_top), 0, 0);
            notificationMain.setLayoutParams(notificationMainLp);

            LinearLayout.LayoutParams contentContainerLp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            contentContainerLp.gravity = Gravity.FILL_VERTICAL;
            contentContainerLp.weight = 1;
            contentContainer.setLayoutParams(contentContainerLp);

            contentContainer.setMinimumHeight(res.getDimensionPixelSize(R.dimen.notification_min_content_height));
            contentContainer.setPadding(0, 0, 0, 0);

            int mediaMargin = res.getDimensionPixelSize(R.dimen.media_actions_margin_bottom);
            LinearLayout.LayoutParams mediaActionsLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            mediaActionsLp.gravity = Gravity.BOTTOM | Gravity.END;
            mediaActionsLp.setMargins(0, mediaMargin, 0, mediaMargin);
            mediaActionsLp.setMarginStart(res.getDimensionPixelSize(R.dimen.media_actions_margin_start));
            mediaActions.setLayoutParams(mediaActionsLp);

            notificationMain.addView(contentContainer);
            notificationMain.addView(mediaActions);
        }
    };

    private static final XC_LayoutInflated notification_template_material_big_picture = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;
            View pic = layout.getChildAt(0);
            View shadow = layout.getChildAt(1);
            View base = layout.getChildAt(2);
            View actions = layout.getChildAt(3);

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notificationHeight = res.getDimensionPixelSize(R.dimen.notification_min_height);
            int notificationActionsPadding = res.getDimensionPixelSize(R.dimen.notification_actions_margin_start);

            FrameLayout.LayoutParams picLParams = (FrameLayout.LayoutParams) pic.getLayoutParams();
            FrameLayout.LayoutParams shadowLParams = (FrameLayout.LayoutParams) shadow.getLayoutParams();
            FrameLayout.LayoutParams baseLParams = (FrameLayout.LayoutParams) base.getLayoutParams();

            picLParams.setMargins(0, notificationHeight, 0, 0);
            shadowLParams.setMargins(0, notificationHeight, 0, 0);
            baseLParams.height = notificationHeight;

            pic.setLayoutParams(picLParams);
            shadow.setLayoutParams(shadowLParams);
            base.setLayoutParams(baseLParams);
            actions.setPadding(notificationActionsPadding, 0, notificationActionsPadding, 0);
        }
    };

    @SuppressWarnings("deprecation")
    private static final XC_LayoutInflated notification_public_default = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            RelativeLayout layout = (RelativeLayout) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);
            int notificationContentMarginTop = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);
            int notificationHeaderMarginTop = res.getDimensionPixelSize(R.dimen.notification_fake_header_margin_top);
            int iconSize = res.getDimensionPixelSize(R.dimen.notification_icon_size);
            int iconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_icon_margin_end);
            int appNameMarginStart = res.getDimensionPixelSize(R.dimen.notification_app_name_margin_start);
            int appNameMarginEnd = res.getDimensionPixelSize(R.dimen.notification_app_name_margin_end);
            int dividerMarginTop = res.getDimensionPixelSize(R.dimen.notification_divider_margin_top);
            int timeMarginStart = res.getDimensionPixelSize(R.dimen.notification_time_margin_start);

            int iconId = context.getResources().getIdentifier("icon", "id", PACKAGE_SYSTEMUI);
            int timeId = context.getResources().getIdentifier("time", "id", PACKAGE_SYSTEMUI);
            int titleId = context.getResources().getIdentifier("title", "id", PACKAGE_SYSTEMUI);

            ImageView icon = (ImageView) layout.findViewById(iconId);
            DateTimeView time = (DateTimeView) layout.findViewById(timeId);
            TextView title = (TextView) layout.findViewById(titleId);
            TextView textView = new TextView(context);
            TextView divider = new TextView(context);

            RelativeLayout.LayoutParams iconLParams = new RelativeLayout.LayoutParams(iconSize, iconSize);
            iconLParams.setMargins(notificationContentPadding, notificationHeaderMarginTop, iconMarginEnd, 0);

            RelativeLayout.LayoutParams timeLParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            timeLParams.setMargins(timeMarginStart, 0, 0, 0);
            timeLParams.addRule(RelativeLayout.RIGHT_OF, R.id.public_app_name_text);
            timeLParams.addRule(RelativeLayout.ALIGN_TOP, iconId);

            RelativeLayout.LayoutParams titleLParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            titleLParams.setMargins(notificationContentPadding, notificationContentMarginTop, 0, 0);
            //titleLParams.addRule(RelativeLayout.BELOW, iconId);

            RelativeLayout.LayoutParams textViewLParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            textViewLParams.setMarginStart(appNameMarginStart);
            textViewLParams.setMarginEnd(appNameMarginEnd);
            textViewLParams.addRule(RelativeLayout.ALIGN_TOP, iconId);
            textViewLParams.addRule(RelativeLayout.RIGHT_OF, iconId);

            RelativeLayout.LayoutParams dividerLParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            dividerLParams.setMargins(0, dividerMarginTop, 0, 0);
            dividerLParams.addRule(RelativeLayout.RIGHT_OF, R.id.public_app_name_text);
            dividerLParams.addRule(RelativeLayout.ALIGN_TOP, iconId);

            time.setGravity(View.TEXT_ALIGNMENT_CENTER);

            textView.setId(R.id.public_app_name_text);
            textView.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));

            divider.setId(R.id.public_time_divider);
            divider.setLayoutParams(dividerLParams);
            divider.setText(res.getString(R.string.notification_header_divider_symbol));
            divider.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));
            divider.setVisibility(View.GONE);

            icon.setLayoutParams(iconLParams);
            time.setLayoutParams(timeLParams);
            title.setLayoutParams(titleLParams);
            textView.setLayoutParams(textViewLParams);

            layout.addView(textView);
            layout.addView(divider);
        }
    };
}
