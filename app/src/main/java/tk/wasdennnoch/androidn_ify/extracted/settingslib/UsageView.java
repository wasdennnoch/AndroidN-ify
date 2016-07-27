package tk.wasdennnoch.androidn_ify.extracted.settingslib;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class UsageView extends FrameLayout {

    /*private final TextView[] mBottomLabels;
    private final TextView[] mLabels;
    private final UsageGraph mUsageGraph;*/

    public UsageView(final Context context, final AttributeSet set) {
        super(context, set);
        final int gravity = 8388613;
        final int n = 2;
        final int n2 = 1;
        /*LayoutInflater.from(context).inflate(R.layout.usage_view, (ViewGroup) this);
        this.mUsageGraph = (UsageGraph) this.findViewById(R.id.usage_graph);
        final TextView[] mLabels = {(TextView) this.findViewById(R.id.label_bottom), null, null};
        mLabels[n2] = (TextView) this.findViewById(R.id.label_middle);
        mLabels[n] = (TextView) this.findViewById(R.id.label_top);
        this.mLabels = mLabels;
        final TextView[] mBottomLabels = new TextView[n];
        mBottomLabels[0] = (TextView) this.findViewById(R.id.label_start);
        mBottomLabels[n2] = (TextView) this.findViewById(R.id.label_end);
        this.mBottomLabels = mBottomLabels;
        final TypedArray obtainStyledAttributes = context.obtainStyledAttributes(set, R$styleable.UsageView, 0, 0);
        if (obtainStyledAttributes.hasValue(R$styleable.UsageView_sideLabels)) {
            this.setSideLabels(obtainStyledAttributes.getTextArray(R$styleable.UsageView_sideLabels));
        }
        if (obtainStyledAttributes.hasValue(R$styleable.UsageView_bottomLabels)) {
            this.setBottomLabels(obtainStyledAttributes.getTextArray(R$styleable.UsageView_bottomLabels));
        }
        if (obtainStyledAttributes.hasValue(R$styleable.UsageView_textColor)) {
            final int color = obtainStyledAttributes.getColor(R$styleable.UsageView_textColor, 0);
            final TextView[] mLabels2 = this.mLabels;
            for (int length = mLabels2.length, i = 0; i < length; ++i) {
                mLabels2[i].setTextColor(color);
            }
            final TextView[] mBottomLabels2 = this.mBottomLabels;
            for (int length2 = mBottomLabels2.length, j = 0; j < length2; ++j) {
                mBottomLabels2[j].setTextColor(color);
            }
        }
        if (obtainStyledAttributes.hasValue(R$styleable.UsageView_android_gravity)) {
            final int int1 = obtainStyledAttributes.getInt(R$styleable.UsageView_android_gravity, 0);
            if (int1 == gravity) {
                final LinearLayout linearLayout = (LinearLayout) this.findViewById(R$id.graph_label_group);
                final LinearLayout linearLayout2 = (LinearLayout) this.findViewById(R$id.label_group);
                linearLayout.removeView((View) linearLayout2);
                linearLayout.addView((View) linearLayout2);
                linearLayout2.setGravity(gravity);
                final LinearLayout linearLayout3 = (LinearLayout) this.findViewById(R$id.bottom_label_group);
                linearLayout3.setPadding(linearLayout3.getPaddingRight(), linearLayout3.getPaddingTop(), linearLayout3.getPaddingLeft(), linearLayout3.getPaddingBottom());
            } else if (int1 != 8388611) {
                throw new IllegalArgumentException("Unsupported gravity " + int1);
            }
        }
        this.mUsageGraph.setAccentColor(obtainStyledAttributes.getColor(R$styleable.UsageView_android_colorAccent, 0));
        obtainStyledAttributes.recycle();*/
    }

    /*public void addPath(final SparseIntArray sparseIntArray) {
        this.mUsageGraph.addPath(sparseIntArray);
    }

    public void clearPaths() {
        this.mUsageGraph.clearPaths();
    }

    public void configureGraph(final int n, final int n2, final boolean b, final boolean b2) {
        this.mUsageGraph.setMax(n, n2);
        this.mUsageGraph.setShowProjection(b, b2);
    }

    public void setBottomLabels(final CharSequence[] array) {
        if (array.length != this.mBottomLabels.length) {
            throw new IllegalArgumentException("Invalid number of labels");
        }
        for (int i = 0; i < this.mBottomLabels.length; ++i) {
            this.mBottomLabels[i].setText(array[i]);
        }
    }

    public void setSideLabels(final CharSequence[] array) {
        if (array.length != this.mLabels.length) {
            throw new IllegalArgumentException("Invalid number of labels");
        }
        for (int i = 0; i < this.mLabels.length; ++i) {
            this.mLabels[i].setText(array[i]);
        }
    }*/
}
