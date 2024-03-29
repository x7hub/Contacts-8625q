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

import edu.bupt.contacts.ContactSaveService;
import edu.bupt.contacts.R;
import edu.bupt.contacts.activities.MultiSelectExport;
import edu.bupt.contacts.activities.PersonInfo;
import edu.bupt.contacts.model.AccountType;
import edu.bupt.contacts.model.AccountTypeManager;
import edu.bupt.contacts.model.EntityDelta;
import edu.bupt.contacts.model.EntityDeltaList;
import edu.bupt.contacts.model.EntityDelta.ValuesDelta;

import com.google.android.collect.Sets;
import com.google.common.annotations.VisibleForTesting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Contacts.People;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Entity;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 北邮ANT实验室
 * ddd
 * 
 * 删除一个联系人（包括保存在卡一卡二中） 通讯录功能2
 * 
 * 此文件取自codeaurora提供的适用于高通8625Q的android 4.1.2源码，有修改
 * 
 * */

/**
 * An interaction invoked to delete a contact.
 */
public class ContactDeletionInteraction extends Fragment
        implements LoaderCallbacks<Cursor>, OnDismissListener {

    private static final String FRAGMENT_TAG = "deleteContact";

    private static final String KEY_ACTIVE = "active";
    private static final String KEY_CONTACT_URI = "contactUri";
    private static final String KEY_FINISH_WHEN_DONE = "finishWhenDone";
    public static final String ARG_CONTACT_URI = "contactUri";

    private static final String[] ENTITY_PROJECTION = new String[] {
        Entity.RAW_CONTACT_ID, //0
        Entity.ACCOUNT_TYPE, //1
        Entity.DATA_SET, // 2
        Entity.CONTACT_ID, // 3
        Entity.LOOKUP_KEY, // 4
    };

    private static final int COLUMN_INDEX_RAW_CONTACT_ID = 0;
    private static final int COLUMN_INDEX_ACCOUNT_TYPE = 1;
    private static final int COLUMN_INDEX_DATA_SET = 2;
    private static final int COLUMN_INDEX_CONTACT_ID = 3;
    private static final int COLUMN_INDEX_LOOKUP_KEY = 4;

	private static final String TAG = null;

    private boolean mActive;
    private Uri mContactUri;
    private boolean mFinishActivityWhenDone;
    private Context mContext;
    private AlertDialog mDialog;
    
    /** This is a wrapper around the fragment's loader manager to be used only during testing. */
    private TestLoaderManager mTestLoaderManager;

    @VisibleForTesting
    int mMessageId;
    
    HashSet<Long>  readOnlyRawContacts;
    HashSet<Long>  writableRawContacts;
	int readOnlyCount;
    int writableCount;
    long rawContactId;
    String display_name = null;
    String display_number = null;
   //ddd
    String account_name=null;
   //ddd_end 
    
    		
    /**
     * Starts the interaction.
     *
     * @param activity the activity within which to start the interaction
     * @param contactUri the URI of the contact to delete
     * @param finishActivityWhenDone whether to finish the activity upon completion of the
     *        interaction
     * @return the newly created interaction
     */
    public static ContactDeletionInteraction start(
            Activity activity, Uri contactUri, boolean finishActivityWhenDone) {
        return startWithTestLoaderManager(activity, contactUri, finishActivityWhenDone, null);
    }

    /**
     * Starts the interaction and optionally set up a {@link TestLoaderManager}.
     *
     * @param activity the activity within which to start the interaction
     * @param contactUri the URI of the contact to delete
     * @param finishActivityWhenDone whether to finish the activity upon completion of the
     *        interaction
     * @param testLoaderManager the {@link TestLoaderManager} to use to load the data, may be null
     *        in which case the default {@link LoaderManager} is used
     * @return the newly created interaction
     */
    @VisibleForTesting
    static ContactDeletionInteraction startWithTestLoaderManager(
            Activity activity, Uri contactUri, boolean finishActivityWhenDone,
            TestLoaderManager testLoaderManager) {
        if (contactUri == null) {
            return null;
        }

        FragmentManager fragmentManager = activity.getFragmentManager();
        ContactDeletionInteraction fragment =
                (ContactDeletionInteraction) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new ContactDeletionInteraction();
            fragment.setTestLoaderManager(testLoaderManager);
            fragment.setContactUri(contactUri);
            fragment.setFinishActivityWhenDone(finishActivityWhenDone);
            fragmentManager.beginTransaction().add(fragment, FRAGMENT_TAG)
                    .commitAllowingStateLoss();
        } else {
            fragment.setTestLoaderManager(testLoaderManager);
            fragment.setContactUri(contactUri);
            fragment.setFinishActivityWhenDone(finishActivityWhenDone);
        }
        return fragment;
    }

    @Override
    public LoaderManager getLoaderManager() {
        // Return the TestLoaderManager if one is set up.
        LoaderManager loaderManager = super.getLoaderManager();
        if (mTestLoaderManager != null) {
            // Set the delegate: this operation is idempotent, so let's just do it every time.
            mTestLoaderManager.setDelegate(loaderManager);
            return mTestLoaderManager;
        } else {
            return loaderManager;
        }
    }

    /** Sets the TestLoaderManager that is used to wrap the actual LoaderManager in tests. */
    private void setTestLoaderManager(TestLoaderManager mockLoaderManager) {
        mTestLoaderManager = mockLoaderManager;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
            mDialog = null;
        }
    }

    public void setContactUri(Uri contactUri) {
        mContactUri = contactUri;
        mActive = true;
        if (isStarted()) {
            Bundle args = new Bundle();
            args.putParcelable(ARG_CONTACT_URI, mContactUri);
            getLoaderManager().restartLoader(R.id.dialog_delete_contact_loader_id, args, this);
        }
    }

    private void setFinishActivityWhenDone(boolean finishActivityWhenDone) {
        this.mFinishActivityWhenDone = finishActivityWhenDone;

    }

    /* Visible for testing */
    boolean isStarted() {
        return isAdded();
    }

    @Override
    public void onStart() {
        if (mActive) {
            Bundle args = new Bundle();
            args.putParcelable(ARG_CONTACT_URI, mContactUri);
            getLoaderManager().initLoader(R.id.dialog_delete_contact_loader_id, args, this);
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mDialog != null) {
            mDialog.hide();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri contactUri = args.getParcelable(ARG_CONTACT_URI);
        return new CursorLoader(mContext,
                Uri.withAppendedPath(contactUri, Entity.CONTENT_DIRECTORY), ENTITY_PROJECTION,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }

        if (!mActive) {
            return;
        }

        long contactId = 0;
        String lookupKey = null;

        // This cursor may contain duplicate raw contacts, so we need to de-dupe them first
        HashSet<Long>  readOnlyRawContacts = Sets.newHashSet();
        HashSet<Long>  writableRawContacts = Sets.newHashSet();

        AccountTypeManager accountTypes = AccountTypeManager.getInstance(getActivity());
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            rawContactId = cursor.getLong(COLUMN_INDEX_RAW_CONTACT_ID);
            
            
            
            
            final String accountType = cursor.getString(COLUMN_INDEX_ACCOUNT_TYPE);
    
            final String dataSet = cursor.getString(COLUMN_INDEX_DATA_SET);
            contactId = cursor.getLong(COLUMN_INDEX_CONTACT_ID);
            lookupKey = cursor.getString(COLUMN_INDEX_LOOKUP_KEY);
            AccountType type = accountTypes.getAccountType(accountType, dataSet);
            boolean writable = type == null || type.areContactsWritable();
            if (writable) {
                writableRawContacts.add(rawContactId);
                Log.i("writableRawContacts_id",""+rawContactId);
            } else {
                readOnlyRawContacts.add(rawContactId);
                Log.i("readOnlyRawContacts_id",""+rawContactId);
            }
        }

        readOnlyCount = readOnlyRawContacts.size();
        writableCount = writableRawContacts.size();
        Log.i("readOnlyCount",""+readOnlyCount);
        Log.i("writableCount",""+writableCount);
        if (readOnlyCount > 0 && writableCount > 0) {
            mMessageId = R.string.readOnlyContactDeleteConfirmation;
            Log.i("mMessageId","1");
        }
//        else if (readOnlyCount > 0 && writableCount == 0) {
//            mMessageId = R.string.readOnlyContactWarning;//read only sim card
//            Log.i("mMessageId","2");
//        } 
        else if (readOnlyCount == 0 && writableCount > 1) {
            mMessageId = R.string.multipleContactDeleteConfirmation;
            Log.i("mMessageId","3");
        } else {
            mMessageId = R.string.deleteConfirmation;//local phone
            Log.i("mMessageId","4");
           
        }
        Log.i("mMessageId",""+mMessageId);
        final Uri contactUri = Contacts.getLookupUri(contactId, lookupKey);
        showDialog(mMessageId, contactUri);

        // We don't want onLoadFinished() calls any more, which may come when the database is
        // updating.
        getLoaderManager().destroyLoader(R.id.dialog_delete_contact_loader_id);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void showDialog(int messageId, final Uri contactUri) {
        mDialog = new AlertDialog.Builder(getActivity())
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(messageId)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	doDeleteContact(contactUri);                  	
             	
/*                        	if (readOnlyCount > 0 && writableCount == 0) {
                        		getContactInfo(); 
                        		doDeleteContact(contactUri);
                        		SimDelete(1);
                        	}else{
                        		doDeleteContact(contactUri);
                        	}
                        	*/
//ddd 
                        	
                        	if ( writableCount > 0 && readOnlyCount== 0) {
                        	getContactInfo(); 
                        	doDeleteContact(contactUri);
                            Log.i(TAG, "this is sim delete function call");
                            Log.i(TAG, "rawContactID "+rawContactId);
                         
                            
                            Cursor p =  mContext.getContentResolver().query(
                                 ContactsContract.RawContacts.CONTENT_URI, //
                                 new String[] { ContactsContract.RawContacts.ACCOUNT_NAME},  //通过联系人的rawContactId得到该联系人所在账户（手机、卡一、卡二）
                                 ContactsContract.RawContacts._ID+" =? ", //
                                 new String[] { String.valueOf(rawContactId) }, //
                                 null);
                            if (p.moveToNext()) {

                             try {
                                 account_name = p.getString(0);
                               Log.i(TAG, "account_name - " + account_name);
                             } catch (Exception e) {
                                 Log.w(TAG, e.toString());
                             }
                             
                            }
                            p.close();
                    		//调用删除联系人的函数，删除选定联系人，该函数参数为联系人所在的账户
                            SimDelete(account_name);
//ddd_end                            
                        	}
                        	else{
                    		doDeleteContact(contactUri);
                     	}
                            
                            
                     }
                   }
                )
                .create();

        mDialog.setOnDismissListener(this);
        mDialog.show();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mActive = false;
        mDialog = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_ACTIVE, mActive);
        outState.putParcelable(KEY_CONTACT_URI, mContactUri);
        outState.putBoolean(KEY_FINISH_WHEN_DONE, mFinishActivityWhenDone);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mActive = savedInstanceState.getBoolean(KEY_ACTIVE);
            mContactUri = savedInstanceState.getParcelable(KEY_CONTACT_URI);
            mFinishActivityWhenDone = savedInstanceState.getBoolean(KEY_FINISH_WHEN_DONE);
        }
    }

    protected void doDeleteContact(Uri contactUri) {
        mContext.startService(ContactSaveService.createDeleteContactIntent(mContext, contactUri));
        if (isAdded() && mFinishActivityWhenDone) {
            getActivity().finish();
        }
    }
    
