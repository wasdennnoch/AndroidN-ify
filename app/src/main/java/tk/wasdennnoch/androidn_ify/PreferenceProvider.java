package tk.wasdennnoch.androidn_ify;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class PreferenceProvider extends RemotePreferenceProvider {

    public static final String AUTHORITY = "tk.wasdennnoch.androidn_ify.PREFERENCES";
    public static final String PREF_NAME = "tk.wasdennnoch.androidn_ify_preferences";

    public PreferenceProvider() {
        super(AUTHORITY, new String[]{PREF_NAME});
    }

}
