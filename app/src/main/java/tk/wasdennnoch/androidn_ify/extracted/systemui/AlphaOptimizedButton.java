package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.content.Context;
import android.widget.Button;

public class AlphaOptimizedButton extends Button {

    public AlphaOptimizedButton(Context context) {
        super(context);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

}
