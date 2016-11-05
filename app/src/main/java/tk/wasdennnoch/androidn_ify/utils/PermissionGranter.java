/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tk.wasdennnoch.androidn_ify.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_OWN;
import static tk.wasdennnoch.androidn_ify.XposedHook.PACKAGE_SYSTEMUI;

public class PermissionGranter {
    public static final String TAG = "PermissionGranter";

    private static final String CLASS_PACKAGE_MANAGER_SERVICE = "com.android.server.pm.PackageManagerService";
    private static final String CLASS_PACKAGE_PARSER_PACKAGE = "android.content.pm.PackageParser.Package";

    private static final String PERM_BATTERY_STATS = "android.permission.BATTERY_STATS";
    private static final String PERM_MANAGE_USERS = "android.permission.MANAGE_USERS";

    private static Map<String, List<String>> mPerms;

    static {
        mPerms = new HashMap<>();

        List<String> systemUiPerms = new ArrayList<>();
        systemUiPerms.add(PERM_BATTERY_STATS);
        mPerms.put(PACKAGE_SYSTEMUI, systemUiPerms);

        List<String> ownPerms = new ArrayList<>();
        ownPerms.add(PERM_MANAGE_USERS);
        mPerms.put(PACKAGE_OWN, ownPerms);
    }

    public static void initAndroid(final ClassLoader classLoader) {
        try {
            final Class<?> pmServiceClass = XposedHelpers.findClass(CLASS_PACKAGE_MANAGER_SERVICE, classLoader);

            XposedHelpers.findAndHookMethod(pmServiceClass, "grantPermissionsLPw",
                    CLASS_PACKAGE_PARSER_PACKAGE, boolean.class, String.class, new XC_MethodHook() {
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            final String pkgName = (String) XposedHelpers.getObjectField(param.args[0], "packageName");

                            if (ConfigUtils.M && mPerms.containsKey(pkgName))
                                grantPermsMm(param, mPerms.get(pkgName));
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void grantPermsMm(XC_MethodHook.MethodHookParam param, List<String> neededPermissions) {
        final Object extras = XposedHelpers.getObjectField(param.args[0], "mExtras");
        final Object ps = XposedHelpers.callMethod(extras, "getPermissionsState");
        final Object settings = XposedHelpers.getObjectField(param.thisObject, "mSettings");
        final Object permissions = XposedHelpers.getObjectField(settings, "mPermissions");

        for (String perm : neededPermissions) {
            if (!(boolean) XposedHelpers.callMethod(ps, "hasInstallPermission", perm)) {
                final Object pAccessPerm = XposedHelpers.callMethod(permissions, "get",
                        perm);
                int ret = (int) XposedHelpers.callMethod(ps, "grantInstallPermission", pAccessPerm);
                XposedHook.logD(TAG, "Permission added: " + pAccessPerm + "; ret=" + ret);
            }
        }

    }
}