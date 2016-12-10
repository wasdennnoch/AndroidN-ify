package tk.wasdennnoch.androidn_ify.utils;

import android.content.Context;
import android.view.RemotableViewMethod;
import android.widget.LinearLayout;

public class RemoteMarginLinearLayout extends LinearLayout {

    public RemoteMarginLinearLayout(Context context) {
        super(context);
    }

    @RemotableViewMethod
    public void setMarginEnd(int margin) {
        ResourceUtils res = ResourceUtils.getInstance(getContext());
        ViewUtils.setMarginEnd(this, res.getDimensionPixelSize(margin));
    }
}
