package tk.wasdennnoch.androidn_ify.utils;

import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public class CachedResolveInfo {

    private CharSequence label;
    private Drawable icon;
    private int color;
    public ResolveInfo resolveInfo;

    private String labelSearch;
    private String packageNameSearch;

    public void setLabel(CharSequence label) {
        this.label = label;
        this.labelSearch = label.toString().toLowerCase();
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public CharSequence getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public int getColor() {
        return color;
    }

    public void setResolveInfo(ResolveInfo resolveInfo) {
        this.resolveInfo = resolveInfo;
        this.packageNameSearch = resolveInfo.activityInfo.packageName.toLowerCase();
    }

    public String getPackageName() {
        return resolveInfo.activityInfo.packageName;
    }

    public boolean search(String query) {
        return labelSearch.contains(query) || packageNameSearch.contains(query);
    }
}
