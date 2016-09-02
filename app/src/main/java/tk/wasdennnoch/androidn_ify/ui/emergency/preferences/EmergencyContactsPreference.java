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
package tk.wasdennnoch.androidn_ify.ui.emergency.preferences;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import tk.wasdennnoch.androidn_ify.ui.emergency.EmergencyContactManager;
import tk.wasdennnoch.androidn_ify.ui.emergency.ReloadablePreferenceInterface;

/**
 * Custom {@link PreferenceCategory} that deals with contacts being deleted from the contacts app.
 *
 * <p>Contacts are stored internally using their ContactsContract.CommonDataKinds.Phone.CONTENT_URI.
 */
public class EmergencyContactsPreference extends PreferenceCategory
        implements ReloadablePreferenceInterface,
        ContactPreference.RemoveContactPreferenceListener {
    private static final String CONTACT_SEPARATOR = "|";
    private static final String QUOTE_CONTACT_SEPARATOR = Pattern.quote(CONTACT_SEPARATOR);
    /** Stores the emergency contact's ContactsContract.CommonDataKinds.Phone.CONTENT_URI */
    private List<Uri> mEmergencyContacts = new ArrayList<Uri>();
    private boolean mEmergencyContactsSet = false;
    public EmergencyContactsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setEmergencyContacts(restorePersistedValue ?
                getPersistedEmergencyContacts() :
                deserializeAndFilter(getKey(),
                        getContext(),
                        (String) defaultValue));
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }
    @Override
    public void reloadFromPreference() {
        setEmergencyContacts(getPersistedEmergencyContacts());
    }
    @Override
    public boolean isNotSet() {
        return mEmergencyContacts.isEmpty();
    }
    @Override
    public void onRemoveContactPreference(ContactPreference contactPreference) {
        Uri newContact = contactPreference.getContactUri();
        if (mEmergencyContacts.contains(newContact)) {
            List<Uri> updatedContacts = new ArrayList<Uri>(mEmergencyContacts);
            if (updatedContacts.remove(newContact) && callChangeListener(updatedContacts)) {
                setEmergencyContacts(updatedContacts);
            }
        }
    }
    /**
     * Adds a new emergency contact. The {@code contactUri} is the
     * ContactsContract.CommonDataKinds.Phone.CONTENT_URI corresponding to the
     * contact's selected phone number.
     */
    public void addNewEmergencyContact(Uri contactUri) {
        if (!mEmergencyContacts.contains(contactUri)) {
            List<Uri> updatedContacts = new ArrayList<Uri>(mEmergencyContacts);
            if (updatedContacts.add(contactUri) && callChangeListener(updatedContacts)) {
                setEmergencyContacts(updatedContacts);
            }
        }
    }
    @VisibleForTesting
    public List<Uri> getEmergencyContacts() {
        return mEmergencyContacts;
    }
    public void setEmergencyContacts(List<Uri> emergencyContacts) {
        final boolean changed = !mEmergencyContacts.equals(emergencyContacts);
        if (changed || !mEmergencyContactsSet) {
            mEmergencyContacts = emergencyContacts;
            mEmergencyContactsSet = true;
            persistString(serialize(emergencyContacts));
            if (changed) {
                notifyChanged();
            }
        }
        while (getPreferenceCount() - emergencyContacts.size() > 0) {
            removePreference(getPreference(0));
        }
        // Reload the preferences or add new ones if necessary
        Iterator<Uri> it = emergencyContacts.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (i < getPreferenceCount()) {
                ContactPreference contactPreference = (ContactPreference) getPreference(i);
                contactPreference.setUri(it.next());
            } else {
                addContactPreference(it.next());
            }
            i++;
        }
    }
    private void addContactPreference(Uri contactUri) {
        final ContactPreference contactPreference = new ContactPreference(getContext(), contactUri);
        onBindContactView(contactPreference);
        addPreference(contactPreference);
    }
    /**
     * Called when {@code contactPreference} has been added to this category. You may now set
     * listeners.
     */
    protected void onBindContactView(final ContactPreference contactPreference) {
        contactPreference.setRemoveContactPreferenceListener(this);
        contactPreference
                .setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                contactPreference.displayContact();
                                return true;
                            }
                        }
                );
    }
    private List<Uri> getPersistedEmergencyContacts() {
        return deserializeAndFilter(getKey(), getContext(), getPersistedString(""));
    }
    @Override
    protected String getPersistedString(String defaultReturnValue) {
        try {
            return super.getPersistedString(defaultReturnValue);
        } catch (ClassCastException e) {
            // Protect against b/28194605: We used to store the contacts using a string set.
            // If it is a string set, ignore its value. If it is not a string set it will throw
            // a ClassCastException
            getPersistedStringSet(Collections.<String>emptySet());
            return defaultReturnValue;
        }
    }

    @SuppressLint("Override")
    public Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        return getPreferenceManager().getSharedPreferences().getStringSet(getKey(), defaultReturnValue);
    }
    /**
     * Converts the string representing the emergency contacts to a list of Uris and only keeps
     * those corresponding to still existing contacts. It persists the contacts if at least one
     * contact was does not exist anymore.
     */
    public static List<Uri> deserializeAndFilter(String key, Context context,
                                                 String emergencyContactString) {
        String[] emergencyContactsArray =
                emergencyContactString.split(QUOTE_CONTACT_SEPARATOR);
        List<Uri> filteredEmergencyContacts = new ArrayList<Uri>(emergencyContactsArray.length);
        for (String emergencyContact : emergencyContactsArray) {
            Uri contactUri = Uri.parse(emergencyContact);
            if (EmergencyContactManager.isValidEmergencyContact(context, contactUri)) {
                filteredEmergencyContacts.add(contactUri);
            }
        }
        // If not all contacts were added, then we need to overwrite the emergency contacts stored
        // in shared preferences. This deals with emergency contacts being deleted from contacts:
        // currently we have no way to being notified when this happens.
        if (filteredEmergencyContacts.size() != emergencyContactsArray.length) {
            String emergencyContactStrings = serialize(filteredEmergencyContacts);
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context);
            sharedPreferences.edit().putString(key, emergencyContactStrings).commit();
        }
        return filteredEmergencyContacts;
    }
    /** Converts the Uris to a string representation. */
    public static String serialize(List<Uri> emergencyContacts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < emergencyContacts.size(); i++) {
            sb.append(emergencyContacts.get(i).toString());
            sb.append(CONTACT_SEPARATOR);
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}