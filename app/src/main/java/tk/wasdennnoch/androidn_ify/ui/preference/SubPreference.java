package tk.wasdennnoch.androidn_ify.ui.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;

import tk.wasdennnoch.androidn_ify.R;

public class SubPreference extends Preference {

    private int mContent;

    public SubPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SubPreference);
        for (int i = a.getIndexCount() - 1; i >= 0; i--) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.SubPreference_content:
                    mContent = a.getResourceId(attr, 0);
                    break;
            }
        }
        a.recycle();
        setFragment("");
    }

    public int getContent() {
        return mContent;
    }
}
