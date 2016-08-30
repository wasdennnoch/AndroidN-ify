package tk.wasdennnoch.androidn_ify.systemui.notifications.views;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import tk.wasdennnoch.androidn_ify.BuildConfig;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class RemoteInputHelperView extends RelativeLayout implements View.OnClickListener, TextView.OnEditorActionListener {

    private RelativeLayout remoteInputView;
    private EditText remoteInputText;
    private ImageButton sendButton;
    private ProgressBar sendProgress;
    private PendingIntent pendingIntent;
    private RemoteInput[] remoteInputs;
    private RemoteInput remoteInput;
    //private ViewGroup scrollContainer = null;

    private RemoteInputHelperView(Context context) {
        super(context);
        try {
            Resources res = getContext().getPackageManager().getResourcesForApplication(BuildConfig.APPLICATION_ID);
            XmlPullParser layout = res.getLayout(res.getIdentifier("reply_layout", "layout", BuildConfig.APPLICATION_ID));
            LayoutInflater.from(getContext().createPackageContext(BuildConfig.APPLICATION_ID, 
Context.CONTEXT_IGNORE_SECURITY)).inflate(layout, this, true);
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
        remoteInputView = (RelativeLayout) findViewById(android.R.id.custom);
        remoteInputText = (EditText) findViewById(android.R.id.input);
        remoteInputText.setOnEditorActionListener(this);
        sendButton = (ImageButton) findViewById(android.R.id.button1);
        sendProgress = (ProgressBar) findViewById(android.R.id.progress);

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
        remoteInputText.setCursorVisible(true);
        unblockFocus();
        remoteInputText.requestFocus();
        showKeyboard();
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) callStaticMethod(InputMethodManager.class, "getInstance");
        try {
            WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) getObjectField(callMethod(this, "getViewRootImpl"), 
"mWindowAttributes");

            callMethod(imm, "onPreWindowFocus", getRootView(), true);
            callMethod(imm, "onPostWindowFocus", getRootView(), this, attrs.softInputMode, false, attrs.flags);

            imm.showSoftInput(this, 0, null);
        } catch (Throwable t) {
            log(t);
        }
    }

    private void unblockFocus() {
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
    }


    @Override
    public void onClick(View v) {
        if (v == sendButton) {
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
            showKeyboard();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        switch (actionId) {
            case EditorInfo.IME_ACTION_SEND:
                onClick(sendButton);
                return true;
        }
        return false;
    }

    /*
    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionevent) {
        if (motionevent.getAction() == MotionEvent.ACTION_DOWN) {
            findScrollContainer();
            if (scrollContainer != null) {
                scrollContainer.requestDisallowInterceptTouchEvent(true);
                callMethod(scrollContainer, "removeLongPressCallback");
            }
        }
        return super.onInterceptTouchEvent(motionevent);
    }

    private void findScrollContainer() {
        if (scrollContainer != null) {
            return;
        }
        ViewParent current = getParent();
        for (int depth = 0; depth < 12; depth++) {
            if (current.getClass().getName().contains("NotificationStackScrollLayout")) {
                scrollContainer = (ViewGroup) current.getParent();
                return;
            }
            current = current.getParent();
        }
    }//*/
}
