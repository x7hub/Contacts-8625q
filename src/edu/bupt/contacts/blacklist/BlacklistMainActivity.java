package edu.bupt.contacts.blacklist;

import edu.bupt.contacts.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;

public class BlacklistMainActivity extends FragmentActivity implements
        RadioButtonsFragment.SwitchTabs {

    public static final String TAG = "BlacklistMainActivity";
    public static final String BACKGROUND_LISTEN_SERVICE = "listen_in_background";
    private Fragment radioButtons;
    private Fragment mainView;
    private Fragment msgBlock;
    private Fragment callBlock;
    private Fragment blackList;
    private Fragment whiteList;
    private Fragment settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.blacklist_main_activity);
        initFragment(savedInstanceState);
    }

    private void initFragment(Bundle savedInstanceState) {

        FragmentManager fragmentManager;
        if (findViewById(R.id.radio_buttons) != null) {
            if (savedInstanceState != null) {
                return;
            }
            radioButtons = new RadioButtonsFragment(this);
            fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .add(R.id.radio_buttons, radioButtons).commit();
        }

        if (findViewById(R.id.main_view) != null) {
            if (savedInstanceState != null) {
                return;
            }
            msgBlock = new MsgBlockFragment(this);
            fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().add(R.id.main_view, msgBlock)
                    .commit();
            mainView = msgBlock;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, 1, R.string.exit);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void msgBlock() {
        updatefragment(msgBlock);
    }

    @Override
    public void callBlock() {
        callBlock = new CallBlockFragment(this);
        updatefragment(callBlock);
    }

    @Override
    public void blackList() {
        blackList = new BlackListFragment(this);
        updatefragment(blackList);
    }

    public void whiteList() {
        whiteList = new WhiteListFragment(this);
        updatefragment(whiteList);
    }

    @Override
    public void settings() {
        settings = new SettingsFragment(this);
        updatefragment(settings);
    }

    private void updatefragment(Fragment fragment) {
        FragmentManager fragmentManager;
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().remove(mainView)
                .add(R.id.main_view, fragment).commit();
        mainView = fragment;
    }

}