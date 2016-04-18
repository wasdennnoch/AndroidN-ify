package tk.wasdennnoch.androidn_ify.extracted;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.widget.ImageView;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

public class ExpandableIndicator extends ImageView {

    private boolean mExpanded;

    public ExpandableIndicator(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int i;
        if (mExpanded) { // TODO module resources?
            i = R.drawable.ic_volume_collapse_animation; // 1
        } else {
            i = R.drawable.ic_volume_expand_animation; // 2
        }
        setImageResource(i);
    }

    public void setExpanded(boolean expanded) {
        if (expanded == mExpanded) {
            return;
        }
        mExpanded = expanded;
        AnimatedVectorDrawable drawable;
        int i;
        if (mExpanded) {
            i = R.drawable.ic_volume_expand_animation; // 2
        } else {
            i = R.drawable.ic_volume_collapse_animation; // 1
        }
        //noinspection ConstantConditions
        drawable = (AnimatedVectorDrawable) ResourceUtils.getInstance(getContext()).getDrawable(i).getConstantState().newDrawable();
        setImageDrawable(drawable);
        drawable.start();
    }
}
