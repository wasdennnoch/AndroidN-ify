package tk.wasdennnoch.androidn_ify.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.widget.TextView;

public class RemoteLpTextView extends TextView {

    public RemoteLpTextView(Context context) {
        super(context);
    }

    public RemoteLpTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RemoteLpTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RemoteLpTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @RemotableViewMethod
    public void setWidth(int width) {
        ViewUtils.setWidth(this, width);
    }

    @RemotableViewMethod
    public void setHeight(int height) {
        ViewUtils.setHeight(this, height);
    }
}