/*    public void SimDelete(int subNum) {
    	Uri uri = null;
    	if(subNum == 1){
    		  uri= Uri.parse("content://iccmsim/adn");
    	}else if(subNum == 2){
    		  uri = Uri.parse("content://iccmsim/adn_sub2");
    	}
       
        Cursor cursor = mContext.getContentResolver().query(uri, null, null,
                null, null);
        Log.d("1023", ">>>>>> " + cursor.getCount());
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(People.NAME));
            String phoneNumber = cursor.getString(cursor
                    .getColumnIndex(People.NUMBER));
            name = display_name;phoneNumber = display_number;
            String where = "tag='" + name + "'";
            where += " AND number='" + phoneNumber + "'";
            mContext.getContentResolver().delete(uri, where, null);
            Log.i(TAG, "this is sim delete inside");
        }
    }
    */
    

//ddd 删除存在卡中的联系人
    public void SimDelete(String account_name) {
    	Uri simUri = null;
        if (account_name.equals("UIM") || account_name.equals("SIM1") ) {//判断联系人所在账户
            simUri = Uri.parse("content://iccmsim/adn");//将卡一地址赋给uri
            Log.i(TAG, "content://iccmsim/adn");
        } else if(account_name.equals("SIM2")) {       //将卡二地址赋给uri
            simUri = Uri.parse("content://iccmsim/adn_sub2");
            Log.i(TAG, "content://iccmsim/adn_sub2");
        } else {
            Log.d(TAG, "return");
            return;
        }

       
        Cursor cursor = mContext.getContentResolver().query(simUri, null, null,
                null, null);
        Log.d("delete account", ">>>>>> " + cursor.getCount());
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(People.NAME));  
            String phoneNumber = cursor.getString(cursor
                    .getColumnIndex(People.NUMBER));
            name = display_name;
            phoneNumber = display_number;
            String where = "tag='" + name + "'"; //通过联系人姓名、电话确定删除项
            where += " AND number='" + phoneNumber + "'";
            int deleteSimRow=mContext.getContentResolver().delete(simUri, where, null);  //删除所选联系人
            
            Log.i(TAG, "delete_name - " + name);
            Log.i(TAG, "delete_phoneNumber - " + phoneNumber);
            Log.i(TAG, "deleteSimRow " + deleteSimRow);
            Log.i(TAG, "where " + where);
            Log.i(TAG, "Simuri " + simUri);
            if (deleteSimRow !=0) {
                Log.d(TAG, "delate sim card successfully");  
            }
      
            
            
        }
    }
    
//ddd_end    
    
    private void getContactInfo() {	
    	
    	String[] projection= {Phone.DISPLAY_NAME, Phone.NUMBER, Phone.PHOTO_ID,Phone.RAW_CONTACT_ID};
    	Cursor cur = mContext.getContentResolver().query(Phone.CONTENT_URI, projection, null, null, Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
    	cur.moveToFirst();
    	while(cur.getCount() > cur.getPosition()) {   		   		
    		String id = cur.getString(cur.getColumnIndex(Phone.RAW_CONTACT_ID));			
    		String number = cur.getString(cur.getColumnIndex(Phone.NUMBER));
    		String name = cur.getString(cur.getColumnIndex(Phone.DISPLAY_NAME));
    		Log.i("CompareToRaw","id="+id+";rawContactId="+rawContactId);
    		if(Long.parseLong(id) == rawContactId ){// && readOnlyCount > 0id.equals(rawContactId)
    			display_name = name;
    			display_number = number;
    			Log.i("contacts", "id:"+id+";display_name:" + display_name + ";display_number:" + display_number);	
    			
    		}
    		cur.moveToNext();	 
    		
    	}
    	cur.close();
    }
    
}
