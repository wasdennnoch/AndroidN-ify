package tk.wasdennnoch.androidn_ify.notifications;

import android.app.Dialog;
import android.app.Notification;
import android.app.RemoteInput;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.LayoutUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class NotificationsHooks {

    private static final String TAG = "NotificationsHooks";

    private static final String PACKAGE_ANDROID = XposedHook.PACKAGE_ANDROID;
    private static final String PACKAGE_SYSTEMUI = XposedHook.PACKAGE_SYSTEMUI;

    private static boolean fullWidthVolume = false;

    private static XC_MethodHook inflateViewsHook = new XC_MethodHook() {

        @SuppressWarnings("deprecation")
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Object entry = param.args[0];
            Object row = XposedHelpers.getObjectField(entry, "row");
            Object contentContainer = XposedHelpers.getObjectField(row, "mPrivateLayout");
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
                icon.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                icon.setPadding(0, 0, 0, 0);
            }

            TextView privateAppName = (TextView) privateView.findViewById(R.id.app_name_text);
            if (privateAppName != null) {
                int color = privateAppName.getTextColors().getDefaultColor();
                if (appName != null) {
                    appName.setTextColor(privateAppName.getTextColors());
                    appName.setText(privateAppName.getText());
                }
                if (icon != null) {
                    icon.setColorFilter(color);
                }
            }

            // TODO automatically determine actions background

            // in row sind (NotificationBackgroundView) mBackgroundNormal / mBackgroundDimmed, darin jeweils mBackground

            /*int actionsId = context.getResources().getIdentifier("actions", "id", PACKAGE_ANDROID);
            View actionsPublic = publicView.findViewById(actionsId);
            View actionsPrivate = privateView.findViewById(actionsId);

            if (actionsPublic != null || actionsPrivate != null) {
                Drawable origNormalBg = (Drawable) XposedHelpers.getObjectField(XposedHelpers.getObjectField(row, "mBackgroundNormal"), "mBackground");
                Drawable origDimmedBg = (Drawable) XposedHelpers.getObjectField(XposedHelpers.getObjectField(row, "mBackgroundDimmed"), "mBackground");
                int origNormalBgColor = -1;
                int origDimmedBgColor = -1;
                if (origNormalBg instanceof LayerDrawable) {
                    origNormalBgColor = ((ColorDrawable) ((LayerDrawable) origNormalBg).findDrawableByLayerId(R.id.notification_background)).getColor();
                }
                if (origDimmedBg instanceof LayerDrawable) {
                    origDimmedBgColor = ((ColorDrawable) ((LayerDrawable) origDimmedBg).findDrawableByLayerId(R.id.notification_background)).getColor();
                }
                // TODO determine if dimmed
                if (actionsPublic != null) {
                    actionsPublic.setBackgroundColor(origNormalBgColor);
                }
                if (actionsPrivate != null) {
                    actionsPrivate.setBackgroundColor(origDimmedBgColor);
                }
            }*/

        }
    };

    private static XC_MethodHook getStandardViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews contentView = (RemoteViews) param.getResult();
            Notification.Builder mBuilder = (Notification.Builder) XposedHelpers.getObjectField(param.thisObject, "mBuilder");

            CharSequence overflowText = (CharSequence) (XposedHelpers.getBooleanField(param.thisObject, "mSummaryTextSet")
                    ? XposedHelpers.getObjectField(param.thisObject, "mSummaryText")
                    : XposedHelpers.getObjectField(mBuilder, "mSubText"));
            if (overflowText != null) {
                contentView.setTextViewText(R.id.notification_summary, (CharSequence) XposedHelpers.callMethod(mBuilder, "processLegacyText", overflowText));
                contentView.setViewVisibility(R.id.notification_summary_divider, View.VISIBLE);
                contentView.setViewVisibility(R.id.notification_summary, View.VISIBLE);
            }
        }
    };

    private static XC_MethodHook applyStandardTemplateHook = new XC_MethodHook() {

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            Resources res = context.getResources();
            RemoteViews contentView = (RemoteViews) param.getResult();
            int mColor = (int) XposedHelpers.callMethod(param.thisObject, "resolveColor");
            contentView.setInt(R.id.notification_icon, "setColorFilter", mColor);
            String appname = context.getPackageName();
            try {
                appname = context.getString(context.getApplicationInfo().labelRes);
            } catch (Throwable t) {
                try {
                    appname = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
                } catch (Throwable ignore) {
                }
            }
            contentView.setTextViewText(R.id.app_name_text, appname);
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
                Bitmap mLargeIcon = (Bitmap) XposedHelpers.getObjectField(param.thisObject, "mLargeIcon");
                if (mLargeIcon != null) {
                    contentView.setImageViewBitmap(res.getIdentifier("right_icon", "id", "android"), mLargeIcon);
                }
            }
            CharSequence mContentInfo = (CharSequence) XposedHelpers.getObjectField(param.thisObject, "mContentInfo");
            int mNumber = XposedHelpers.getIntField(param.thisObject, "mNumber");
            if (mContentInfo != null) {
                contentView.setViewVisibility(R.id.notification_info_divider, View.VISIBLE);
            } else if (mNumber > 0) {
                contentView.setViewVisibility(R.id.notification_info_divider, View.VISIBLE);
            } else {
                contentView.setViewVisibility(R.id.notification_info_divider, View.GONE);
            }
            if ((boolean) XposedHelpers.callMethod(param.thisObject, "showsTimeOrChronometer")) {
                contentView.setViewVisibility(R.id.time_divider, View.VISIBLE);
            }
            contentView.setInt(res.getIdentifier("right_icon", "id", "android"), "setBackgroundResource", 0);

            int resId = (int) param.args[0];
            if (resId == context.getResources().getIdentifier("notification_template_material_big_media", "layout", PACKAGE_ANDROID) ||
                    resId == context.getResources().getIdentifier("notification_template_material_media", "layout", PACKAGE_ANDROID)) {
                CharSequence mContentText = (CharSequence) XposedHelpers.getObjectField(param.thisObject, "mContentText");
                CharSequence mSubText = (CharSequence) XposedHelpers.getObjectField(param.thisObject, "mSubText");
                if (mContentText != null && mSubText != null) {
                    contentView.setTextViewText(context.getResources().getIdentifier("text", "id", PACKAGE_ANDROID),
                            (CharSequence) XposedHelpers.callMethod(param.thisObject, "processLegacyText", mContentText));
                    contentView.setViewVisibility(context.getResources().getIdentifier("text2", "id", PACKAGE_ANDROID), View.GONE);
                    contentView.setTextViewText(R.id.notification_summary, (CharSequence) XposedHelpers.callMethod(param.thisObject, "processLegacyText", mSubText));
                    contentView.setViewVisibility(R.id.notification_summary, View.VISIBLE);
                    contentView.setViewVisibility(R.id.notification_summary_divider, View.VISIBLE);
                    contentView.setTextColor(R.id.notification_summary, mColor);
                    contentView.setTextColor(R.id.notification_summary_divider, mColor);
                    XposedHelpers.callMethod(param.thisObject, "unshrinkLine3Text");
                }
            }
        }
    };

    private static XC_MethodHook resetStandardTemplateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            RemoteViews contentView = (RemoteViews) param.args[0];
            contentView.setViewVisibility(R.id.notification_summary_divider, View.GONE);
            contentView.setViewVisibility(R.id.notification_info_divider, View.GONE);
            contentView.setViewVisibility(R.id.time_divider, View.GONE);
            contentView.setTextViewText(R.id.notification_summary, null);
        }
    };

    private static XC_MethodHook processSmallIconAsLargeHook = new XC_MethodReplacement() {
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

    private static XC_MethodHook applyStandardTemplateWithActionsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

            RemoteViews big = (RemoteViews) param.getResult();
            big.setViewVisibility(context.getResources().getIdentifier("action_divider", "id", PACKAGE_ANDROID), View.GONE);
        }
    };

    private static XC_MethodHook generateActionButtonHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

            int mColor = (int) XposedHelpers.callMethod(param.thisObject, "resolveColor");
            int textViewId = context.getResources().getIdentifier("action0", "id", PACKAGE_ANDROID);

            RemoteViews button = (RemoteViews) param.getResult();
            button.setTextViewCompoundDrawablesRelative(textViewId, 0, 0, 0, 0);
            button.setTextColor(textViewId, mColor);
        }
    };

    private static XC_MethodReplacement resolveColorHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mColor = XposedHelpers.getIntField(param.thisObject, "mColor");
            if (mColor == 0)
                return ConfigUtils.notifications().appname_color;
            return mColor;
        }
    };

    private static XC_MethodHook initConstantsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.setBooleanField(param.thisObject, "mScaleDimmed", false);
        }
    };

    private static XC_MethodHook updateWindowWidthHHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            ViewGroup mDialogView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mDialogView");
            Context context = mDialogView.getContext();
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mDialogView.getLayoutParams();
            lp.setMargins(0, 0, 0, 0);
            if (fullWidthVolume) {
                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                lp.width = dm.widthPixels;
            }
            mDialogView.setLayoutParams(lp);
            //noinspection deprecation
            mDialogView.setBackgroundColor(context.getResources().getColor(context.getResources().getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
        }
    };
    private static XC_MethodHook updateWidthHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Dialog mDialog = (Dialog) XposedHelpers.getObjectField(param.thisObject, "mDialog");
            Resources res = mDialog.getContext().getResources();
            Window window = mDialog.getWindow();
            if (fullWidthVolume) {
                WindowManager.LayoutParams lp = window.getAttributes();
                DisplayMetrics dm = res.getDisplayMetrics();
                lp.width = dm.widthPixels;
                window.setAttributes(lp);
            }

            ViewGroup mContentParent = (ViewGroup) XposedHelpers.getObjectField(window, "mContentParent");
            ViewGroup panel = (ViewGroup) mContentParent.findViewById(res.getIdentifier("visible_panel", "id", PACKAGE_SYSTEMUI));
            ViewGroup dialogView = (ViewGroup) panel.getParent();
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) dialogView.getLayoutParams();
            params.leftMargin = params.rightMargin = 0;
            //noinspection deprecation
            dialogView.setBackgroundColor(res.getColor(res.getIdentifier("system_primary_color", "color", PACKAGE_SYSTEMUI)));
        }
    };

    private static XC_MethodHook dismissViewButtonConstructorHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (param.thisObject instanceof Button) {
                Button button = (Button) param.thisObject;

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
            if (config.notifications.enable) {

                final XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);
                XResources.DimensionReplacement zero = new XResources.DimensionReplacement(0, TypedValue.COMPLEX_UNIT_DIP);

                // Notifications
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_side_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notifications_top_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_padding", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_material_rounded_rect_radius", zero);
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "speed_bump_height", zero);

                if (config.notifications.change_style) {
                    // Notifications
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_min_height", modRes.fwd(R.dimen.notification_min_height));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_mid_height", modRes.fwd(R.dimen.notification_mid_height));
                    resparam.res.setReplacement(PACKAGE_SYSTEMUI, "dimen", "notification_max_height", modRes.fwd(R.dimen.notification_max_height));

                    // Layouts
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "notification_public_default", notification_public_default);
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_no_notifications", status_bar_no_notifications);
                }
                if (config.notifications.dismiss_button) {
                    resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "status_bar_notification_dismiss_all", status_bar_notification_dismiss_all);
                    try {
                        resparam.res.hookLayout(PACKAGE_SYSTEMUI, "layout", "recents_dismiss_button", status_bar_notification_dismiss_all);
                    } catch (Exception ignored) {
                    }
                }

                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg", new XResources.DrawableLoader() {
                    @Override
                    public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                        return getNotificationBackground(xResources, modRes);
                    }
                });
                resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "notification_material_bg_dim", new XResources.DrawableLoader() {
                    @Override
                    public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                        return getNotificationBackgroundDimmed(xResources, modRes);
                    }
                });

            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI resources", t);
        }
    }

    private static RippleDrawable getNotificationBackground(XResources xRes, XModuleResources modRes) {
        return new RippleDrawable(
                ColorStateList.valueOf(xRes.getColor(xRes.getIdentifier("notification_ripple_untinted_color", "color", PACKAGE_SYSTEMUI))),
                getBackgroundRippleContent(modRes, xRes.getColor(xRes.getIdentifier("notification_material_background_color", "color", PACKAGE_SYSTEMUI))),
                null);
    }

    private static RippleDrawable getNotificationBackgroundDimmed(XResources xRes, XModuleResources modRes) {
        return new RippleDrawable(
                ColorStateList.valueOf(xRes.getColor(android.R.color.transparent)),
                getBackgroundRippleContent(modRes, xRes.getColor(xRes.getIdentifier("notification_material_background_dimmed_color", "color", PACKAGE_SYSTEMUI))),
                null);
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    private static LayerDrawable getBackgroundRippleContent(XModuleResources modRes, int color) {
        LayerDrawable layerDrawable = (LayerDrawable) modRes.getDrawable(R.drawable.notification_background_template);
        Drawable bg = new ColorDrawable(color);
        layerDrawable.setDrawableByLayerId(R.id.notification_background, bg);
        return layerDrawable;
    }

    @SuppressWarnings("unused")
    public static void hook(ClassLoader classLoader) {
        try {
            Class classNotificationBuilder = Notification.Builder.class;
            Class classNotificationStyle = Notification.Style.class;
            Class classRemoteViews = RemoteViews.class;

            if (ConfigUtils.notifications().change_style) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    XposedHelpers.findAndHookMethod(classNotificationBuilder, "processSmallIconAsLarge", Icon.class, classRemoteViews, processSmallIconAsLargeHook);
                } else {
                    XposedHelpers.findAndHookMethod(classNotificationBuilder, "processSmallIconAsLarge", int.class, classRemoteViews, processSmallIconAsLargeHook);
                }
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyLargeIconBackground", classRemoteViews, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyStandardTemplate", int.class, boolean.class, applyStandardTemplateHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "applyStandardTemplateWithActions", int.class, applyStandardTemplateWithActionsHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "resetStandardTemplate", RemoteViews.class, resetStandardTemplateHook);
                XposedHelpers.findAndHookMethod(classNotificationBuilder, "generateActionButton", Notification.Action.class, generateActionButtonHook);
                if (ConfigUtils.notifications().custom_appname_color)
                    XposedHelpers.findAndHookMethod(classNotificationBuilder, "resolveColor", resolveColorHook);
                XposedHelpers.findAndHookMethod(classNotificationStyle, "getStandardView", int.class, getStandardViewHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking app", t);
        }
    }

    public static void hookSystemUI(ClassLoader classLoader) {
        try {
            ConfigUtils config = ConfigUtils.getInstance();
            if (config.notifications.enable) {
                Class classBaseStatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.BaseStatusBar", classLoader);
                Class classEntry = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationData.Entry", classLoader);
                Class classStackScrollAlgorithm = XposedHelpers.findClass("com.android.systemui.statusbar.stack.StackScrollAlgorithm", classLoader);
                Class classNotificationGuts = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationGuts", classLoader);

                if (config.notifications.change_style) {
                    XposedHelpers.findAndHookMethod(classBaseStatusBar, "inflateViews", classEntry, ViewGroup.class, inflateViewsHook);
                    XposedHelpers.findAndHookMethod(classStackScrollAlgorithm, "initConstants", Context.class, initConstantsHook);
                    XposedHelpers.findAndHookMethod(classNotificationGuts, "onFinishInflate", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Drawable bg = (Drawable) XposedHelpers.getObjectField(param.thisObject, "mBackground");
                            try {
                                ((ShapeDrawable) bg).getPaint().setPathEffect(null);
                            } catch (ClassCastException e) {
                                //noinspection ConstantConditions
                                ((GradientDrawable) bg).setCornerRadius(0);
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
                }

                fullWidthVolume = config.header.full_width_volume;

                if (Build.VERSION.SDK_INT >= 23) {
                    Class classVolumeDialog = XposedHelpers.findClass("com.android.systemui.volume.VolumeDialog", classLoader);
                    XposedHelpers.findAndHookMethod(classVolumeDialog, "updateWindowWidthH", updateWindowWidthHHook);
                } else {
                    Class classVolumePanel = XposedHelpers.findClass("com.android.systemui.volume.VolumePanel", classLoader);
                    XposedHelpers.findAndHookMethod(classVolumePanel, "updateWidth", updateWidthHook);
                }
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking SystemUI", t);
        }
    }

    private boolean hasValidRemoteInput(Notification.Action action) {
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

    public static void hookResAndroid(XC_InitPackageResources.InitPackageResourcesParam resparam) {
        try {
            if (ConfigUtils.notifications().change_style) {

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_material_action", notification_material_action);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_icon_group", notification_template_icon_group);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_base", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_media", notification_template_material_media);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_base", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_media", notification_template_material_big_media);

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_picture", notification_template_material_big_picture);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_text", notification_template_material_base);
                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_inbox", notification_template_material_base);

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line1", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout layout = (LinearLayout) liparam.view;
                        if (layout.getChildAt(1) != null) {
                            layout.removeViewAt(1);
                        }
                        if (layout.getChildAt(1) != null) {
                            layout.removeViewAt(1);
                        }

                        Context context = layout.getContext();
                        ResourceUtils res = ResourceUtils.getInstance(context);

                        int notificationTextMarginEnd = res.getDimensionPixelSize(R.dimen.notification_text_margin_end);

                        TextView title = (TextView) layout.findViewById(context.getResources().getIdentifier("title", "id", PACKAGE_ANDROID));
                        LinearLayout.LayoutParams titleLp = (LinearLayout.LayoutParams) title.getLayoutParams();
                        titleLp.rightMargin = notificationTextMarginEnd;
                        title.setLayoutParams(titleLp);
                    }
                });

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_part_line3", new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout layout = (LinearLayout) liparam.view;

                        Context context = layout.getContext();
                        ResourceUtils res = ResourceUtils.getInstance(context);

                        int notificationTextMarginEnd = res.getDimensionPixelSize(R.dimen.notification_text_margin_end);

                        TextView text = (TextView) layout.findViewById(context.getResources().getIdentifier("text", "id", PACKAGE_ANDROID));
                        LinearLayout.LayoutParams textLp = (LinearLayout.LayoutParams) text.getLayoutParams();
                        textLp.rightMargin = notificationTextMarginEnd;
                        text.setLayoutParams(textLp);

                        if (layout.getChildAt(1) != null) {
                            layout.removeViewAt(1);
                        }
                    }
                });

                resparam.res.hookLayout(PACKAGE_ANDROID, "layout", "notification_template_material_big_base", notification_template_material_big_base);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking framework resources", t);
        }
    }

    private static XC_LayoutInflated notification_template_material_big_base = new XC_LayoutInflated() {

        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int notificationTextMarginEnd = res.getDimensionPixelSize(R.dimen.notification_text_margin_end);

            TextView bigText = (TextView) layout.findViewById(context.getResources().getIdentifier("big_text", "id", PACKAGE_ANDROID));
            if (bigText != null) {
                LinearLayout.LayoutParams bigTextLp = (LinearLayout.LayoutParams) bigText.getLayoutParams();
                bigTextLp.rightMargin = notificationTextMarginEnd;
                bigText.setLayoutParams(bigTextLp);
            }
        }
    };

    private static XC_LayoutInflated status_bar_no_notifications = new XC_LayoutInflated() {

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

    private static XC_LayoutInflated status_bar_notification_dismiss_all = new XC_LayoutInflated() {

        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout layout = (FrameLayout) liparam.view;

            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int dismissButtonPadding = res.getDimensionPixelSize(R.dimen.notification_dismiss_button_padding);
            int dismissButtonPaddingTop = res.getDimensionPixelSize(R.dimen.notification_dismiss_button_padding_top);

            View buttonView = layout.getChildAt(0);
            if (buttonView instanceof ImageButton) {
                int oldId = buttonView.getId();
                layout.removeView(buttonView);
                buttonView = new Button(context);
                buttonView.setId(oldId);
                buttonView.setFocusable(true);
                buttonView.setContentDescription(context.getResources().getString(context.getResources().getIdentifier("accessibility_clear_all", "string", PACKAGE_SYSTEMUI)));
                layout.addView(buttonView);
            }
            Button button = (Button) buttonView;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.END;
            button.setLayoutParams(lp);
            button.setTextColor(res.getColor(android.R.color.white));
            button.setText(context.getString(context.getResources().getIdentifier("clear_all_notifications_text", "string", PACKAGE_SYSTEMUI)));
            button.setAllCaps(true);
            button.setBackground(res.getDrawable(R.drawable.ripple_dismiss_all));
            button.setPadding(dismissButtonPadding, dismissButtonPaddingTop, dismissButtonPadding, dismissButtonPadding);

            FrameLayout.LayoutParams buttonLp = (FrameLayout.LayoutParams) button.getLayoutParams();
            buttonLp.width = FrameLayout.LayoutParams.WRAP_CONTENT;
            button.setLayoutParams(buttonLp);
        }
    };

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

            String dividerText = res.getString(R.string.notification_header_divider_symbol);

            LinearLayout linearLayout = new LinearLayout(context);

            FrameLayout.LayoutParams linearLayoutLParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, iconSize);
            linearLayoutLParams.setMargins(0, notificationHeaderMarginTop, 0, notificationContentPadding);

            linearLayout.setLayoutParams(linearLayoutLParams);
            linearLayout.setPadding(notificationContentPadding, 0, notificationContentPadding, 0);

            ImageView fakeIcon = new ImageView(context);
            LinearLayout.LayoutParams fakeIconLParams = new LinearLayout.LayoutParams(0, 0);
            fakeIcon.setLayoutParams(fakeIconLParams);
            fakeIcon.setId(android.R.id.icon);

            ImageView icon = new ImageView(context);
            LinearLayout.LayoutParams iconLParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLParams.setMarginEnd(iconMarginEnd);
            icon.setLayoutParams(iconLParams);
            icon.setId(R.id.notification_icon);

            TextView textView = new TextView(context);
            LinearLayout.LayoutParams textViewLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textViewLParams.setMarginStart(appNameMarginStart);
            textViewLParams.setMarginEnd(appNameMarginEnd);
            textView.setLayoutParams(textViewLParams);
            textView.setId(R.id.app_name_text);
            textView.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));

            TextView summaryDivider = new TextView(context);
            LinearLayout.LayoutParams summaryDividerLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            summaryDividerLParams.setMargins(0, dividerMarginTop, 0, 0);
            summaryDivider.setLayoutParams(summaryDividerLParams);
            summaryDivider.setId(R.id.notification_summary_divider);
            summaryDivider.setText(dividerText);
            summaryDivider.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));
            summaryDivider.setVisibility(View.GONE);

            TextView summaryText = new TextView(context);
            LinearLayout.LayoutParams summaryTextLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            summaryTextLParams.setMarginStart(appNameMarginStart);
            summaryTextLParams.setMarginEnd(appNameMarginEnd);
            summaryText.setLayoutParams(summaryTextLParams);
            summaryText.setId(R.id.notification_summary);
            summaryText.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));
            summaryText.setVisibility(View.GONE);

            TextView infoDivider = new TextView(context);
            LinearLayout.LayoutParams infoDividerLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            infoDividerLParams.setMargins(0, dividerMarginTop, 0, 0);
            infoDivider.setLayoutParams(infoDividerLParams);
            infoDivider.setId(R.id.notification_info_divider);
            infoDivider.setText(dividerText);
            infoDivider.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));
            infoDivider.setVisibility(View.GONE);

            TextView infoText = new TextView(context);
            LinearLayout.LayoutParams infoTextLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            infoTextLParams.setMarginStart(appNameMarginStart);
            infoTextLParams.setMarginEnd(appNameMarginEnd);
            infoText.setLayoutParams(summaryTextLParams);
            infoText.setId(context.getResources().getIdentifier("info", "id", PACKAGE_ANDROID));
            infoText.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));
            infoText.setVisibility(View.GONE);

            TextView divider = new TextView(context);
            LinearLayout.LayoutParams dividerLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dividerLParams.setMargins(0, dividerMarginTop, 0, 0);
            divider.setLayoutParams(dividerLParams);
            divider.setId(R.id.time_divider);
            divider.setText(res.getString(R.string.notification_header_divider_symbol));
            divider.setTextAppearance(context, context.getResources().getIdentifier("TextAppearance.Material.Notification.Info", "style", "android"));
            divider.setVisibility(View.GONE);

            View timeView = LayoutInflater.from(context).inflate(context.getResources().getIdentifier("notification_template_part_time", "layout", "android"), null);
            LinearLayout.LayoutParams timeViewLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            timeView.setLayoutParams(timeViewLParams);
            timeView.setId(context.getResources().getIdentifier("time", "id", "android"));
            timeView.setPadding(appNameMarginEnd, 0, 0, 0);

            View chronometerView = LayoutInflater.from(context).inflate(context.getResources().getIdentifier("notification_template_part_chronometer", "layout", "android"), null);
            LinearLayout.LayoutParams chronometerViewLParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chronometerView.setLayoutParams(chronometerViewLParams);
            chronometerView.setId(context.getResources().getIdentifier("chronometer", "id", "android"));
            chronometerView.setPadding(appNameMarginEnd, 0, 0, 0);

            linearLayout.addView(fakeIcon);
            linearLayout.addView(icon);
            linearLayout.addView(textView);
            linearLayout.addView(summaryDivider);
            linearLayout.addView(summaryText);
            linearLayout.addView(infoDivider);
            linearLayout.addView(infoText);
            linearLayout.addView(divider);
            linearLayout.addView(timeView);
            linearLayout.addView(chronometerView);

            layout.addView(linearLayout);
        }
    };

    private static XC_LayoutInflated notification_template_material_base = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            XposedHook.logD(TAG, "notification_template_material_base inflate hook called for layout \"" + liparam.resNames.fullName + "\"");
            FrameLayout layout = (FrameLayout) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            int headerHeight = res.getDimensionPixelSize(R.dimen.notification_header_height);
            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);
            int notificationContentPaddingTop = res.getDimensionPixelSize(R.dimen.notification_content_margin_top);
            int rightIconSize = res.getDimensionPixelSize(R.dimen.notification_right_icon_size);
            int rightIconMarginTop = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_top);
            int rightIconMarginEnd = res.getDimensionPixelSize(R.dimen.notification_right_icon_margin_end);
            int actionsMarginTop = res.getDimensionPixelSize(R.dimen.notification_actions_margin_top);

            FrameLayout headerLayout = (FrameLayout) layout.getChildAt(0);
            LinearLayout notificationMain = (LinearLayout) layout.findViewById(context.getResources().getIdentifier("notification_main_column", "id", "android"));
            if (notificationMain == null) { // Some ROMs completely removed the ID
                notificationMain = (LinearLayout) layout.getChildAt(layout.getChildCount() - 1);
            }
            ImageView rightIcon = new ImageView(context);

            FrameLayout.LayoutParams headerLayoutLParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, headerHeight);
            headerLayout.setLayoutParams(headerLayoutLParams);

            FrameLayout.LayoutParams notificationMainLParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            notificationMainLParams.setMargins(0, notificationContentPaddingTop, 0, 0);
            notificationMain.setLayoutParams(notificationMainLParams);

            //noinspection SuspiciousNameCombination
            FrameLayout.LayoutParams rightIconLParams = new FrameLayout.LayoutParams(rightIconSize, rightIconSize);
            rightIconLParams.setMargins(0, rightIconMarginTop, rightIconMarginEnd, 0);
            rightIconLParams.gravity = Gravity.TOP | Gravity.END;
            rightIcon.setLayoutParams(rightIconLParams);
            rightIcon.setId(context.getResources().getIdentifier("right_icon", "id", "android"));

            layout.addView(rightIcon);

            ViewGroup.LayoutParams params = layout.getLayoutParams();
            if (params == null)
                params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layout.setLayoutParams(params);

            boolean isInboxLayout = liparam.resNames.fullName.contains("notification_template_material_inbox");
            boolean isBigTextLayout = liparam.resNames.fullName.contains("notification_template_material_big_text");

            // Margins for every child except actions container
            int actionsId = context.getResources().getIdentifier("actions", "id", PACKAGE_ANDROID);
            int childCount = notificationMain.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = notificationMain.getChildAt(i);
                if (child.getId() != actionsId) {
                    ViewGroup.MarginLayoutParams childLp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                    childLp.leftMargin += notificationContentPadding;
                    childLp.rightMargin += notificationContentPadding;
                    child.setLayoutParams(childLp);
                } else {
                    ViewGroup.MarginLayoutParams childLp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();

                    int separatorSize = res.getDimensionPixelSize(R.dimen.notification_separator_size);

                    childLp.bottomMargin += separatorSize;

                    if (!isInboxLayout) {
                        childLp.topMargin += actionsMarginTop;
                    }
                    if (!ConfigUtils.notifications().custom_actions_color) {
                        child.setBackgroundColor(res.getColor(R.color.notification_action_list));
                    } else {
                        child.setBackgroundColor(ConfigUtils.notifications().actions_color);
                    }
                    child.setLayoutParams(childLp);
                    child.setPadding(child.getPaddingLeft() + notificationContentPadding,
                            child.getPaddingTop(),
                            child.getPaddingRight() + notificationContentPadding,
                            child.getPaddingBottom());
                }
            }

            if (isInboxLayout || isBigTextLayout) {
                int notificationTextMarginEnd = res.getDimensionPixelSize(R.dimen.notification_text_inbox_margin_end);
                if (isInboxLayout) {
                    View inboxText0 = notificationMain.findViewById(context.getResources().getIdentifier("inbox_text0", "id", PACKAGE_ANDROID));
                    applyNotificationTextMarginEnd(inboxText0, notificationTextMarginEnd);
                }
                if (isBigTextLayout) {
                    View bigText = notificationMain.findViewById(context.getResources().getIdentifier("big_text", "id", PACKAGE_ANDROID));
                    applyNotificationTextMarginEnd(bigText, notificationTextMarginEnd);
                }
                // Remove divider
                notificationMain.removeViewAt(notificationMain.getChildCount() - 2);
                // Remove bottom line
                notificationMain.removeViewAt(notificationMain.getChildCount() - 1);
            }
        }
    };

    private static void applyNotificationTextMarginEnd(View text, int marginEnd) {
        LinearLayout.LayoutParams textLp = (LinearLayout.LayoutParams) text.getLayoutParams();
        textLp.rightMargin = marginEnd;
        text.setLayoutParams(textLp);
    }

    private static XC_LayoutInflated notification_material_action = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            Button button = (Button) liparam.view;

            LinearLayout.LayoutParams buttonLp = (LinearLayout.LayoutParams) button.getLayoutParams();
            buttonLp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            buttonLp.weight = 0;
            button.setLayoutParams(buttonLp);
        }
    };

    private static XC_LayoutInflated notification_template_material_big_media = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            RelativeLayout layout = (RelativeLayout) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            View headerLayout = layout.getChildAt(0);
            LinearLayout notificationMain = (LinearLayout) layout.getChildAt(1);

            int headerHeight = res.getDimensionPixelSize(R.dimen.notification_header_height);
            int headerHeightExcludeTop = res.getDimensionPixelSize(R.dimen.notification_header_height_exclude_top);
            int notificationHeight = res.getDimensionPixelSize(R.dimen.notification_min_height);
            int notificationMidHeight = res.getDimensionPixelSize(R.dimen.notification_mid_height);
            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);

            LayoutUtils.setHeight(layout, notificationMidHeight);

            ViewGroup.MarginLayoutParams headerLayoutLp = (ViewGroup.MarginLayoutParams) headerLayout.getLayoutParams();
            headerLayoutLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            headerLayoutLp.height = headerHeightExcludeTop;
            headerLayout.setLayoutParams(headerLayoutLp);

            RelativeLayout.LayoutParams notificationMainLp = (RelativeLayout.LayoutParams) notificationMain.getLayoutParams();
            notificationMainLp.topMargin = headerHeight;
            notificationMainLp.leftMargin = notificationContentPadding;
            notificationMain.setLayoutParams(notificationMainLp);
            notificationMain.setMinimumHeight(notificationHeight);
        }
    };

    private static XC_LayoutInflated notification_template_material_media = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
            LinearLayout layout = (LinearLayout) liparam.view;
            Context context = layout.getContext();
            ResourceUtils res = ResourceUtils.getInstance(context);

            layout.setOrientation(LinearLayout.VERTICAL);

            View headerLayout = layout.getChildAt(0);
            LinearLayout notificationMain = (LinearLayout) layout.getChildAt(1);
            LinearLayout mediaActions = (LinearLayout) layout.getChildAt(2);

            layout.removeViewAt(2);
            layout.removeViewAt(1);

            layout.setClipChildren(false);

            int headerHeight = res.getDimensionPixelSize(R.dimen.notification_header_height_exclude_top);
            int notificationContentPadding = res.getDimensionPixelSize(R.dimen.notification_content_margin_start);

            ViewGroup.MarginLayoutParams headerLayoutLp = (ViewGroup.MarginLayoutParams) headerLayout.getLayoutParams();
            headerLayoutLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            headerLayoutLp.height = headerHeight;
            headerLayout.setLayoutParams(headerLayoutLp);

            LinearLayout notificationLayout = new LinearLayout(context);
            LinearLayout.LayoutParams notificationLayoutLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            notificationLayoutLp.setMargins(notificationContentPadding, 0, notificationContentPadding, 0);
            notificationLayout.setLayoutParams(notificationLayoutLp);
            notificationLayout.setOrientation(LinearLayout.HORIZONTAL);

            layout.addView(notificationLayout);

            notificationLayout.addView(notificationMain);
            notificationLayout.addView(mediaActions);
        }
    };

    private static XC_LayoutInflated notification_template_material_big_picture = new XC_LayoutInflated() {
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