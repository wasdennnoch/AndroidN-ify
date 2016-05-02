package tk.wasdennnoch.androidn_ify.notifications;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DateTimeView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class NotificationsHooks {

    private static final String PACKAGE_ANDROID = XposedHook.PACKAGE_ANDROID;
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

    private static final String TAG = "NotificationsHooks";

    private static XC_MethodHook inflateViews = new XC_MethodHook() {

        @SuppressWarnings("deprecation")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object entry = param.args[0];
            Object row = XposedHelpers.getObjectField(entry, "row");
            Object contentContainer = XposedHelpers.callMethod(row, "getPrivateLayout");
            Object contentContainerPublic = XposedHelpers.callMethod(row, "getPublicLayout");

            View privateView = (View) XposedHelpers.callMethod(contentContainer, "getContractedChild");
            View publicView = (View) XposedHelpers.callMethod(contentContainerPublic, "getContractedChild");

            Context context = publicView.getContext();

            TextView privateTextView = (TextView) privateView.findViewById(R.id.app_name_text);
            int color = privateTextView.getTextColors().getDefaultColor();

            ImageView icon = (ImageView) publicView.findViewById(context.getResources().getIdentifier("icon", "id", PACKAGE_SYSTEMUI));
            icon.setBackgroundResource(0);
            icon.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            icon.setPadding(0, 0, 0, 0);
            icon.setColorFilter(color);

            TextView textView = (TextView) publicView.findViewById(R.id.public_app_name_text);
            textView.setTextColor(privateTextView.getTextColors());
            textView.setText(privateTextView.getText());

            View time = publicView.findViewById(context.getResources().getIdentifier("time", "id", PACKAGE_SYSTEMUI));
            if (time != null) {
                publicView.findViewById(R.id.public_time_divider).setVisibility(time.getVisibility());
            }
        }
    };

    private static XC_MethodHook applyStandardTemplate = new XC_MethodHook() {

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            Resources res = context.getResources();
            RemoteViews contentView = (RemoteViews) param.getResult();
            int mColor = (int) XposedHelpers.callMethod(param.thisObject, "resolveColor");
            contentView.setInt(R.id.notification_icon, "setColorFilter", mColor);
            contentView.setTextViewText(R.id.app_name_text, context.getString(context.getApplicationInfo().labelRes));
            contentView.setTextColor(R.id.app_name_text, mColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Icon mSmallIcon = (Icon) XposedHelpers.getObjectField(param.thisObject, "mSmallIcon");
                contentView.setImageViewIcon(R.id.notification_icon, mSmallIcon);
                Icon mLargeIcon = (Icon) XposedHelpers.getObjectField(param.thisObject, "mLargeIcon");
                if (mLargeIcon != null) {
                    contentView.setImageViewIcon(res.getIdentifier("right_icon", "id", "android"), mLargeIcon);
                }
            } else {
                int mSmallIcon = XposedHelpers.getIntField(param.thisObject, "mSmallIcon");
                contentView.setImageViewResource(R.id.notification_icon, mSmallIcon);
                int mLargeIcon = XposedHelpers.getIntField(param.thisObject, "mLargeIcon");
                if (mLargeIcon != 0) {
                    contentView.setImageViewResource(res.getIdentifier("right_icon", "id", "android"), mLargeIcon);
                }
            }
            if ((boolean) XposedHelpers.callMethod(param.thisObject, "showsTimeOrChronometer")) {
                contentView.setViewVisibility(R.id.time_divider, View.VISIBLE);
            }
        }
    };

    private static XC_MethodHook processSmallIconAsLarge = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
            if (!((boolean) XposedHelpers.callMethod(methodHookParam.thisObject, "isLegacy"))) {
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

    public static void hookResSystemui(XC_InitPackageResources.InitPackageResourcesParam resparam, XSharedPreferences prefs, String modulePath) {
        try {
            if (prefs.getBoolean("enable_notification_tweaks", true)) {

                XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
                XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);

                // Notifications
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_side_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notifications_top_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_material_rounded_rect_radius", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "speed_bump_height", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_min_height", modRes.fwd(R.dimen.notification_min_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_mid_height", modRes.fwd(R.dimen.notification_mid_height));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_max_height", modRes.fwd(R.dimen.notification_max_height));

                // Drawables
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_header_bg", modRes.fwd(R.drawable.replacement_notification_header_bg));
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_guts_bg", modRes.fwd(R.drawable.replacement_notification_guts_bg));

                // Layouts
                resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "notification_public_default", notification_public_default);

                if (prefs.getBoolean("notification_dark_theme", false)) {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg", modRes.fwd(R.drawable.replacement_notification_material_bg_dark));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg_dim", modRes.fwd(R.drawable.replacement_notification_material_bg_dim_dark));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "notification_material_background_low_priority_color", modRes.fwd(R.color.notification_material_background_low_priority_color_dark));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "notification_material_background_media_default_color", modRes.fwd(R.color.notification_material_background_media_default_color_dark));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "color", "notification_ripple_color_low_priority", modRes.fwd(R.color.notification_ripple_color_low_priority_dark));
                } else {
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg", modRes.fwd(R.drawable.replacement_notification_material_bg));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg_dim", modRes.fwd(R.drawable.replacement_notification_material_bg_dim));
                }

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

    @SuppressWarnings("unused")
    public static void hook(ClassLoader classLoader, XSharedPreferences prefs) {
        Class classNotificationBuilder = Notification.Builder.class;
        Class classRemoteViews = RemoteViews.class;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            XposedHelpers.findAndHookMethod(classNotificationBuilder, "processSmallIconAsLarge", Icon.class, classRemoteViews, processSmallIconAsLarge);
        } else {
            XposedHelpers.findAndHookMethod(classNotificationBuilder, "processSmallIconAsLarge", int.class, classRemoteViews, processSmallIconAsLarge);
        }
        XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyLargeIconBackground", classRemoteViews, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyStandardTemplate", int.class, boolean.class, applyStandardTemplate);
    }

    public static void hookSystemUI(ClassLoader classLoader, XSharedPreferences prefs) {
        try {
            if (prefs.getBoolean("enable_notification_tweaks", true)) {
                Class classBaseStatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.BaseStatusBar", classLoader);
                Class classEntry = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationData.Entry", classLoader);
                XposedHelpers.findAndHookMethod(classBaseStatusBar, "inflateViews", classEntry, ViewGroup.class, inflateViews);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

    public static void hookResAndroid(XC_InitPackageResources.InitPackageResourcesParam resparam, XSharedPreferences prefs) {
        try {
            if (prefs.getBoolean("enable_notification_tweaks", true)) {

                //TODO More notification styling in the future

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_icon_group", notification_template_icon_group);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_base", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_base", notification_template_material_base);

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_picture", notification_template_material_big_picture);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_text", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_inbox", notification_template_material_base);

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line1", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout layout = (LinearLayout) liparam.view;
                        layout.removeViewAt(1);
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking framework resources", t);
        }
    }

    @SuppressWarnings("deprecation")
    private static XC_LayoutInflated notification_template_icon_group = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;
            layout.removeAllViews();

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);
            int notificationHeaderMarginTop = res.getDimensionPixelSize(R.dimen.notification_header_margin_top);
            int iconSize = res.getDimensionPixelSize(R.dimen.notification_icon_size);
            int iconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_icon_margin_end);
            int appNameMarginStart = res.getDimensionPixelSize(R.dimen.notification_app_name_margin_start);
            int appNameMarginEnd = res.getDimensionPixelSize(R.dimen.notification_app_name_margin_end);
            int dividerMarginTop = res.getDimensionPixelSize(R.dimen.notification_divider_margin_top);

            LinearLayout linearLayout = new LinearLayout(context);

            FrameLayout.LayoutParams linearLayoutLParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, iconSize);
            linearLayoutLParams.setMargins(0, notificationHeaderMarginTop, 0, notificationContentPadding);

            linearLayout.setLayoutParams(linearLayoutLParams);
            linearLayout.setPadding(notificationContentPadding, 0, notificationContentPadding, 0);

            ImageView fakeIcon = new ImageView(context);
            ImageView icon = new ImageView(context);
            TextView textView = new TextView(context);
            TextView divider = new TextView(context);

            View timeView = LayoutInflater.from(context).inflate(context.getResources().getIdentifier("notification_template_part_time", "layout", "android"), null);

            LinearLayout.LayoutParams fakeIconLParams = new LinearLayout.LayoutParams(0, 0);

            LinearLayout.LayoutParams iconLParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLParams.setMarginEnd(iconMarginEnd);

            LinearLayout.LayoutParams textViewLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textViewLParams.setMarginStart(appNameMarginStart);
            textViewLParams.setMarginEnd(appNameMarginEnd);

            LinearLayout.LayoutParams timeViewLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            LinearLayout.LayoutParams dividerLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dividerLParams.setMargins(0, dividerMarginTop, 0, 0);

            fakeIcon.setLayoutParams(fakeIconLParams);
            icon.setLayoutParams(iconLParams);
            textView.setLayoutParams(textViewLParams);
            timeView.setLayoutParams(timeViewLParams);
            divider.setLayoutParams(dividerLParams);

            fakeIcon.setId(android.R.id.icon);
            icon.setId(R.id.notification_icon);
            textView.setId(R.id.app_name_text);
            timeView.setId(context.getResources().getIdentifier("time", "id", "android"));
            divider.setId(R.id.time_divider);

            textView.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));

            timeView.setPadding(appNameMarginEnd, 0, 0, 0);

            divider.setText(res.getString(R.string.notification_header_divider_symbol));
            divider.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));
            divider.setVisibility(View.GONE);

            linearLayout.addView(fakeIcon);
            linearLayout.addView(icon);
            linearLayout.addView(textView);
            linearLayout.addView(divider);
            linearLayout.addView(timeView);

            layout.addView(linearLayout);
        }
    };

    private static XC_LayoutInflated notification_template_material_base = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int headerHeight = res.getDimensionPixelSize(R.dimen.notification_header_height);
            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);
            int notificationContentPaddingTop = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);
            int rightIconSize = res.getDimensionPixelSize(R.dimen.notification_right_icon_size);
            int rightIconMarginTop = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_top);
            int rightIconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_end);

            FrameLayout headerLayout = (FrameLayout) layout.getChildAt(0);
            LinearLayout notificationMain = (LinearLayout) layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", "android"));
            ImageView rightIcon = new ImageView(context);

            FrameLayout.LayoutParams headerLayoutLParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, headerHeight);
            headerLayout.setLayoutParams(headerLayoutLParams);

            FrameLayout.LayoutParams notificationMainLParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            notificationMainLParams.setMargins(notificationContentPadding, notificationContentPaddingTop, notificationContentPadding, 0);
            notificationMain.setLayoutParams(notificationMainLParams);

            //noinspection SuspiciousNameCombination
            FrameLayout.LayoutParams rightIconLParams = new FrameLayout.LayoutParams(rightIconSize, rightIconSize);
            rightIconLParams.setMargins(0, rightIconMarginTop, rightIconMarginEnd, 0);
            rightIconLParams.gravity = Gravity.TOP | Gravity.END;
            rightIcon.setLayoutParams(rightIconLParams);
            rightIcon.setId(context.getResources().getIdentifier("right_icon", "id", "android"));

            layout.addView(rightIcon);

            ViewGroup.LayoutParams params = layout.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layout.setLayoutParams(params);
        }
    };

    private static XC_LayoutInflated notification_template_material_big_picture = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            XposedHook.logI(TAG, "notification_template_material_big_picture");
            FrameLayout layout = (FrameLayout) liparam.view;
            View pic = layout.getChildAt(0);
            View shadow = layout.getChildAt(1);
            View base = layout.getChildAt(2);

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notificationHeight = res.getDimensionPixelSize(R.dimen.notification_min_height);

            FrameLayout.LayoutParams picLParams = (FrameLayout.LayoutParams) pic.getLayoutParams();
            FrameLayout.LayoutParams shadowLParams = (FrameLayout.LayoutParams) shadow.getLayoutParams();
            FrameLayout.LayoutParams baseLParams = (FrameLayout.LayoutParams) base.getLayoutParams();

            picLParams.setMargins(0, notificationHeight, 0, 0);
            shadowLParams.setMargins(0, notificationHeight, 0, 0);
            baseLParams.height = notificationHeight;

            pic.setLayoutParams(picLParams);
            shadow.setLayoutParams(shadowLParams);
            base.setLayoutParams(baseLParams);
        }
    };

    @SuppressWarnings("deprecation")
    private static XC_LayoutInflated notification_public_default = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            RelativeLayout layout = (RelativeLayout) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);
            int notificationHeaderMarginTop = res.getDimensionPixelSize(R.dimen.notification_header_margin_top);
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

            RelativeLayout.LayoutParams timeLParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            timeLParams.setMargins(timeMarginStart, 0, 0, 0);
            timeLParams.addRule(RelativeLayout.RIGHT_OF, R.id.public_app_name_text);
            timeLParams.addRule(RelativeLayout.ALIGN_TOP, iconId);

            RelativeLayout.LayoutParams titleLParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            titleLParams.setMargins(notificationContentPadding, 0, 0, 0);
            titleLParams.addRule(RelativeLayout.BELOW, iconId);

            RelativeLayout.LayoutParams textViewLParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textViewLParams.setMarginStart(appNameMarginStart);
            textViewLParams.setMarginEnd(appNameMarginEnd);
            textViewLParams.addRule(RelativeLayout.ALIGN_TOP, iconId);
            textViewLParams.addRule(RelativeLayout.RIGHT_OF, iconId);

            RelativeLayout.LayoutParams dividerLParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
