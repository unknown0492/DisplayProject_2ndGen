package com.excel.displayproject.secondgen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.excel.excelclasslibrary.UtilShell;


public class Receiver extends BroadcastReceiver {

    final static String TAG = "Receiver";

    @Override
    public void onReceive( final Context context, final Intent intent ) {
        String action = intent.getAction();
        Log.d( TAG, "action : " + action );

        if( action.equals( "android.net.conn.CONNECTIVITY_CHANGE" ) || action.equals( "connectivity_changed" ) ){

            // 1. First time in order to receive broadcasts, the app should be started at least once
            startMe( context );

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
