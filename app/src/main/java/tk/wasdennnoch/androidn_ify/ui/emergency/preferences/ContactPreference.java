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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.view.View;

import com.android.internal.annotations.VisibleForTesting;

import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.extracted.settingslib.CircleFramedDrawable;
import tk.wasdennnoch.androidn_ify.ui.emergency.EmergencyContactManager;


/**
 * A {@link Preference} to display or call a contact using the specified URI string.
 */
public class ContactPreference extends Preference {

    private EmergencyContactManager.Contact mContact;
    @Nullable
    private RemoveContactPreferenceListener mRemoveContactPreferenceListener;
    @Nullable
    private AlertDialog mRemoveContactDialog;

    /**
     * Listener for removing a contact.
     */
    public interface RemoveContactPreferenceListener {
        /**
         * Callback to remove a contact preference.
         */
        void onRemoveContactPreference(ContactPreference preference);
    }

    /**
     * Instantiates a ContactPreference that displays an emergency contact, taking in a Context and
     * the Uri.
     */
    public ContactPreference(Context context, @NonNull Uri contactUri) {
        super(context);
        setOrder(DEFAULT_ORDER);

        setUri(contactUri);

        setWidgetLayoutResource(R.layout.preference_user_delete_widget);
        setPersistent(false);
    }

    @SuppressWarnings("deprecation")
    public void setUri(@NonNull Uri contactUri) {
        if (mContact != null && !contactUri.equals(mContact.getContactUri()) &&
                mRemoveContactDialog != null) {
            mRemoveContactDialog.dismiss();
        }

        mContact = EmergencyContactManager.getContact(getContext(), contactUri);

        setTitle(mContact.getName());
        setKey(mContact.getContactUri().toString());
        String summary = mContact.getPhoneType() == null ?
                mContact.getPhoneNumber() :
                String.format(
                        getContext().getResources().getString(R.string.phone_type_and_phone_number),
                        mContact.getPhoneType(),
                        BidiFormatter.getInstance().unicodeWrap(mContact.getPhoneNumber(),
                                TextDirectionHeuristics.LTR));
        setSummary(summary);

        // Update the message to show the correct name.
        if (mRemoveContactDialog != null) {
            mRemoveContactDialog.setMessage(
                    String.format(getContext().getString(R.string.remove_contact),
                            mContact.getName()));
        }

        //TODO: Consider doing the following in a non-UI thread.
        Drawable icon;
        if (mContact.getPhoto() != null) {
            icon = new CircleFramedDrawable(mContact.getPhoto(),
                    (int) getContext().getResources().getDimension(R.dimen.circle_avatar_size));
        } else {
            icon = getContext().getResources().getDrawable(R.drawable.ic_person_black_24dp);
        }
        setIcon(icon);
    }

    /** Listener to be informed when a contact preference should be deleted. */
    public void setRemoveContactPreferenceListener(
            RemoveContactPreferenceListener removeContactListener) {
        mRemoveContactPreferenceListener = removeContactListener;
        if (mRemoveContactPreferenceListener == null) {
            mRemoveContactDialog = null;
            return;
        }
        if (mRemoveContactDialog != null) {
            return;
        }
        // Create the remove contact dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setNegativeButton(getContext().getString(R.string.cancel), null);
        builder.setPositiveButton(getContext().getString(R.string.remove),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                                        int which) {
                        if (mRemoveContactPreferenceListener != null) {
                            mRemoveContactPreferenceListener
                                    .onRemoveContactPreference(ContactPreference.this);
                        }
                    }
                });
        builder.setMessage(String.format(getContext().getString(R.string.remove_contact),
                mContact.getName()));
        mRemoveContactDialog = builder.create();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View deleteContactIcon = view.findViewById(R.id.delete_contact);
        if (mRemoveContactPreferenceListener == null) {
            deleteContactIcon.setVisibility(View.GONE);
        } else {
            deleteContactIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showRemoveContactDialog(null);
                }
            });

        }
    }

    public Uri getContactUri() {
        return mContact.getContactUri();
    }

    /**
     * Calls the contact.
     */
    public void callContact() {
        Intent callIntent =
                new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mContact.getPhoneNumber()));
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        getContext().startActivity(callIntent);
    }

    /**
     * Displays a contact card for the contact.
     */
    public void displayContact() {
        Intent contactIntent = new Intent(Intent.ACTION_VIEW);
        contactIntent.setData(mContact.getContactLookupUri());
        getContext().startActivity(contactIntent);
    }

    /** Shows the dialog to remove the contact, restoring it from {@code state} if it's not null. */
    private void showRemoveContactDialog(Bundle state) {
        if (mRemoveContactDialog == null) {
            return;
        }
        if (state != null) {
            mRemoveContactDialog.onRestoreInstanceState(state);
        }
        mRemoveContactDialog.show();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mRemoveContactDialog == null || !mRemoveContactDialog.isShowing()) {
            return superState;
        }
        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = true;
        myState.dialogBundle = mRemoveContactDialog.onSaveInstanceState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (myState.isDialogShowing) {
            showRemoveContactDialog(myState.dialogBundle);
        }
    }

    private static class SavedState extends BaseSavedState {
        boolean isDialogShowing;
        Bundle dialogBundle;

        @SuppressLint("ParcelClassLoader")
        public SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeBundle(dialogBundle);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}