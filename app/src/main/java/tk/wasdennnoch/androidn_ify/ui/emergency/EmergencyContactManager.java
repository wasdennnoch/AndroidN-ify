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

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.ByteArrayInputStream;

/**
 * Provides methods to read name, phone number, photo, etc. from contacts.
 */
public class EmergencyContactManager {

    /**
     * Returns a {@link Contact} that contains all the relevant information of the contact indexed
     * by {@code @contactUri}.
     */
    public static Contact getContact(Context context, Uri contactUri) {
        String phoneNumber = null;
        String phoneType = null;
        String name = null;
        Bitmap photo = null;
        final Uri contactLookupUri =
                ContactsContract.Contacts.getLookupUri(context.getContentResolver(),
                        contactUri);
        Cursor cursor = context.getContentResolver().query(
                contactUri,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.LABEL,
                        ContactsContract.CommonDataKinds.Photo.PHOTO_ID},
                null, null, null);
        try {
            if (cursor != null && cursor.moveToNext()) {
                name = cursor.getString(0);
                phoneNumber = cursor.getString(1);
                phoneType = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.getResources(),
                        cursor.getInt(2),
                        cursor.getString(3)).toString();
                Long photoId = cursor.getLong(4);
                if (photoId != null && photoId > 0) {
                    Uri photoUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                            photoId);
                    Cursor cursor2 = context.getContentResolver().query(
                            photoUri,
                            new String[]{ContactsContract.Contacts.Photo.PHOTO},
                            null, null, null);
                    try {
                        if (cursor2 != null && cursor2.moveToNext()) {
                            byte[] data = cursor2.getBlob(0);
                            photo = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                        }
                    } finally {
                        if (cursor2 != null) {
                            cursor2.close();
                        }
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return new Contact(contactLookupUri, contactUri, name, phoneNumber, phoneType, photo);
    }

    /** Returns whether the contact uri is not null and corresponds to an existing contact. */
    public static boolean isValidEmergencyContact(Context context, Uri contactUri) {
        return contactUri != null && contactExists(context, contactUri);
    }

    private static boolean contactExists(Context context, Uri contactUri) {
        Cursor cursor = context.getContentResolver().query(contactUri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    /** Wrapper for a contact with a phone number. */
    public static class Contact {
        /** The lookup uri is necessary to display the contact. */
        private final Uri mContactLookupUri;
        /**
         * The contact uri is associated to a particular phone number and can be used to reload that
         * number and keep the number displayed in the preferences fresh.
         */
        private final Uri mContactUri;
        /** The display name of the contact. */
        private final String mName;
        /** The emergency contact's phone number selected by the user. */
        private final String mPhoneNumber;
        /** The emergency contact's phone number type (mobile, work, home, etc). */
        private final String mPhoneType;
        /** The contact's photo. */
        private final Bitmap mPhoto;

        /** Constructs a new contact. */
        public Contact(Uri contactLookupUri,
                       Uri contactUri,
                       String name,
                       String phoneNumber,
                       String phoneType,
                       Bitmap photo) {
            mContactLookupUri = contactLookupUri;
            mContactUri = contactUri;
            mName = name;
            mPhoneNumber = phoneNumber;
            mPhoneType = phoneType;
            mPhoto = photo;
        }

        /** Returns the contact's CONTENT_LOOKUP_URI. Use this to display the contact. */
        public Uri getContactLookupUri() {
            return mContactLookupUri;
        }

        /**
         * The contact uri as defined in ContactsContract.CommonDataKinds.Phone.CONTENT_URI. Use
         * this to reload the contact. This links to a particular phone number of the emergency
         * contact
         */
        public Uri getContactUri() {
            return mContactUri;
        }

        /** Returns the display name of the contact. */
        public String getName() {
            return mName;
        }

        /** Returns the phone number selected by the user. */
        public String getPhoneNumber() {
            return mPhoneNumber;
        }

        /** Returns the phone type (e.g. mobile, work, home, etc.) . */
        public String getPhoneType() {
            return mPhoneType;
        }

        /** Returns the photo assigned to this contact. */
        public Bitmap getPhoto() {
            return mPhoto;
        }
    }
}