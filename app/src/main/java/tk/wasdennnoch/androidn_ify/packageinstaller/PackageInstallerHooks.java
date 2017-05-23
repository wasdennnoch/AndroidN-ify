package tk.wasdennnoch.androidn_ify.packageinstaller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.content.res.XModuleResources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Message;
import android.os.Process;
import android.os.UserManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import  tk.wasdennnoch.androidn_ify.extracted.android.pm.PackageManager;


public class PackageInstallerHooks {

    private static final String TAG = "PackageInstallerHooks";
    private static final String PACKAGE_PACKAGEINSTALLER = XposedHook.PACKAGE_PACKAGEINSTALLER;

    private static final int INSTALL_COMPLETE = 1;
    private static final int UNINSTALL_COMPLETE = 1;
    private static final String EXTRA_INSTALL_RESULT = "android.intent.extra.INSTALL_RESULT";
    private static final String ACTION_USER_SETTINGS = "android.settings.USER_SETTINGS";
    private static final int DLG_OUT_OF_SPACE = 1;
    private static final int DELETE_FAILED_OWNER_BLOCKED = -4;
    private static final int USER_NULL = -10000;

    public static void hook(ClassLoader classLoader) {

        try {
            if (ConfigUtils.others().package_installer) {
                Class<?> classPackageInstaller = XposedHelpers.findClass(PACKAGE_PACKAGEINSTALLER + ".PackageInstallerActivity", classLoader);
                Class<?> classInstallAppProgress = XposedHelpers.findClass(PACKAGE_PACKAGEINSTALLER + ".InstallAppProgress", classLoader);
                Class<?> classUninstallAppProgress = XposedHelpers.findClass(PACKAGE_PACKAGEINSTALLER + ".UninstallAppProgress", classLoader);
                Class<?> classInstallHandler = XposedHelpers.findClass(PACKAGE_PACKAGEINSTALLER + ".InstallAppProgress$1", classLoader);
                Class<?> classUninstallHandler = XposedHelpers.findClass(PACKAGE_PACKAGEINSTALLER + ".UninstallAppProgress$1", classLoader);

                XposedHelpers.findAndHookMethod(classPackageInstaller, "startInstallConfirm", startInstallConfirmHook);
                XposedHelpers.findAndHookMethod(classInstallHandler, "handleMessage", Message.class, handleInstallMessage);
                XposedHelpers.findAndHookMethod(classUninstallHandler, "handleMessage", Message.class, handleUninstallMessageHook);
                XposedHelpers.findAndHookMethod(classInstallAppProgress, "getExplanationFromErrorCode", int.class, getExplanationFromErrorCode);
                XposedHelpers.findAndHookMethod(classUninstallAppProgress, "initView", initViewHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking PackageInstaller", t);
        }

    }

    public static void hookRes(final XC_InitPackageResources.InitPackageResourcesParam resparam, String modulePath) {

        try {
            if (ConfigUtils.others().package_installer) {
                final XModuleResources modRes = XModuleResources.createInstance(modulePath, resparam.res);

                resparam.res.setReplacement(PACKAGE_PACKAGEINSTALLER, "string", "install_failed_inconsistent_certificates", modRes.fwd(R.string.install_failed_conflict));
                resparam.res.setReplacement(PACKAGE_PACKAGEINSTALLER, "string", "install_failed_cpu_abi_incompatible", modRes.fwd(R.string.install_failed_incompatible));
                resparam.res.setReplacement(PACKAGE_PACKAGEINSTALLER, "string", "install_failed_older_sdk", modRes.fwd(R.string.install_failed_blocked));

                resparam.res.hookLayout(PACKAGE_PACKAGEINSTALLER, "layout", "install_start", install_start);
                resparam.res.hookLayout(PACKAGE_PACKAGEINSTALLER, "layout", "app_details", app_details);
                resparam.res.hookLayout(PACKAGE_PACKAGEINSTALLER, "layout", "install_confirm", install_confirm);
                resparam.res.hookLayout(PACKAGE_PACKAGEINSTALLER, "layout", "op_progress", op_progress);
                resparam.res.hookLayout(PACKAGE_PACKAGEINSTALLER, "layout", "uninstall_confirm", uninstall_confirm);
                resparam.res.hookLayout(PACKAGE_PACKAGEINSTALLER, "layout", "uninstall_progress", uninstall_progress);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error hooking PackageInstaller resources", t);
        }
    }

    private static XC_MethodHook handleUninstallMessageHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            Message msg = (Message) param.args[0];
            final Activity uninstallAppProgress = (Activity) XposedHelpers.getSurroundingThis(param.thisObject);
            Button mUsersButton;
            mUsersButton = (Button)uninstallAppProgress.findViewById(R.id.users_button);
            if (msg.what == UNINSTALL_COMPLETE) {
                if (msg.arg1 == DELETE_FAILED_OWNER_BLOCKED) {
                    UserManager userManager =
                            (UserManager) uninstallAppProgress.getSystemService(Context.USER_SERVICE);
                    int myUserId = (Process.myUid() / 100000);
                    int blockingUserId = USER_NULL;
                    if (!(boolean) XposedHelpers.callMethod(uninstallAppProgress, "isProfileOfOrSame", userManager, myUserId, blockingUserId)) {
                        mUsersButton.setVisibility(View.VISIBLE);
                    }
                }
                uninstallAppProgress.findViewById(R.id.progress_view).setVisibility(View.GONE);
                uninstallAppProgress.findViewById(R.id.status_view).setVisibility(View.VISIBLE);
            }
        }
    };

    private static XC_MethodHook initViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            final Activity uninstallAppProgress = (Activity)param.thisObject;
            Button mUsersButton;
            mUsersButton = (Button)uninstallAppProgress.findViewById(R.id.users_button);
            mUsersButton.setVisibility(View.GONE);
            mUsersButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ACTION_USER_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
                    uninstallAppProgress.startActivity(intent);
                    uninstallAppProgress.finish();
                }
            });
            ((LinearLayout)XposedHelpers.getObjectField(uninstallAppProgress, "mOkPanel")).setVisibility(View.VISIBLE);
        }
    };

    private static XC_MethodHook startInstallConfirmHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!ConfigUtils.M)
                return;
            Activity packageInstallerActivity = (Activity) param.thisObject;
            Context context = packageInstallerActivity.getApplicationContext();
            int dividerId = context.getResources().getIdentifier("divider", "id", PACKAGE_PACKAGEINSTALLER);

            TabHost tabHost = (TabHost) packageInstallerActivity.findViewById(android.R.id.tabhost);
            tabHost.setVisibility(View.VISIBLE);
            PackageInfo mPkgInfo = (PackageInfo) XposedHelpers.getObjectField(packageInstallerActivity, "mPkgInfo");
            ApplicationInfo mAppInfo = (ApplicationInfo) XposedHelpers.getObjectField(packageInstallerActivity, "mAppInfo");
            boolean permVisible = false;
            AppSecurityPermissions perms = new AppSecurityPermissions(context, mPkgInfo);
            final int N = perms.getPermissionCount(AppSecurityPermissions.WHICH_ALL);
            boolean supportsRuntimePermissions = mPkgInfo.applicationInfo.targetSdkVersion
                    >= Build.VERSION_CODES.M;
            if (mAppInfo != null) {
                boolean newPermissionsFound = false;
                if (!supportsRuntimePermissions) {
                    newPermissionsFound =
                            (perms.getPermissionCount(AppSecurityPermissions.WHICH_NEW) > 0);
                    if (newPermissionsFound) {
                        permVisible = true;
                    }
                }
            }
            if (!supportsRuntimePermissions && N > 0) {
                permVisible = true;
            }
            if (!permVisible) {
                if (mAppInfo != null)
                    packageInstallerActivity.findViewById(dividerId).setVisibility(View.VISIBLE);
                tabHost.setVisibility(View.INVISIBLE);
            }
        }
    };

    private static XC_MethodReplacement handleInstallMessage = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            Message msg = (Message) param.args[0];
            final Activity installAppProgress = (Activity) XposedHelpers.getSurroundingThis(param.thisObject);
            Context context = installAppProgress.getApplicationContext();
            final ProgressBar mProgressBar = (ProgressBar) XposedHelpers.getObjectField(installAppProgress, "mProgressBar");
            final Button mLaunchButton = (Button) XposedHelpers.getObjectField(installAppProgress, "mLaunchButton");
            final Button mDoneButton = (Button) XposedHelpers.getObjectField(installAppProgress, "mDoneButton");
            final View mOkPanel = (View) XposedHelpers.getObjectField(installAppProgress, "mOkPanel");
            final TextView mExplanationTextView = (TextView) XposedHelpers.getObjectField(installAppProgress, "mExplanationTextView");
            final ApplicationInfo mAppInfo = (ApplicationInfo) XposedHelpers.getObjectField(installAppProgress, "mAppInfo");
            Intent mLaunchIntent;
            final ResourceUtils resUtils = ResourceUtils.getInstance(context);
            final int centerTextId = context.getResources().getIdentifier("center_text", "id", PACKAGE_PACKAGEINSTALLER);
            switch (msg.what) {
                case INSTALL_COMPLETE:
                    if (installAppProgress.getIntent().getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)) {
                        Intent result = new Intent();
                        result.putExtra(EXTRA_INSTALL_RESULT, msg.arg1);
                        installAppProgress.setResult(msg.arg1 == PackageManager.INSTALL_SUCCEEDED
                                        ? Activity.RESULT_OK : Activity.RESULT_FIRST_USER,
                                result);
                        XposedHelpers.callMethod(installAppProgress, "clearCachedApkIfNeededAndFinish");
                        return null;
                    }
                    // Update the status text
                    mProgressBar.setVisibility(View.GONE);
                    // Show the ok button
                    int centerTextLabel;
                    int centerExplanationLabel = -1;
                    if (msg.arg1 == PackageManager.INSTALL_SUCCEEDED) {
                        mLaunchButton.setVisibility(View.VISIBLE);
                        ((ImageView) installAppProgress.findViewById(R.id.center_icon))
                                .setImageDrawable(resUtils.getDrawable(R.drawable.ic_done_92));
                        centerTextLabel = context.getResources().getIdentifier("install_done", "string", PACKAGE_PACKAGEINSTALLER);
                        // Enable or disable launch button
                        XposedHelpers.setObjectField(installAppProgress, "mLaunchIntent", installAppProgress.getPackageManager().getLaunchIntentForPackage(
                                mAppInfo.packageName));
                        mLaunchIntent = installAppProgress.getPackageManager().getLaunchIntentForPackage(
                                mAppInfo.packageName);
                        boolean enabled = false;
                        if (mLaunchIntent != null) {
                            List<ResolveInfo> list = installAppProgress.getPackageManager().
                                    queryIntentActivities(mLaunchIntent, 0);
                            if (list != null && list.size() > 0) {
                                enabled = true;
                            }
                        }
                        if (enabled) {
                            mLaunchButton.setOnClickListener((View.OnClickListener) installAppProgress);
                        } else {
                            mLaunchButton.setEnabled(false);
                        }
                    } else if (msg.arg1 == PackageManager.INSTALL_FAILED_INSUFFICIENT_STORAGE) {
                        XposedHelpers.callMethod(installAppProgress, "showDialogInner", DLG_OUT_OF_SPACE);
                        return null;
                    } else {
                        // Generic error handling for all other error codes.
                        ((ImageView) installAppProgress.findViewById(R.id.center_icon))
                                .setImageDrawable(resUtils.getDrawable(R.drawable.ic_report_problem_92));
                        centerExplanationLabel = (int) XposedHelpers.callMethod(installAppProgress, "getExplanationFromErrorCode", msg.arg1);
                        centerTextLabel = context.getResources().getIdentifier("install_failed", "string", PACKAGE_PACKAGEINSTALLER);
                        mLaunchButton.setVisibility(View.GONE);
                    }
                    if (centerExplanationLabel != -1) {
                        mExplanationTextView.setText(centerExplanationLabel);
                        installAppProgress.findViewById(R.id.center_view).setVisibility(View.GONE);
                        ((TextView) installAppProgress.findViewById(R.id.explanation_status)).setText(centerTextLabel);
                        installAppProgress.findViewById(R.id.explanation_view).setVisibility(View.VISIBLE);
                    } else {
                        ((TextView) installAppProgress.findViewById(centerTextId)).setText(centerTextLabel);
                        installAppProgress.findViewById(R.id.center_view).setVisibility(View.VISIBLE);
                        installAppProgress.findViewById(R.id.explanation_view).setVisibility(View.GONE);
                    }
                    mDoneButton.setOnClickListener((View.OnClickListener) installAppProgress);
                    mOkPanel.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
            return null;
        }
    };

    private static XC_MethodReplacement getExplanationFromErrorCode = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            /* There's certainly a better way to do it but I'm lazy */
            int errCode = (int) param.args[0];
            Activity activity = (Activity) param.thisObject;
            Log.d(TAG, "Installation error code: " + errCode);
            switch (errCode) {
                case PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES:
                case PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES:
                case PackageManager.INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING:
                case PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID:
                case PackageManager.INSTALL_FAILED_USER_RESTRICTED:
                case PackageManager.INSTALL_FAILED_DUPLICATE_PERMISSION:
                case PackageManager.INSTALL_FAILED_SHARED_USER_INCOMPATIBLE:
                    return activity.getResources().getIdentifier("install_failed_older_sdk", "string", PACKAGE_PACKAGEINSTALLER);
                case PackageManager.INSTALL_FAILED_ALREADY_EXISTS:
                case PackageManager.INSTALL_FAILED_DUPLICATE_PACKAGE:
                case PackageManager.INSTALL_FAILED_VERSION_DOWNGRADE:
                case PackageManager.INSTALL_FAILED_UPDATE_INCOMPATIBLE:
                case PackageManager.INSTALL_FAILED_REPLACE_COULDNT_DELETE:
                    return activity.getResources().getIdentifier("install_failed_inconsistent_certificates", "string", PACKAGE_PACKAGEINSTALLER);
                case PackageManager.INSTALL_FAILED_OLDER_SDK:
                case PackageManager.INSTALL_FAILED_NEWER_SDK:
                case PackageManager.INSTALL_FAILED_CPU_ABI_INCOMPATIBLE:
                case PackageManager.INSTALL_FAILED_MISSING_FEATURE:
                case PackageManager.INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE:
                case PackageManager.INSTALL_FAILED_NO_MATCHING_ABIS:
                case PackageManager.NO_NATIVE_LIBRARIES:
                case PackageManager.INSTALL_FAILED_MISSING_SHARED_LIBRARY:
                    return activity.getResources().getIdentifier("install_failed_cpu_abi_incompatible", "string", PACKAGE_PACKAGEINSTALLER);
                case PackageManager.INSTALL_FAILED_INVALID_APK:
                case PackageManager.INSTALL_FAILED_INVALID_URI:
                case PackageManager.INSTALL_PARSE_FAILED_NOT_APK:
                case PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST:
                case PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME:
                case PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED:
                case PackageManager.INSTALL_PARSE_FAILED_MANIFEST_EMPTY:
                    return activity.getResources().getIdentifier("install_failed_invalid_apk", "string", PACKAGE_PACKAGEINSTALLER);
                default:
                    return -1;
            }
        }
    };

    private static XC_LayoutInflated install_start = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout thisLayout = (FrameLayout) liparam.view;
            RelativeLayout relativeLayout = (RelativeLayout) thisLayout.getChildAt(0);
            Context context = relativeLayout.getContext();

            int appSnippetId = context.getResources().getIdentifier("app_snippet", "id", PACKAGE_PACKAGEINSTALLER);
            int installConfirmId = context.getResources().getIdentifier("install_confirm_panel", "id", PACKAGE_PACKAGEINSTALLER);

            LinearLayout appSnippetLayout = (LinearLayout) relativeLayout.findViewById(appSnippetId);
            LinearLayout installConfirmLayout = (LinearLayout) relativeLayout.findViewById(installConfirmId);
            LinearLayout newLayout = new LinearLayout(context);

            LinearLayout.LayoutParams newLayoutLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            newLayout.setLayoutParams(newLayoutLp);
            newLayout.setOrientation(LinearLayout.VERTICAL);

            ((ViewGroup) appSnippetLayout.getParent()).removeAllViews();
            thisLayout.removeView(relativeLayout);
            thisLayout.addView(newLayout);
            newLayout.addView(appSnippetLayout);
            newLayout.addView(installConfirmLayout);
        }
    };

    private static XC_LayoutInflated app_details = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            RelativeLayout thisLayout = (RelativeLayout) liparam.view;
            Context context = thisLayout.getContext();
            ResourceUtils resUtils = ResourceUtils.getInstance(context);

            int appIconSize = resUtils.getDimensionPixelSize(R.dimen.app_icon_size);
            int appIconMargin = resUtils.getDimensionPixelSize(R.dimen.app_icon_margin);
            int appNameMarginLeft = resUtils.getDimensionPixelSize(R.dimen.app_name_margin_left);
            int appNameMarginRight = resUtils.getDimensionPixelSize(R.dimen.app_name_margin_right);

            final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                    new int[]{android.R.attr.actionBarSize, android.R.attr.colorPrimary});
            int actionBarSize = (int) styledAttributes.getDimension(0, 0);
            int colorPrimary = styledAttributes.getColor(1, 0);
            styledAttributes.recycle();


            int thisLayoutId = context.getResources().getIdentifier("app_snippet", "id", PACKAGE_PACKAGEINSTALLER);
            int appIconId = context.getResources().getIdentifier("app_icon", "id", PACKAGE_PACKAGEINSTALLER);
            int appNameId = context.getResources().getIdentifier("app_name", "id", PACKAGE_PACKAGEINSTALLER);

            ImageView appIcon = (ImageView) thisLayout.findViewById(appIconId);
            TextView appName = new TextView(context);
            LinearLayout appSnippetLayout = new LinearLayout(context);

            appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams appIconLp = new LinearLayout.LayoutParams(appIconSize, appIconSize, Gravity.CENTER_VERTICAL);
            appIconLp.gravity = Gravity.CENTER_VERTICAL;
            appIconLp.setMargins(appIconMargin, 0, 0, 0);
            appIconLp.weight = 0;
            appIcon.setLayoutParams(appIconLp);

            appName.setId(appNameId);

            LinearLayout.LayoutParams appNameLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            appNameLp.gravity = Gravity.CENTER_VERTICAL;
            appNameLp.setMargins(appNameMarginLeft, 0, appNameMarginRight, 0);
            appNameLp.weight = 0;
            appName.setLayoutParams(appNameLp);
            if (ConfigUtils.M)
                appName.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Widget_ActionBar_Title);
            else
                appName.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Widget_ActionBar_Title);

            appSnippetLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams appSnippetLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, actionBarSize);
            appSnippetLayout.setLayoutParams(appSnippetLp);
            appSnippetLayout.setId(thisLayoutId);
            appSnippetLayout.setBackgroundColor(colorPrimary);

            thisLayout.removeAllViews();
            ViewGroup parent = (ViewGroup) thisLayout.getParent();
            parent.removeView(thisLayout);
            parent.addView(appSnippetLayout);
            appSnippetLayout.addView(appIcon);
            appSnippetLayout.addView(appName);
        }
    };

    private static XC_LayoutInflated install_confirm = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            LinearLayout thisLayout = (LinearLayout) liparam.view;
            Context context = thisLayout.getContext();
            ResourceUtils resUtils = ResourceUtils.getInstance(context);

            final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                    new int[]{android.R.attr.colorPrimary, android.R.attr.textColorPrimary, android.R.attr.windowBackground});
            int colorPrimary = styledAttributes.getColor(0, 0);
            int textColorPrimary = styledAttributes.getColor(1, 0);
            Drawable windowBackground = styledAttributes.getDrawable(2);
            styledAttributes.recycle();

            int installConfirmPadding = resUtils.getDimensionPixelSize(R.dimen.install_confirm_question_padding);
            int buttonContainerPadding = resUtils.getDimensionPixelSize(R.dimen.button_container_padding);
            int dividerHeight = resUtils.getDimensionPixelSize(R.dimen.divider_height);

            int installConfirmQuestionId = context.getResources().getIdentifier("install_confirm_question", "id", PACKAGE_PACKAGEINSTALLER);
            int dividerId = context.getResources().getIdentifier("divider", "id", PACKAGE_PACKAGEINSTALLER);
            int fillerId = context.getResources().getIdentifier("filler", "id", PACKAGE_PACKAGEINSTALLER);
            int okButtonId = context.getResources().getIdentifier("ok_button", "id", PACKAGE_PACKAGEINSTALLER);
            int cancelButtonId = context.getResources().getIdentifier("cancel_button", "id", PACKAGE_PACKAGEINSTALLER);
            int horizontalScrollViewId = context.getResources().getIdentifier("tabscontainer", "id", PACKAGE_PACKAGEINSTALLER);

            ImageView divider = (ImageView) thisLayout.findViewById(dividerId);
            FrameLayout filler = (FrameLayout) thisLayout.findViewById(fillerId);
            Button okButton = (Button) thisLayout.findViewById(okButtonId);
            Button cancelButton = (Button) thisLayout.findViewById(cancelButtonId);
            LinearLayout buttonContainer = (LinearLayout) okButton.getParent();
            LinearLayout buttonContainerParent = (LinearLayout) buttonContainer.getParent();
            TextView installConfirmQuestion = (TextView) thisLayout.findViewById(installConfirmQuestionId);
            HorizontalScrollView horizontalScrollView = (HorizontalScrollView) thisLayout.findViewById(horizontalScrollViewId);

            horizontalScrollView.setBackground(null);
            horizontalScrollView.setBackgroundColor(colorPrimary);

            View spacer = new View(context);
            View dividerView = new View(context);

            dividerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dividerHeight));
            dividerView.setId(dividerId);
            dividerView.setVisibility(View.GONE);
            dividerView.setBackgroundColor(colorPrimary);

            filler.setLayoutParams(new LinearLayout.LayoutParams(0, 0));

            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));

            installConfirmQuestion.setPadding(installConfirmPadding, 0, installConfirmPadding, 0);
            installConfirmQuestion.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            installConfirmQuestion.setBackgroundColor(colorPrimary);
            installConfirmQuestion.setTextColor(textColorPrimary);

            buttonContainer.setBackground(windowBackground);
            buttonContainer.setPadding(buttonContainerPadding, buttonContainerPadding, buttonContainerPadding, buttonContainerPadding);

            cancelButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            okButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            thisLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


            thisLayout.removeView(divider);
            thisLayout.addView(dividerView, 1);
            buttonContainer.removeAllViews();
            buttonContainer.addView(spacer);
            buttonContainer.addView(cancelButton);
            buttonContainer.addView(okButton);
            buttonContainerParent.removeView(buttonContainer);
            thisLayout.removeView(buttonContainerParent);
            thisLayout.addView(buttonContainer);
        }
    };

    private static XC_LayoutInflated op_progress = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout parentLayout = (FrameLayout) liparam.view;
            RelativeLayout thisLayout = (RelativeLayout) parentLayout.getChildAt(0);
            Context context = parentLayout.getContext();
            ResourceUtils resUtils = ResourceUtils.getInstance(context);

            int centerViewPadding = resUtils.getDimensionPixelSize(R.dimen.center_view_padding);
            int explanationViewPadding = resUtils.getDimensionPixelSize(R.dimen.explanation_view_padding);
            int buttonsPanelPadding = resUtils.getDimensionPixelSize(R.dimen.buttons_panel_padding);
            int explanationStatusMargin = resUtils.getDimensionPixelSize(R.dimen.explanation_status_margin);
            int progressBarWidth = resUtils.getDimensionPixelSize(R.dimen.progress_bar_width);
            Drawable centerIconDrawable = resUtils.getDrawable(R.drawable.ic_android_92);

            int appSnippetId = context.getResources().getIdentifier("app_snippet", "id", PACKAGE_PACKAGEINSTALLER);
            int centerViewId = R.id.center_view;
            int centerIconId = R.id.center_icon;
            int explanationViewId = R.id.explanation_view;
            int centerExplanationId = context.getResources().getIdentifier("center_explanation", "id", PACKAGE_PACKAGEINSTALLER);
            int progressBarId = context.getResources().getIdentifier("progress_bar", "id", PACKAGE_PACKAGEINSTALLER);
            int centerTextId = context.getResources().getIdentifier("center_text", "id", PACKAGE_PACKAGEINSTALLER);
            int explanationStatusId = R.id.explanation_status;
            int buttonsPanelId = context.getResources().getIdentifier("buttons_panel", "id", PACKAGE_PACKAGEINSTALLER);
            int doneButtonId = context.getResources().getIdentifier("done_button", "id", PACKAGE_PACKAGEINSTALLER);
            int launchButtonId = context.getResources().getIdentifier("launch_button", "id", PACKAGE_PACKAGEINSTALLER);

            LinearLayout appSnippetLayout = (LinearLayout) thisLayout.findViewById(appSnippetId);
            LinearLayout buttonsPanel = new LinearLayout(context, null, android.R.attr.buttonBarStyle);
            LinearLayout centerView = new LinearLayout(context);
            ImageView centerIcon = new ImageView(context);
            ScrollView explanationView = new ScrollView(context);
            ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            TextView centerText = (TextView) thisLayout.findViewById(centerTextId);
            TextView explanationStatus = new TextView(context, null, android.R.attr.textAppearanceMedium);
            LinearLayout explanationContainer = new LinearLayout(context);
            TextView explanationText = new TextView(context, null, android.R.attr.textAppearanceSmall);
            View spacer = new View(context);
            Button doneButton = (Button) thisLayout.findViewById(doneButtonId);
            Button launchButton = (Button) thisLayout.findViewById(launchButtonId);
            TextView centerExplanation = (TextView) thisLayout.findViewById(centerExplanationId);

            centerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            centerView.setGravity(Gravity.CENTER);
            centerView.setOrientation(LinearLayout.VERTICAL);
            centerView.setPadding(centerViewPadding, 0, centerViewPadding, 0);
            centerView.setId(centerViewId);

            centerIcon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            centerIcon.setImageDrawable(centerIconDrawable);
            centerIcon.setContentDescription(null);
            centerIcon.setId(centerIconId);

            progressBar.setLayoutParams(new LinearLayout.LayoutParams(progressBarWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            progressBar.setIndeterminate(true);
            progressBar.setId(progressBarId);

            centerText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            centerText.setGravity(Gravity.CENTER_HORIZONTAL);
            centerText.setPadding(0, 0, 0, 0);

            explanationView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            explanationView.setVisibility(View.GONE);
            explanationView.setPadding(explanationViewPadding, 0, explanationViewPadding, 0);
            explanationView.setId(explanationViewId);

            explanationContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            explanationContainer.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams explanationStatusLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            explanationStatusLp.setMargins(0, 0, 0, explanationStatusMargin);
            explanationStatus.setLayoutParams(explanationStatusLp);
            explanationStatus.setId(explanationStatusId);

            explanationText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            explanationText.setId(centerExplanationId);

            buttonsPanel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            buttonsPanel.setMeasureWithLargestChildEnabled(true);
            buttonsPanel.setOrientation(LinearLayout.HORIZONTAL);
            buttonsPanel.setPadding(buttonsPanelPadding, buttonsPanelPadding, buttonsPanelPadding, buttonsPanelPadding);
            buttonsPanel.setId(buttonsPanelId);

            appSnippetLayout.setBackgroundColor(0);

            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));

            doneButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            launchButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            ((ViewGroup) doneButton.getParent()).removeAllViews();
            buttonsPanel.removeAllViews();
            ((ViewGroup) centerExplanation.getParent()).removeAllViews();

            thisLayout.removeAllViews();
            parentLayout.removeAllViews();

            centerView.addView(centerIcon);
            centerView.addView(progressBar);
            centerView.addView(centerText);

            explanationContainer.addView(explanationStatus);
            explanationContainer.addView(explanationText);

            explanationView.addView(explanationContainer);

            buttonsPanel.addView(spacer);
            buttonsPanel.addView(doneButton);
            buttonsPanel.addView(launchButton);

            LinearLayout newLayout = new LinearLayout(context);
            newLayout.setOrientation(LinearLayout.VERTICAL);
            newLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            newLayout.addView(appSnippetLayout);
            newLayout.addView(centerView);
            newLayout.addView(explanationView);
            newLayout.addView(buttonsPanel);

            parentLayout.addView(newLayout);
        }
    };

    private static XC_LayoutInflated uninstall_confirm = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout parentLayout = (FrameLayout) liparam.view;
            LinearLayout thisLayout = (LinearLayout) parentLayout.getChildAt(0);
            LinearLayout containerLayout = (LinearLayout) thisLayout.getChildAt(0);

            Context context = parentLayout.getContext();
            ResourceUtils resUtils = ResourceUtils.getInstance(context);

            int activityTextPadding = resUtils.getDimensionPixelSize(R.dimen.activity_text_padding);
            int topDividerSidePadding = resUtils.getDimensionPixelSize(R.dimen.top_divider_side_padding);
            int topDividerTopPadding = resUtils.getDimensionPixelSize(R.dimen.top_divider_top_padding);

            int activityTextId = context.getResources().getIdentifier("activity_text", "id", PACKAGE_PACKAGEINSTALLER);
            int uninstallActivitySnippetId = context.getResources().getIdentifier("uninstall_activity_snippet", "id", PACKAGE_PACKAGEINSTALLER);
            int uninstallConfirmId = context.getResources().getIdentifier("uninstall_confirm", "id", PACKAGE_PACKAGEINSTALLER);
            int progressBarId = context.getResources().getIdentifier("progress_bar", "id", PACKAGE_PACKAGEINSTALLER);
            int topDividerId = context.getResources().getIdentifier("top_divider", "id", PACKAGE_PACKAGEINSTALLER);

            TextView activityText = (TextView) thisLayout.findViewById(activityTextId);
            LinearLayout uninstallActivitySnippet = (LinearLayout) thisLayout.findViewById(uninstallActivitySnippetId);
            TextView uninstallConfirm = (TextView) thisLayout.findViewById(uninstallConfirmId);
            FrameLayout topDivider = new FrameLayout(context);
            ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);

            activityText.setPaddingRelative(activityTextPadding, 0, activityTextPadding, 0);

            topDivider.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            topDivider.setPaddingRelative(topDividerSidePadding, topDividerTopPadding, topDividerSidePadding, 0);
            topDivider.setId(topDividerId);

            progressBar.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            progressBar.setId(progressBarId);

            topDivider.addView(progressBar);

            containerLayout.removeAllViews();

            containerLayout.addView(activityText);
            containerLayout.addView(uninstallActivitySnippet);
            containerLayout.addView(topDivider);
            containerLayout.addView(uninstallConfirm);
        }
    };

    private static XC_LayoutInflated uninstall_progress = new XC_LayoutInflated() {
        @Override
        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
            FrameLayout parentLayout = (FrameLayout) liparam.view;
            RelativeLayout thisLayout = (RelativeLayout) parentLayout.getChildAt(0);

            Context context = parentLayout.getContext();
            ResourceUtils resUtils = ResourceUtils.getInstance(context);

            int progressViewPadding = resUtils.getDimensionPixelSize(R.dimen.progress_view_padding);
            int imageViewMargin = resUtils.getDimensionPixelSize(R.dimen.image_margin);
            int progressBarWidth = resUtils.getDimensionPixelSize(R.dimen.progress_bar_width);
            int statusViewPadding = resUtils.getDimensionPixelSize(R.dimen.status_view_padding);
            int okPanelPadding = resUtils.getDimensionPixelSize(R.dimen.ok_panel_padding);

            int okPanelId = context.getResources().getIdentifier("ok_panel", "id", PACKAGE_PACKAGEINSTALLER);
            int appSnippetId = context.getResources().getIdentifier("app_snippet", "id", PACKAGE_PACKAGEINSTALLER);
            int uninstallHolderId = context.getResources().getIdentifier("uninstall_holder", "id", PACKAGE_PACKAGEINSTALLER);
            int okButtonId = context.getResources().getIdentifier("ok_button", "id", PACKAGE_PACKAGEINSTALLER);
            int uninstallingScrollViewId = context.getResources().getIdentifier("uninstalling_scrollview", "id", PACKAGE_PACKAGEINSTALLER);
            int centerTextId = context.getResources().getIdentifier("center_text", "id", PACKAGE_PACKAGEINSTALLER);
            int deviceManagerButtonId = context.getResources().getIdentifier("device_manager_button", "id", PACKAGE_PACKAGEINSTALLER);
            int progressBarId = context.getResources().getIdentifier("progress_bar", "id", PACKAGE_PACKAGEINSTALLER);

            LinearLayout appSnippet = (LinearLayout) thisLayout.findViewById(appSnippetId);
            TextView centerText = (TextView) thisLayout.findViewById(centerTextId);
            ScrollView uninstallingScrollView = (ScrollView) thisLayout.findViewById(uninstallingScrollViewId);
            LinearLayout uninstallHolder = (LinearLayout) thisLayout.findViewById(uninstallHolderId);
            LinearLayout okPanel = (LinearLayout) thisLayout.findViewById(okPanelId);
            LinearLayout newLayout = new LinearLayout(context);
            LinearLayout progressViewLayout = new LinearLayout(context);
            ImageView imageView = new ImageView(context);
            ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            TextView uninstallingText = new TextView(context, null, android.R.attr.textAppearanceMedium);
            ScrollView statusView = new ScrollView(context);
            LinearLayout newOkPanel = new LinearLayout(context, null, android.R.attr.buttonBarStyle);
            View spacer = new View(context);
            Button deviceManagerButton = new Button(context, null, android.R.attr.buttonBarButtonStyle);
            Button manageUsersButton = new Button(context, null, android.R.attr.buttonBarButtonStyle);
            Button okButton = new Button(context, null, android.R.attr.buttonBarButtonStyle);

            newLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            newLayout.setOrientation(LinearLayout.VERTICAL);

            progressViewLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            progressViewLayout.setOrientation(LinearLayout.VERTICAL);
            progressViewLayout.setGravity(Gravity.CENTER);
            progressViewLayout.setPadding(progressViewPadding, progressViewPadding, progressViewPadding, progressViewPadding);
            progressViewLayout.setId(R.id.progress_view);

            LinearLayout.LayoutParams imageViewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            imageViewLp.setMargins(0, 0, 0, imageViewMargin);
            imageView.setLayoutParams(imageViewLp);
            imageView.setContentDescription(null);
            imageView.setImageDrawable(resUtils.getDrawable(R.drawable.ic_android_92));

            progressBar.setLayoutParams(new LinearLayout.LayoutParams(progressBarWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            progressBar.setIndeterminate(true);
            progressBar.setId(progressBarId);

            uninstallingText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            uninstallingText.setGravity(Gravity.CENTER_HORIZONTAL);
            uninstallingText.setText(context.getResources().getIdentifier("uninstalling", "string", PACKAGE_PACKAGEINSTALLER));

            statusView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            statusView.setVisibility(View.GONE);
            statusView.setPadding(statusViewPadding, statusViewPadding, statusViewPadding, statusViewPadding);
            statusView.setId(R.id.status_view);

            centerText.setText(null);

            newOkPanel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            newOkPanel.setOrientation(LinearLayout.HORIZONTAL);
            newOkPanel.setMeasureWithLargestChildEnabled(true);
            newOkPanel.setVisibility(View.GONE);
            newOkPanel.setPadding(okPanelPadding, okPanelPadding, okPanelPadding, okPanelPadding);
            newOkPanel.setId(okPanelId);

            appSnippet.setBackgroundColor(0);

            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));

            deviceManagerButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            deviceManagerButton.setVisibility(View.GONE);
            deviceManagerButton.setMaxLines(2);
            deviceManagerButton.setText(context.getResources().getIdentifier("manage_device_administrators", "string", PACKAGE_PACKAGEINSTALLER));
            deviceManagerButton.setId(deviceManagerButtonId);

            manageUsersButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            manageUsersButton.setVisibility(View.GONE);
            manageUsersButton.setMaxLines(2);
            manageUsersButton.setText(resUtils.getResources().getString(R.string.manage_users));
            manageUsersButton.setId(R.id.users_button);

            okButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            okButton.setText(context.getResources().getIdentifier("ok", "string", PACKAGE_PACKAGEINSTALLER));
            okButton.setMaxLines(2);
            okButton.setId(okButtonId);

            ((LinearLayout) okPanel.getChildAt(0)).removeAllViews();
            okPanel.removeAllViews();
            uninstallingScrollView.removeAllViews();
            uninstallHolder.removeAllViews();
            thisLayout.removeAllViews();
            parentLayout.removeAllViews();

            progressViewLayout.addView(imageView);
            progressViewLayout.addView(progressBar);
            progressViewLayout.addView(uninstallingText);

            statusView.addView(centerText);

            newOkPanel.addView(spacer);
            newOkPanel.addView(deviceManagerButton);
            newOkPanel.addView(manageUsersButton);
            newOkPanel.addView(okButton);

            newLayout.addView(appSnippet);
            newLayout.addView(progressViewLayout);
            newLayout.addView(statusView);
            newLayout.addView(newOkPanel);

            parentLayout.addView(newLayout);
        }
    };
}
