package tk.wasdennnoch.androidn_ify.settings.misc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

public class DashboardOverlay extends View {

    private Drawable mDivider;
    private LinearLayout mLayout;

    public DashboardOverlay(Context context) {
        super(context);
        setWillNotDraw(false);
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.listDivider, value, true);
        mDivider = context.getDrawable(value.resourceId);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        final int childCount = mLayout.getChildCount();
        for (int i = 1; i < childCount; i++) {
            final View child = mLayout.getChildAt(i);

            int top = getChildTop(child);
            mDivider.setBounds(child.getLeft(), top, child.getRight(),
                    top + mDivider.getIntrinsicHeight());
            mDivider.draw(c);
        }
    }

    private int getChildTop(View child) {
        final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child
                .getLayoutParams();
        return child.getTop() + params.topMargin + Math.round(ViewCompat.getTranslationY(child));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mLayout != null) {
            setMeasuredDimension(mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
            invalidate();
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void setDashboardLayout(LinearLayout dashboardLayout) {
        mLayout = dashboardLayout;
    }
}
