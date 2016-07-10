package tk.wasdennnoch.androidn_ify.phone.emergency;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.ui.EmergencyInfoActivity;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class EmergencyHooks {

    private static final String TAG = "EmergencyHooks";
    public static final String PACKAGE_PHONE = XposedHook.PACKAGE_PHONE;
    private static Button mEmergencyButton;
    private static Context mContext;
    private static ResourceUtils mResUtils;
    private static boolean mClicked = false;

    private static XC_MethodReplacement setupAssistActionsHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            try {
                FrameLayout emergencyActionGroup = (FrameLayout) param.thisObject;
                mContext = emergencyActionGroup.getContext();
                mResUtils = ResourceUtils.getInstance(mContext);

                int button1Id = mContext.getResources().getIdentifier("action1", "id", PACKAGE_PHONE);
                int button2Id = mContext.getResources().getIdentifier("action2", "id", PACKAGE_PHONE);
                int button3Id = mContext.getResources().getIdentifier("action3", "id", PACKAGE_PHONE);

                int[] buttonIds = new int[]{button1Id, button2Id, button3Id};

                for (int i = 0; i < 3; i++) {
                    Button button = (Button) emergencyActionGroup.findViewById(buttonIds[i]);

                    if (button.getId() == button1Id) {
                        button.setVisibility(View.VISIBLE);
                        button.setText(mResUtils.getString(R.string.emergency_info));
                        button.setOnClickListener(clickListener);
                        mEmergencyButton = button;
                    } else {
                        button.setVisibility(View.GONE);
                    }
                }

                mClicked = false;
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error in setupAssistActionsHook", t);
            }

            return null; // void
        }
    };

    public static void hook(ClassLoader classLoader) {
        try {
            if (ConfigUtils.lockscreen().enable_emergency_info) {
                Class classEmergencyActionGroup = XposedHelpers.findClass("com.android.phone.EmergencyActionGroup", classLoader);
                XposedHelpers.findAndHookMethod(classEmergencyActionGroup, "setupAssistActions", setupAssistActionsHook);
            }
        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in emergency hook", t);
        }
    }

    private static View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                if (!mClicked) {
                    mEmergencyButton.getBackground().setColorFilter(mResUtils.getColor(R.color.md_red_500), PorterDuff.Mode.SRC_ATOP);
                    mClicked = true;
                } else {
                    mEmergencyButton.getBackground().clearColorFilter();
                    mClicked = false;

                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClassName("tk.wasdennnoch.androidn_ify",
                            EmergencyInfoActivity.class.getName());

                    try {
                        mContext.startActivity(intent);
                    } catch (Exception e) {
                        XposedHook.logE(TAG, "Unable to start activity " + intent.toString(), e);
                    }
                }
            } catch (Throwable t) {
                XposedHook.logE(TAG, "Error in onClick", t);
            }
        }
    };

}
