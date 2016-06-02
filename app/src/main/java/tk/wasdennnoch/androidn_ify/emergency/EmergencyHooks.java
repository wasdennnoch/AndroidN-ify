package tk.wasdennnoch.androidn_ify.emergency;

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

public class EmergencyHooks implements View.OnClickListener {

    private static final String TAG = "EmergencyHooks";
    public static final String PACKAGE_PHONE = "com.android.phone";
    private Button mEmergencyButton;
    private Context mContext;
    private ResourceUtils mResUtils;
    private int mHitCount = 0;

    private XC_MethodReplacement setupAssistActionsHook = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            FrameLayout emergencyActionGroup = (FrameLayout) param.thisObject;
            mContext = emergencyActionGroup.getContext();
            mResUtils = ResourceUtils.getInstance(mContext);

            int button1Id = mContext.getResources().getIdentifier("action1", "id", PACKAGE_PHONE);
            int button2Id = mContext.getResources().getIdentifier("action2", "id", PACKAGE_PHONE);
            int button3Id = mContext.getResources().getIdentifier("action3", "id", PACKAGE_PHONE);

            int[] buttonIds = new int[] {button1Id, button2Id, button3Id};

            for (int i = 0; i < 3; i++) {
                Button button = (Button) emergencyActionGroup.findViewById(buttonIds[i]);

                if (button.getId() == button1Id) {
                    button.setVisibility(View.VISIBLE);
                    button.setText(mResUtils.getString(R.string.emergency_info));
                    button.setOnClickListener(EmergencyHooks.this);
                    mEmergencyButton = button;
                } else {
                    button.setVisibility(View.GONE);
                }
            }

            mHitCount = 0;

            return null;
        }
    };

    public void hook(ClassLoader classLoader) {
        Class classEmergencyActionGroup = XposedHelpers.findClass("com.android.phone.EmergencyActionGroup", classLoader);

        if (ConfigUtils.lockscreen().enable_emergency_info) {
            XposedHelpers.findAndHookMethod(classEmergencyActionGroup, "setupAssistActions", setupAssistActionsHook);
        }
    }

    @Override
    public void onClick(View v) {
        switch (mHitCount) {
            case 0:
                mEmergencyButton.getBackground().setColorFilter(mResUtils.getColor(R.color.md_red_500), PorterDuff.Mode.SRC_ATOP);
                break;
            default:
                mEmergencyButton.getBackground().clearColorFilter();
                mHitCount = -1;

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("tk.wasdennnoch.androidn_ify",
                        EmergencyInfoActivity.class.getName());

                try {
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    XposedHook.logE(TAG, "Unable to start activity " + intent.toString(), e);
                }
        }
        mHitCount++;
    }
}
