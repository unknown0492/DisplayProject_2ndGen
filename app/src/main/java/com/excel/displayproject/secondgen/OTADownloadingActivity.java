package com.excel.displayproject.secondgen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.excel.configuration.ConfigurationReader;
import com.excel.customitems.CustomItems;
import com.excel.excelclasslibrary.Constants;
import com.excel.excelclasslibrary.UtilMisc;
import com.excel.excelclasslibrary.UtilShell;
import com.excel.util.MD5;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class OTADownloadingActivity extends Activity {


    final static String TAG = "OTADownloadingActivity";

    // Script
    public static final String FIRMWARE_UPDATE_SCRIPT			= 	"echo 'boot-recovery ' > /cache/recovery/command\n" +
            "echo '--update_package=/cache/update.zip' >> /cache/recovery/command\n"+
            "reboot recovery";

    ConfigurationReader configurationReader;
    Context context;
    Timer countdown;
    int counter = 30;
    BroadcastReceiver downloadCompleteReceiver;
    int file_size;
    Handler handler = new Handler();
    LinearLayout ll_progress;
    String new_firmware_md5 = "";
    File ota_zip_file;

    boolean postpone_clicked = false;
    double progress;
    BroadcastReceiver progressUpdateReceiver;
    boolean show_prompt;
    boolean showing = true;
    double total;
    TextView tv_download_complete;
    TextView tv_message;
    TextView tv_progress;
    TextView tv_total_size;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_otadownloading );

        context = this;

        init();

    }



    private void init(){
        initViews();

        registerBroadcasts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d( TAG, "onPause()" );

        validatePrompt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d( TAG, "onResume()" );

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d( TAG, "onDestroy()" );

        unregisterReceiver( progressUpdateReceiver );
        unregisterReceiver( downloadCompleteReceiver );
    }

    private void initViews(){
        tv_message = (TextView) findViewById( R.id.tv_message );
        tv_progress = (TextView) findViewById( R.id.tv_progress );
        tv_total_size = (TextView) findViewById( R.id.tv_total_size );
        ll_progress = (LinearLayout) findViewById( R.id.ll_progress );
        tv_download_complete = (TextView) findViewById( R.id.tv_download_complete );
    }

    private void registerBroadcasts(){
        progressUpdateReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent ) {
                //Log.i( TAG, "Progress Updated !" );

                tv_message.setVisibility( View.VISIBLE );
                ll_progress.setVisibility( View.VISIBLE );
                tv_download_complete.setVisibility( View.GONE );

                progress    = intent.getDoubleExtra( "progress", 0 );
                total       = intent.getDoubleExtra( "file_size", 0 );

                Log.d( TAG, "Progress : " + progress + "%" );

                setSizes();


            }
        };
        registerReceiver( progressUpdateReceiver, new IntentFilter( "ota_progress_update1" ) );
        //LocalBroadcastManager.getInstance( context ).registerReceiver( progressUpdateReceiver, new IntentFilter( "ota_progress_update" ) );

        downloadCompleteReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( final Context context, Intent intent ) {
                Log.i( TAG, "Downloaded !" );

                show_prompt = intent.getBooleanExtra( "show_prompt", false );

                tv_message.setVisibility( View.GONE );
                ll_progress.setVisibility( View.GONE );
                tv_download_complete.setVisibility( View.VISIBLE );

                if( show_prompt ){
                    showPrompt();
                }

            }
        };
        registerReceiver( downloadCompleteReceiver, new IntentFilter( "ota_download_complete1" ) );
        //LocalBroadcastManager.getInstance( context ).registerReceiver( downloadCompleteReceiver, new IntentFilter( "ota_download_complete" ) );
    }

    private void validatePrompt(){
        if( show_prompt ){
            if( ! postpone_clicked ){
                postponeUpgrade();
            }
        }
    }

    private void showPrompt(){
        // Show AlertDialog with a yes and no Button
        // Yes Button will send a broadcast back to DataDownloader to reboot the box and start the upgrade
        // No Button will set an Alarm of next 1 hour to broadcast the ota_download_complete intent with show_prompt=true
        AlertDialog.Builder ab = new AlertDialog.Builder( context );
        ab.setCancelable( false );

        ab.setTitle( "APPS TV is Upgrading" );
        ab.setMessage( "APPS TV needs to upgrade. Is it okay to upgrade now ? Press Yes to upgrade now or No to postpone the upgrade !" );
        ab.setPositiveButton( "Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick( DialogInterface dialog, int which ) {
                sendBroadcast( new Intent( "run_ota_upgrade" ) );
                postpone_clicked = true;
                stopTimer();
                dialog.dismiss();
            }

        });
        ab.setNegativeButton( "No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick( final DialogInterface dialog, int which) {
                postponeUpgrade();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        postpone_clicked = true;
                        dialog.dismiss();
                        stopTimer();
                        finish();
                    }
                }, 2000 );
            }
        });
        ab.show();
        startTimer();
    }


    private void startTimer(){
        counter = 30;
        countdown = new Timer();
        countdown.scheduleAtFixedRate( new TimerTask() {

            @Override
            public void run() {
                Log.i( TAG, "Timer : "+counter );
                if( counter == 0 ){
                    /*UtilShell.executeShellCommandWithOp( "reboot" );
                    //stopTimer( di );*/
                    sendBroadcast( new Intent( "run_ota_upgrade" ) );
                    postpone_clicked = true;
                    stopTimer();
                    finish();
                    return;
                }
                counter--;
            }

        }, 0, 1000 );
    }

    private void stopTimer(){
        Log.i( TAG, "Timer stopped" );
        countdown.cancel();
    }

    /*private void updateCountdown(){
		*//*Intent intent = new Intent( "update-countdown" );
		LocalBroadcastManager.getInstance( this ).sendBroadcast( intent );*//*

        new Handler( Looper.getMainLooper() ).post(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_countdown.setText( counter+"" );
                    }
                });
            }
        });
    }*/

    private void postponeUpgrade(){
        /*Intent in = new Intent( "postpone_ota_upgrade" );
        sendBroadcast( in );*/

        UtilMisc.sendExplicitExternalBroadcast( context, "postpone_ota_upgrade", Constants.DATADOWNLOADER_PACKAGE_NAME, Constants.DATADOWNLOADER_RECEIVER_NAME );
    }

    private void setSizes(){
        // total    = Double.parseDouble( String.format( "%.2f", (double)file_size/(double)1024/(double)1024 ) );
        tv_total_size.setText( "" + total + " MB" );
        tv_progress.setText( "" + progress + "%" );
        Log.d( TAG, "Total Size : " + total + " MB" );
    }

    private void createUpdateScript(){
        configurationReader = ConfigurationReader.getInstance();

        // Create the script inside OTS
        File temp_file = new File( Environment.getExternalStorageDirectory() + File.separator + "up.sh" );
        if( temp_file.exists() )
            temp_file.delete();

        try {
            FileOutputStream fos = new FileOutputStream( temp_file );
            Log.d( TAG, "temp file created at : " + temp_file.getAbsolutePath());
            fos.write( FIRMWARE_UPDATE_SCRIPT.getBytes() );

            // Copy up.sh to /cache/
            UtilShell.executeShellCommandWithOp( "cp /mnt/sdcard/up.sh /cache/up.sh");

            // Set 777 permission for up.sh
            UtilShell.executeShellCommandWithOp( "chmod -R 777 /cache", "chmod 777 /cache/up.sh");

            fos.close();
        }
        catch ( Exception e ){
            e.printStackTrace();
        }
    }

    private void copyFirmwareToCache(){

        UtilShell.executeShellCommandWithOp( "chmod -R 777 /cache", "rm /cache/update.zip" );

        String s = UtilShell.executeShellCommandWithOp( "cp /mnt/sdcard/appstv_data/firmware/update.zip /cache/update.zip",
                "chmod 777 /cache/update.zip" );

        Log.d( TAG, "Firmware successfully copied to /cache : " + s );
    }

    private void verifyCacheFirmwareCopy(){

        try {
            String downloaded_md5 = MD5.getMD5Checksum( new File( "/cache/update.zip" ) );
            Log.d( TAG, String.format( "Original MD5 %s, Downloaded MD5 %s", new_firmware_md5, downloaded_md5 ) );

            if( ! new_firmware_md5.equals( downloaded_md5 ) ){
                CustomItems.showCustomToast( context, "error", "There was an error. Trying Again !", Toast.LENGTH_LONG );
                copyFirmwareToCache();
            }
            else{
                Log.d( TAG, "All Good ! Box is Rebooting Now :)" );
                UtilShell.executeShellCommandWithOp( "sh /cache/up.sh" );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        return true;
    }
}
