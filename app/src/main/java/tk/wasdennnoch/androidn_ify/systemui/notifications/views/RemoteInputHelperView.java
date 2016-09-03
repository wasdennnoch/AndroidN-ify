package tk.wasdennnoch.androidn_ify.systemui.notifications.views;

import android.animation.Animator;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHooks;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

@SuppressWarnings("WeakerAccess")
public class RemoteInputHelperView extends FrameLayout implements View.OnClickListener, View.OnFocusChangeListener, TextView.OnEditorActionListener, TextWatcher, Animator.AnimatorListener {

    public static final boolean DIRECT_REPLY_ENABLED = false;

    private final LinearLayout remoteInputView;
    private final EditText remoteInputText;
    private final ImageButton sendButton;
    private final ProgressBar sendProgress;
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

        remoteInputText.setSingleLine(true);
        remoteInputText.setOnFocusChangeListener(this);
        remoteInputText.setOnEditorActionListener(this);
        remoteInputText.addTextChangedListener(this);
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

    public void show(View view, boolean animate) {
        showRemoteInput(view, animate);
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

    private void showRemoteInput(View view, boolean animate) {
        remoteInputView.setVisibility(VISIBLE);
        if (!animate) return;
        int cx = view.getLeft() + view.getWidth() / 2;
        int cy = view.getTop() + view.getHeight() / 2;
        int w = remoteInputView.getWidth();
        int h = remoteInputView.getHeight();
        int r = Math.max(
                Math.max(cx + cy, cx + (h - cy)),
                Math.max((w - cx) + cy, (w - cx) + (h - cy)));
        Animator animator = ViewAnimationUtils.createCircularReveal(remoteInputView, cx, cy, 0, r);
        animator.addListener(this);
        animator.start();
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
            sendRemoteInput();
        } else if (v == remoteInputText) {
            show(v, false);
        }
    }

    private void sendRemoteInput() {
        if (remoteInputText.getText().length() == 0) return;

        setWindowManagerFocus(false);
        remoteInputText.setFocusable(false);
        remoteInputText.setEnabled(false);
        sendButton.setVisibility(INVISIBLE);
        sendProgress.setVisibility(VISIBLE);

        Bundle resultData = new Bundle();
        resultData.putString(remoteInput.getResultKey(), remoteInputText.getText().toString());
        Intent result = new Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        RemoteInput.addResultsToIntent(remoteInputs, result, resultData);
        try {
            pendingIntent.send(getContext(), 0, result);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == remoteInputText) {
            setWindowManagerFocus(hasFocus);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

        if (event == null
                && (actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_SEND)) {
            if (remoteInputText.length() > 0) {
                sendRemoteInput();
            }
            return true;
        }
        return false;
    }

    private void updateSendButton() {
        sendButton.setEnabled(remoteInputText.getText().length() != 0);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        updateSendButton();
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        remoteInputText.requestFocus();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
}