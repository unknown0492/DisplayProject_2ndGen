package com.excel.displayproject.secondgen;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.excel.displayproject.util.Constants;
import com.excel.excelclasslibrary.UtilSharedPreferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class MainActivity extends Activity {

    static final String TAG = "DisplayProject";

    String[] permissions = {
            // Manifest.permission.RECEIVE_BOOT_COMPLETED, // Normal Permission
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            // Manifest.permission.WRITE_SETTINGS   // Special Permisison -> https://developer.android.com/reference/android/Manifest.permission.html#WRITE_SETTINGS
    };

    SharedPreferences spfs;
    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerAllBroadcasts();

        spfs = (SharedPreferences) UtilSharedPreferences.createSharedPreference( this, Constants.PERMISSION_SPFS );

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if ( checkPermissions() ) {
                // permissions  granted.
                UtilSharedPreferences.editSharedPreference( spfs, Constants.IS_PERMISSION_GRANTED, Constants.PERMISSION_GRANTED_YES );
                finish();
            }
        }
        else{
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if( ( keyCode == KeyEvent.KEYCODE_ENTER ) ||
                ( keyCode == KeyEvent.KEYCODE_DPAD_CENTER ) ){
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    Vector<IntentFilter> intentFilterVector;

    private void registerAllBroadcasts(){
        receiver = new Receiver();
        intentFilterVector = new Vector<IntentFilter>();
        intentFilterVector.add( new IntentFilter( "connectivity_change" ) );
        intentFilterVector.add( new IntentFilter( "show_ota_downloading" ) );
        intentFilterVector.add( new IntentFilter( "ota_progress_update" ) );
        intentFilterVector.add( new IntentFilter( "ota_download_complete" ) );
        intentFilterVector.add( new IntentFilter( "ota_download_complete1" ) );
        intentFilterVector.add( new IntentFilter( "ota_progress_update1" ) );
        intentFilterVector.add( new IntentFilter( "show_reboot_prompt" ) );
        // intentFilterVector.add( new IntentFilter( "" ) );

        Iterator<IntentFilter> iterator = intentFilterVector.iterator();
        while( iterator.hasNext() ){
            registerReceiver( receiver, iterator.next() );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver( receiver );

    }

    @Override
    public void onRequestPermissionsResult( int requestCode, String permissions[], int[] grantResults ) {
        switch ( requestCode ) {
            case 10:
            {
                if( grantResults.length > 0 && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ){
                    // permissions granted.
                    Log.d( TAG, grantResults.length + " Permissions granted : " );
                    UtilSharedPreferences.editSharedPreference( spfs, Constants.IS_PERMISSION_GRANTED, Constants.PERMISSION_GRANTED_YES );
                } else {
                    String permission = "";
                    for ( String per : permissions ) {
                        permission += "\n" + per;
                    }
                    // permissions list of don't granted permission
                    Log.d( TAG, "Permissions not granted : "+permission );
                    UtilSharedPreferences.editSharedPreference( spfs, Constants.IS_PERMISSION_GRANTED, Constants.PERMISSION_GRANTED_NO );
                }
                return;
            }
        }
    }

    public  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for ( String p:permissions ) {
            result = ContextCompat.checkSelfPermission( this, p );
            if ( result != PackageManager.PERMISSION_GRANTED ) {
                listPermissionsNeeded.add( p );
            }
        }
        if ( !listPermissionsNeeded.isEmpty() ) {
            ActivityCompat.requestPermissions( this, listPermissionsNeeded.toArray( new String[ listPermissionsNeeded.size() ] ), 10 );
            return false;
        }
        return true;
    }
}
