/*
 * Copyright (C) 2011 The Android Open Source Project
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
package edu.bupt.contacts.list;

import edu.bupt.contacts.ContactsUtils;
import edu.bupt.contacts.list.ContactTileAdapter.ContactEntry;
import edu.bupt.contacts.util.ViewUtil;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

/**
 * 北邮ANT实验室
 * zzz
 * 
 * 此文件取自codeaurora提供的适用于高通8625Q的android 4.1.2源码，未修改
 * 
 * */

/**
 * A dark version of the {@link ContactTileView} that is used in Dialtacts
 * for frequently called contacts. Slightly different behavior from superclass...
 * when you tap it, you want to call the frequently-called number for the
 * contact, even if that is not the default number for that contact.
 */
public class ContactTilePhoneFrequentView extends ContactTileView {
    private String mPhoneNumberString;

    public ContactTilePhoneFrequentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean isDarkTheme() {
        return true;
    }

    @Override
    protected int getApproximateImageSize() {
        return ViewUtil.getConstantPreLayoutWidth(getQuickContact());
    }

    @Override
    public void loadFromContact(ContactEntry entry) {
        super.loadFromContact(entry);
        mPhoneNumberString = null; // ... in case we're reusing the view
        if (entry != null) {
            // Grab the phone-number to call directly... see {@link onClick()}
            mPhoneNumberString = entry.phoneNumber;
        }
    }

    @Override
    protected OnClickListener createClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener == null) return;
                if (TextUtils.isEmpty(mPhoneNumberString)) {
                    // Copy "superclass" implementation
                    mListener.onContactSelected(getLookupUri(), ContactsUtils.getTargetRectFromView(
                            mContext, ContactTilePhoneFrequentView.this));
                } else {
                    // When you tap a frequently-called contact, you want to
                    // call them at the number that you usually talk to them
                    // at (i.e. the one displayed in the UI), regardless of
                    // whether that's their default number.
                    mListener.onCallNumberDirectly(mPhoneNumberString);
                }
            }
        };
    }
}
