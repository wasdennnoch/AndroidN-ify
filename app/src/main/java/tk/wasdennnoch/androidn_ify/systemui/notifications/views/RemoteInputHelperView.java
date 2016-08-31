package tk.wasdennnoch.androidn_ify.systemui.notifications.views;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHooks;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class RemoteInputHelperView extends FrameLayout implements View.OnClickListener, View.OnFocusChangeListener {

    private LinearLayout remoteInputView;
    private EditText remoteInputText;
    private ImageButton sendButton;
    private ProgressBar sendProgress;
    private PendingIntent pendingIntent;
    private RemoteInput[] remoteInputs;
    private RemoteInput remoteInput;

    private ViewGroup mScrollContainer = null;
    private View mScrollContainerChild = null;

    private RemoteInputHelperView(Context context) {
        super(context);
        LayoutInflater.from(ResourceUtils.createOwnContext(getContext())).inflate(ResourceUtils.getInstance(getContext()).getLayout(R.layout.reply_layout), this, true);
        remoteInputView = (LinearLayout) findViewById(android.R.id.custom);
        remoteInputText = (EditText) findViewById(android.R.id.input);
        sendButton = (ImageButton) findViewById(android.R.id.button1);
        sendProgress = (ProgressBar) findViewById(android.R.id.progress);

        remoteInputText.setOnFocusChangeListener(this);
        remoteInputText.setOnClickListener(this);
        sendButton.setOnClickListener(this);
    }

    public static RemoteInputHelperView newInstance(Context c, PendingIntent pi, int backgroundColor) {
        RemoteInputHelperView v = new RemoteInputHelperView(c);
        v.pendingIntent = pi;
        v.remoteInputView.setBackgroundColor(backgroundColor);
        return v;
    }

    public void setRemoteInput(RemoteInput[] ris, RemoteInput ri) {
        remoteInputs = ris;
        remoteInput = ri;
        remoteInputText.setHint(remoteInput.getLabel());
    }

    public void show() {
        remoteInputView.setVisibility(VISIBLE);
        setWindowManagerFocus(true);
        remoteInputText.setFocusableInTouchMode(true);
        remoteInputText.setFocusable(true);
        remoteInputText.setSelection(remoteInputText.getText().length());
        // Unblock focus
        ViewParent ancestor = getParent();
        while (ancestor instanceof ViewGroup) {
            final ViewGroup vgAncestor = (ViewGroup) ancestor;
            if (vgAncestor.getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
                vgAncestor.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
                return;
            } else {
                ancestor = vgAncestor.getParent();
            }
        }
        remoteInputText.requestFocus();
        remoteInputText.setCursorVisible(true);

        // Handle IME
        InputMethodManager imm = (InputMethodManager) callStaticMethod(InputMethodManager.class, "getInstance");
        try {
            WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) getObjectField(callMethod(this, "getViewRootImpl"), "mWindowAttributes");
            callMethod(imm, "onPreWindowFocus", getRootView(), true);
            callMethod(imm, "onPostWindowFocus", getRootView(), this, attrs.softInputMode, false, attrs.flags);

            imm.showSoftInput(this, 0, null);
        } catch (Throwable t) {
            log(t);
        }
    }

    public boolean requestScrollTo() {
        findScrollContainer();
        callMethod(mScrollContainer, "lockScrollTo", mScrollContainerChild);
        return true;
    }

    private void findScrollContainer() {
        if (mScrollContainer == null) {
            mScrollContainerChild = null;
            ViewParent p = this;
            while (p != null) {
                if (mScrollContainerChild == null && findClass("ExpandableView", getClass().getClassLoader()).isInstance(p)) {
                    mScrollContainerChild = (View) p;
                }
                if (findClass("ScrollContainer", getClass().getClassLoader()).isInstance(p.getParent())) {
                    mScrollContainer = (ViewGroup) p.getParent();
                    if (mScrollContainerChild == null) {
                        mScrollContainerChild = (View) p;
                    }
                    break;
                }
                p = p.getParent();
            }
        }
    }

    public void setWindowManagerFocus(boolean focus) {
        NotificationHooks.remoteInputActive = focus;
        if (NotificationHooks.statusBarWindowManager != null)
            callMethod(NotificationHooks.statusBarWindowManager, "apply", getObjectField(NotificationHooks.statusBarWindowManager, "mCurrentState"));
    }

    @Override
    public void onClick(View v) {
        if (v == sendButton) {
            setWindowManagerFocus(false);
            remoteInputText.setFocusable(false);
            remoteInputText.setEnabled(false);
            sendButton.setVisibility(INVISIBLE);
            sendProgress.setVisibility(VISIBLE);

            Bundle resultData = new Bundle();
            resultData.putString(remoteInput.getResultKey(), remoteInputText.getText().toString());
            Intent result = new Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            RemoteInput.addResultsToIntent(remoteInputs, result, resultData);
            try {
                pendingIntent.send(getContext(), 0, result);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        } else if (v == remoteInputText) {
            show();
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == remoteInputText) {
            setWindowManagerFocus(hasFocus);
        }
    }
}