/*
 * Copyright (C) 2010 The Android Open Source Project
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

package edu.bupt.contacts.interactions;

import edu.bupt.contacts.R;
import edu.bupt.contacts.activities.MultiSelectExport;
import edu.bupt.contacts.editor.SelectAccountDialogFragment;
import edu.bupt.contacts.model.AccountTypeManager;
import edu.bupt.contacts.model.AccountWithDataSet;
import edu.bupt.contacts.msim.MultiSimConfig;
import edu.bupt.contacts.util.AccountSelectionUtil;
import edu.bupt.contacts.util.AccountsListAdapter.AccountListFilter;
import edu.bupt.contacts.vcard.ExportVCardActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;

import java.util.List;

/**
 * 北邮ANT实验室
 * ddd
 * 实现联系人的导入导出
 * 
 * 此文件取自codeaurora提供的适用于高通8625Q的android 4.1.2源码，有修改
 * 
 * */


/**
 * An dialog invoked to import/export contacts.
 */
public class ImportExportDialogFragment extends DialogFragment
        implements SelectAccountDialogFragment.Listener {
    public static final String TAG = "ImportExportDialogFragment";

    private static final String KEY_RES_ID = "resourceId";
    private static final String ARG_CONTACTS_ARE_AVAILABLE = "CONTACTS_ARE_AVAILABLE";
    private static int SIM_ID_INVALID = -1;
    private static int mSelectedSim = SIM_ID_INVALID;

    private final String[] LOOKUP_PROJECTION = new String[] {
            Contacts.LOOKUP_KEY
    };

    /** Preferred way to show this dialog */
    public static void show(FragmentManager fragmentManager, boolean contactsAreAvailable) {
        final ImportExportDialogFragment fragment = new ImportExportDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_CONTACTS_ARE_AVAILABLE, contactsAreAvailable);
        fragment.setArguments(args);
        fragment.show(fragmentManager, ImportExportDialogFragment.TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Wrap our context to inflate list items using the correct theme
        final Resources res = getActivity().getResources();
        final LayoutInflater dialogInflater = (LayoutInflater)getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final boolean contactsAreAvailable = getArguments().getBoolean(ARG_CONTACTS_ARE_AVAILABLE);

        // Adapter that shows a list of string resources
        final ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(getActivity(),
                R.layout.select_dialog_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final TextView result = (TextView)(convertView != null ? convertView :
                        dialogInflater.inflate(R.layout.select_dialog_item, parent, false));

                final int resId = getItem(position);
                result.setText(resId);
                return result;
            }
        };

        boolean hasIccCard = false;

        if (MultiSimConfig.isMultiSimEnabled()) {
            for (int i = 0; i < MSimTelephonyManager.getDefault().getPhoneCount(); i++) {
                hasIccCard = MSimTelephonyManager.getDefault().hasIccCard(i);
                if (hasIccCard) {
                    break;
                }
            }
        } else {
            hasIccCard = TelephonyManager.getDefault().hasIccCard();
        }
//        boolean hasIccCard = true;
        if (hasIccCard
                && res.getBoolean(R.bool.config_allow_sim_import)) {
            /** zzz */
            // baoge
//            adapter.add(R.string.manage_sim_contacts);
//            adapter.add(R.string.export_to_sim);
        }
        if (res.getBoolean(R.bool.config_allow_import_from_sdcard)) {
            adapter.add(R.string.import_from_sdcard);
        }
        if (res.getBoolean(R.bool.config_allow_export_to_sdcard)) {
            if (contactsAreAvailable) {
                adapter.add(R.string.export_to_sdcard);
            }
        }

        /** zzz */
//        if (res.getBoolean(R.bool.config_allow_share_visible_contacts)) {
//            if (contactsAreAvailable) {
//                adapter.add(R.string.share_visible_contacts);
//            }
//        }
        /** zzz */
        adapter.add(R.string.manage_sim_contacts);

        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean dismissDialog;
                final int resId = adapter.getItem(which);
                switch (resId) {
                    // baoge
                    case R.string.manage_sim_contacts:{//批量分享联系人
                        dismissDialog = true;
                        Intent exportIntent = new Intent(getActivity(), MultiSelectExport.class);
                        getActivity().startActivity(exportIntent);
                        break;
                    }
                    case R.string.import_from_sdcard: {//从存储设备导入
                        dismissDialog = handleImportRequest(resId);
                        break;
                    }
                    case R.string.export_to_sdcard: {//导出到存储设备
                        dismissDialog = true;
                        Intent exportIntent = new Intent(getActivity(), ExportVCardActivity.class);
                        getActivity().startActivity(exportIntent);
                        break;
                    }

                    /** zzz */
//                    case R.string.share_visible_contacts: 
//                        dismissDialog = true;
//                        doShareVisibleContacts();
//                        break;
//                    }

                    case R.string.export_to_sim: {//导出到sim卡
                        dismissDialog = true;
                        if (MultiSimConfig.isMultiSimEnabled()) {
                            displaySIMSelection();
                        } else {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setClassName("com.android.phone",
                                "com.android.phone.ExportContactsToSim");
                            startActivity(intent);
                        }
                        break;
                    }
//                    case R.string.multiSelectExport:{
//                    	dismissDialog = true;
//                    	break;
//                    }
                    default: {
                        dismissDialog = true;
                        Log.e(TAG, "Unexpected resource: "
                                + getActivity().getResources().getResourceEntryName(resId));
                    }
                }
                if (dismissDialog) {
                    dialog.dismiss();
                }
            }
        };
        return new AlertDialog.Builder(getActivity())
                .setTitle(contactsAreAvailable
                        ? R.string.dialog_import_export
                        : R.string.dialog_import)
                .setSingleChoiceItems(adapter, -1, clickListener)
                .create();
    }

    private void doShareVisibleContacts() {
        // TODO move the query into a loader and do this in a background thread//Contacts.CONTENT_URI
        final Cursor cursor = getActivity().getContentResolver().query(Contacts.CONTENT_URI,
                LOOKUP_PROJECTION, Contacts.IN_VISIBLE_GROUP + "!=0", null, null);
        Log.i("Contacts.CONTENT_URI",""+Contacts.CONTENT_URI);
        if (cursor != null) {
            try {
                if (!cursor.moveToFirst()) {
                    Toast.makeText(getActivity(), R.string.share_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i("cursor","\n"+cursor.getString(0));
                StringBuilder uriListBuilder = new StringBuilder();
                int index = 0;
                do {               	
                    if (index != 0)
                        uriListBuilder.append(':');
                    uriListBuilder.append(cursor.getString(0));
                    index++;
                } while (cursor.moveToNext());
                Uri uri = Uri.withAppendedPath(
                        Contacts.CONTENT_MULTI_VCARD_URI,
                        Uri.encode(uriListBuilder.toString()));
Log.i("share","\n"+uri);
                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(Contacts.CONTENT_VCARD_TYPE);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                getActivity().startActivity(intent);
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Handle "import from SIM" and "import from SD".
     *
     * @return {@code true} if the dialog show be closed.  {@code false} otherwise.
     */
    private boolean handleImportRequest(int resId) {
        // There are three possibilities:
        // - more than one accounts -> ask the user
        // - just one account -> use the account without asking the user
        // - no account -> use phone-local storage without asking the user
        
        /** zzz */
        /** force using PHONE account*/
//        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(getActivity());
//        final List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
//        final int size = accountList.size();
//        if (size > 1) {
//            // Send over to the account selector
//            final Bundle args = new Bundle();
//            args.putInt(KEY_RES_ID, resId);
//            SelectAccountDialogFragment.show(
//                    getFragmentManager(), this,
//                    R.string.dialog_new_contact_account,
//                    AccountListFilter.ACCOUNTS_CONTACT_WRITABLE, args);
//
//            // In this case, because this DialogFragment is used as a target fragment to
//            // SelectAccountDialogFragment, we can't close it yet.  We close the dialog when
//            // we get a callback from it.
//            return false;
//        }
//
//        AccountSelectionUtil.doImport(getActivity(), resId,
//                (size == 1 ? accountList.get(0) : null));
        AccountSelectionUtil
                .doImport(getActivity(), resId, new AccountWithDataSet("PHONE",
                        "com.android.localphone", null));

        return true; // Close the dialog.
    }

    /**
     * Called when an account is selected on {@link SelectAccountDialogFragment}.
     */
    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        AccountSelectionUtil.doImport(getActivity(), extraArgs.getInt(KEY_RES_ID), account);

        // At this point the dialog is still showing (which is why we can use getActivity() above)
        // So close it.
        dismiss();
    }

    @Override
    public void onAccountSelectorCancelled() {
        // See onAccountChosen() -- at this point the dialog is still showing.  Close it.
        dismiss();
    }


    private  void displaySIMSelection() {
        Log.d(TAG, "displayMyDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_sim);
        mSelectedSim = SIM_ID_INVALID;
        builder.setSingleChoiceItems(R.array.sub_list, -1,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Log.d(TAG, "onClicked Dialog on arg1 = " + arg1);
                mSelectedSim = arg1;
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "onClicked OK");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClassName("com.android.phone",
                    "com.android.phone.ExportContactsToSim");
                intent.putExtra(SUBSCRIPTION_KEY, mSelectedSim);
                if (mSelectedSim != SIM_ID_INVALID) {
                    ((AlertDialog)dialog).getContext().startActivity(intent);
                }
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "onClicked Cancel");
            }
        });

        dialog.setOnDismissListener(new OnDismissListener () {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "onDismiss");
                Log.d(TAG, "Selected SUB = " + mSelectedSim);
            }
        });
        dialog.show();
    }
}
