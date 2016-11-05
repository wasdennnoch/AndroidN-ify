package tk.wasdennnoch.androidn_ify;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class PreferenceProvider extends RemotePreferenceProvider {

    public PreferenceProvider() {
        super("tk.wasdennnoch.androidn_ify.PREFERENCES", new String[]{"tk.wasdennnoch.androidn_ify_preferences"});
    }

}
