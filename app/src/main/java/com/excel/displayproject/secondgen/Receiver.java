package com.excel.displayproject.secondgen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.excel.configuration.ConfigurationReader;
import com.excel.excelclasslibrary.UtilSharedPreferences;
import com.excel.excelclasslibrary.UtilShell;

import static com.excel.util.Constants.IS_PERMISSION_GRANTED;
import static com.excel.util.Constants.PERMISSION_GRANTED_NO;
import static com.excel.util.Constants.PERMISSION_SPFS;

public class Receiver extends BroadcastReceiver {

    final static String TAG = "Receiver";
    ConfigurationReader configurationReader;
    SharedPreferences spfs;

    @Override
    public void onReceive( final Context context, final Intent intent ) {
        String action = intent.getAction();
        Log.d( TAG, "action : " + action );

        /*MainActivity ma = new MainActivity();

        // Check permissions before executing any broadcast, this is to prevent the app from hanging
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if ( ma.checkPermissions() ) {
                // permissions  granted.
                Log.d( TAG, "All permissions have been granted, just proceed !" );
            }
            else{
                startMe( context );
                return;
            }
        }*/

        // Check permissions before executing any broadcast, this is to prevent the app from hanging
        spfs = (SharedPreferences) UtilSharedPreferences.createSharedPreference( context, PERMISSION_SPFS );
        String is_permission_granted = UtilSharedPreferences.getSharedPreference( spfs, IS_PERMISSION_GRANTED, PERMISSION_GRANTED_NO ).toString().trim();
        Log.d( TAG, "Permission granted : "+is_permission_granted );

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if( is_permission_granted.equals( "yes" ) ){
                Log.d( TAG, "All permissions have been granted, just proceed !" );
            }
            else{
                startMe( context );
                return;
            }
        }

        configurationReader = ConfigurationReader.reInstantiate();


        if( action.equals( "android.net.conn.CONNECTIVITY_CHANGE" ) || action.equals( "connectivity_changed" ) ){

            // 1. First time in order to receive broadcasts, the app should be started at least once
            startMe( context );

            // Execute these broadcasts only when the OTS IS COMPLETED
            String is_ots_completed = configurationReader.getIsOtsCompleted().trim();
            if( is_ots_completed.equals( "0" ) ) {
                Log.d( TAG, "OTS has not been completed, DisplayProject Broadcasts will not execute !" );
                return;
            }

            // These broadcasts are to be FIRED only once per box reboot
            if( ! isConnectivityBroadcastFired() ){

                setConnectivityBroadcastFired( true );

            }

        }
        else if( action.equals( "show_ota_downloading" ) ){
            openOTADownloadingActivity( context, intent );
        }
        else if( action.equals( "ota_progress_update" ) ){
            openOTADownloadingActivity( context, intent );
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent in = new Intent( "ota_progress_update1" );
                    in.putExtras( intent.getExtras() );
                    context.sendBroadcast( in );
                }
            }, 2000 );

        }
        else if( action.equals( "ota_download_complete" ) ){
            openOTADownloadingActivity( context, intent );
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent in = new Intent( "ota_download_complete1" );
                    //in.putExtras( intent.getExtras() );
                    in.putExtra( "show_prompt", intent.getBooleanExtra( "show_prompt", false ) );
                    context.sendBroadcast( in );
                }
            }, 2000 );

        }
        else if( action.equals( "show_reboot_prompt" ) ){
            openRebootPromptActivity( context );
        }


    }

    private void startMe( Context context ){
        // Start this app activity
        Intent in = new Intent( context, MainActivity.class );
        in.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        context.startActivity( in );
    }

    private void openOTADownloadingActivity( Context context, Intent intent ){
        Intent in = new Intent( context, OTADownloadingActivity.class );
        in.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        context.startActivity( in );
    }

    private void openRebootPromptActivity( Context context ){
        Intent in = new Intent( context, RebootPromptActivity.class );
        in.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        context.startActivity( in );
    }


    private void setConnectivityBroadcastFired( boolean is_it ){
        String s = (is_it)?"1":"0";
        Log.d( TAG, "setConnectivityBroadcastFired() : " + s );
        UtilShell.executeShellCommandWithOp( "setprop dp_br_fired " + s );
    }

    private boolean isConnectivityBroadcastFired(){
        String is_it = UtilShell.executeShellCommandWithOp( "getprop dp_br_fired" ).trim();
        return ( is_it.equals( "0" ) || is_it.equals( "" ) )?false:true;
    }


}
