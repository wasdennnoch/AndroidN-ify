/*
 * Copyright (C) 2016 The Android Open Source Project
 *
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
package tk.wasdennnoch.androidn_ify.ui.emergency;
/**
 * Provides common methods to be called when reloading a preference.
 */
public interface ReloadablePreferenceInterface {
    /** Reloads the value from the preference and updates the summary with that value. */
    void reloadFromPreference();
    /**
     * Returns whether the persisted string is empty or set to the default value, i.e. the user
     * didn't set it.
     */
    boolean isNotSet();
}