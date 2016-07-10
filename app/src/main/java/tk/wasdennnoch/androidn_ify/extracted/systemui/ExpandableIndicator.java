package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.widget.ImageView;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class ExpandableIndicator extends ImageView {

    private boolean mExpanded;
    private AnimatedVectorDrawable mExpandedDrawable;
    private AnimatedVectorDrawable mCollapsedDrawable;

    public ExpandableIndicator(Context context) {
        super(context);
        ResourceUtils res = ResourceUtils.getInstance(context);
        mExpandedDrawable = (AnimatedVectorDrawable) res.getDrawable(R.drawable.ic_volume_expand_animation)
                .getConstantState().newDrawable();
        mCollapsedDrawable = (AnimatedVectorDrawable) res.getDrawable(R.drawable.ic_volume_collapse_animation)
                .getConstantState().newDrawable();
    }

    public void setExpanded(boolean expanded) {
        if (expanded == mExpanded) return;
        mExpanded = expanded;

        AnimatedVectorDrawable drawable;
        if (mExpanded) {
            drawable = mExpandedDrawable;
        } else {
            drawable = mCollapsedDrawable;
        }
        setImageDrawable(drawable);
        drawable.start();
    }

    public boolean isExpanded() {
        return mExpanded;
    }

}
