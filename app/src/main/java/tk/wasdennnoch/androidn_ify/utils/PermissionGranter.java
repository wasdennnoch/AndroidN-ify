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
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

public class PermissionGranter {
    public static final String TAG = "GB:PermissionGranter";
    public static final boolean DEBUG = false;

    private static final String CLASS_PACKAGE_MANAGER_SERVICE = "com.android.server.pm.PackageManagerService";
    private static final String CLASS_PACKAGE_PARSER_PACKAGE = "android.content.pm.PackageParser.Package";

    private static final String PERM_BATTERY_STATS = "android.permission.BATTERY_STATS";

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
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

                            // SystemUI
                            if (pkgName.equals(XposedHook.PACKAGE_SYSTEMUI)) {
                                List<String> neededPermissions = new ArrayList<>();
                                // Add android.permission.BATTERY_STATS needed by battery tile
                                neededPermissions.add(PERM_BATTERY_STATS);
                                if (ConfigUtils.M)
                                    grantPermsMm(param, pkgName, neededPermissions);
                                else
                                    grantPermsLp(param, pkgName, neededPermissions);
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static void grantPermsMm(XC_MethodHook.MethodHookParam param, String pkgName, List<String> neededPermissions) {
        final Object extras = XposedHelpers.getObjectField(param.args[0], "mExtras");
        final Object ps = XposedHelpers.callMethod(extras, "getPermissionsState");
        final List<String> grantedPerms =
                (List<String>) XposedHelpers.getObjectField(param.args[0], "requestedPermissions");
        final Object settings = XposedHelpers.getObjectField(param.thisObject, "mSettings");
        final Object permissions = XposedHelpers.getObjectField(settings, "mPermissions");

        for (String perm : neededPermissions) {
            if (!(boolean)XposedHelpers.callMethod(ps,"hasInstallPermission", perm)) {
                final Object pAccessPerm = XposedHelpers.callMethod(permissions, "get",
                        perm);
                int ret = (int) XposedHelpers.callMethod(ps, "grantInstallPermission", pAccessPerm);
                if (DEBUG) log("Permission added: " + pAccessPerm + "; ret=" + ret);
            }
        }


        if (DEBUG) {
            log("List of permissions: ");
            for (Object perm : grantedPerms) {
                log(pkgName + ": " + perm);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void grantPermsLp(XC_MethodHook.MethodHookParam param, String pkgName, List<String> neededPermissions) {
        final Object extras = XposedHelpers.getObjectField(param.args[0], "mExtras");
        final Object sharedUser = XposedHelpers.getObjectField(extras, "sharedUser");
        final Set<String> grantedPerms =
                (Set<String>) XposedHelpers.getObjectField(extras, "grantedPermissions");
        final Object settings = XposedHelpers.getObjectField(param.thisObject, "mSettings");
        final Object permissions = XposedHelpers.getObjectField(settings, "mPermissions");

        for (String perm : neededPermissions) {
            if (!grantedPerms.contains(perm)) {
                final Object pCns = XposedHelpers.callMethod(permissions, "get",
                        perm);
                grantedPerms.add(perm);
                int[] gpGids = (int[]) XposedHelpers.getObjectField(sharedUser, "gids");
                int[] bpGids = (int[]) XposedHelpers.getObjectField(pCns, "gids");
                gpGids = (int[]) XposedHelpers.callStaticMethod(param.thisObject.getClass(),
                        "appendInts", gpGids, bpGids);

                if (DEBUG) log(pkgName + ": Permission added: " + pCns);
            }
        }

        if (DEBUG) {
            log("List of permissions: ");
            for (String perm : grantedPerms) {
                log(pkgName + ": " + perm);
            }
        }
    }
}