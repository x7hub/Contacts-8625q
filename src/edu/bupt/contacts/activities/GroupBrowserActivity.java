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

package edu.bupt.contacts.activities;

import edu.bupt.contacts.ContactsActivity;
import edu.bupt.contacts.R;

import android.content.Intent;
import android.os.Bundle;

/**
 * 北邮ANT实验室
 * zzz
 * 
 * 此文件取自codeaurora提供的适用于高通8625Q的android 4.1.2源码，未修改
 * 
 * */

/**
 * Displays a list to browse groups.
 */
public class GroupBrowserActivity extends ContactsActivity {

    private static final String TAG = "GroupBrowserActivity";

    public GroupBrowserActivity() {
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        configureContentView(true, savedState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        configureContentView(false, null);
    }

    private void configureContentView(boolean createContentView, Bundle savedState) {
        // TODO: Create Intent Resolver to handle the different ways users can get to this list.
        // TODO: Use savedState if necessary
        // TODO: Setup action bar
        if (createContentView) {
            setContentView(R.layout.group_browser_activity);
        }
    }
}
