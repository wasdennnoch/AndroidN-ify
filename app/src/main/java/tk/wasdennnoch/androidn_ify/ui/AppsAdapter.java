package tk.wasdennnoch.androidn_ify.ui;

import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.CachedResolveInfo;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.AppViewHolder> {
    List<CachedResolveInfo> mApps;
    List<String> mBlacklistedApps;
    PackageManager mPackageManager;
    AppsAdapterListener mListener;

    public AppsAdapter(List<CachedResolveInfo> apps, List<String> blacklistedApps, PackageManager pm, AppsAdapterListener listener) {
        mApps = apps;
        mBlacklistedApps = blacklistedApps;
        mPackageManager = pm;
        mListener = listener;
    }

    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_row, parent, false);
        return new AppViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final AppViewHolder holder, int position) {
        CachedResolveInfo app = mApps.get(position);
        holder.mAppName.setText(app.getLabel());
        holder.mPackageName.setText(app.getPackageName());
        holder.mAppIcon.setImageDrawable(app.getIcon());
        holder.mCheckBox.setChecked(mBlacklistedApps.contains(app.getPackageName()));
    }

    @Override
    public int getItemCount() {
        return mApps.size();
    }

    public void setApps(List<CachedResolveInfo> apps) {
        mApps = apps;
    }

    public List<CachedResolveInfo> getApps() {
        return mApps;
    }

    public void setBlacklistedApps(List<String> blacklistedApps) {
        mBlacklistedApps = blacklistedApps;
    }

    public class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView mAppName;
        public TextView mPackageName;
        public ImageView mAppIcon;
        public CheckBox mCheckBox;

        public AppViewHolder(View itemView) {
            super(itemView);

            mAppName = (TextView) itemView.findViewById(R.id.appName);
            mPackageName = (TextView) itemView.findViewById(R.id.packageName);
            mAppIcon = (ImageView) itemView.findViewById(R.id.appIcon);
            mCheckBox = (CheckBox) itemView.findViewById(R.id.check);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mListener.onSelect(getAdapterPosition(), this);
        }
    }

    public interface AppsAdapterListener {
        void onSelect(int adapterPosition, AppViewHolder itemView);
    }
}
