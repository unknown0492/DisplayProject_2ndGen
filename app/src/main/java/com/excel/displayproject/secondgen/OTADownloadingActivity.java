package com.excel.displayproject.secondgen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
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
import com.excel.excelclasslibrary.UtilShell;
import com.excel.util.MD5;

import java.io.File;
import java.io.FileOutputStream;

public class OTADownloadingActivity extends Activity {

    TextView tv_progress, tv_total_size, tv_message, tv_download_complete;
    LinearLayout ll_progress;
    double progress, total;
    File ota_zip_file;
    int file_size;
    final static String TAG = "OTADownloadingActivity";
    Handler handler = new Handler();
    Context context;
    BroadcastReceiver progressUpdateReceiver, downloadCompleteReceiver;
    ConfigurationReader configurationReader;
    String new_firmware_md5 = "";

    // Script
    public static final String FIRMWARE_UPDATE_SCRIPT			= 	"echo 'boot-recovery ' > /cache/recovery/command\n" +
            "echo '--update_package=/cache/update.zip' >> /cache/recovery/command\n"+
            "reboot recovery";

    boolean showing = true;

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

        getData();

        calculateSizes();

        // startProgressTimer();
    }

    boolean postpone_clicked = false;

    @Override
    protected void onPause() {
        super.onPause();
        Log.d( TAG, "onPause()" );

        validatePrompt();
        //postponeUpgrade();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d( TAG, "onResume()" );

        /*handler.post(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getData();

                        calculateSizes();
                    }
                });
            }
        });*/



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d( TAG, "onDestroy()" );

    }

    private void initViews(){
        tv_message = (TextView) findViewById( R.id.tv_message );
        tv_progress = (TextView) findViewById( R.id.tv_progress );
        tv_total_size = (TextView) findViewById( R.id.tv_total_size );
        ll_progress = (LinearLayout) findViewById( R.id.ll_progress );
        tv_download_complete = (TextView) findViewById( R.id.tv_download_complete );
    }


    private void getData(){
        Intent in = getIntent();
        //ota_zip_file = new File( in.getStringExtra( "ota_zip_file_path" ) );
        try {
            file_size = in.getIntExtra( "file_size", 0 );
            progress = in.getDoubleExtra( "progress", 0 );

        }
        catch( Exception e ){
            file_size = 0;
            progress = 0;
            e.printStackTrace();
        }
        //Log.d( TAG, "File Path : " + ota_zip_file.getAbsolutePath());
        Log.d( TAG, "File Size : " + file_size + " bytes" );

    }

    boolean show_prompt;

    private void registerBroadcasts(){
        progressUpdateReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent ) {
                //Log.i( TAG, "Progress Updated !" );

                progress    = intent.getDoubleExtra( "progress", 0 );
                file_size   = intent.getIntExtra( "file_size", 0 );

                Log.d( TAG, "Progress : " + progress + "%" );
                //tv_total_size.setText( "" + total + " MB" );
                //tv_progress.setText( "" + progress + "%" );
                calculateSizes();


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

               /* new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        *//*createUpdateScript();

                        copyFirmwareToCache();

                        verifyCacheFirmwareCopy();*//*
                    }
                }, 2000 );*/
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
                dialog.dismiss();
            }

        });
        ab.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                postponeUpgrade();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 0 );
            }
        });
        ab.show();
    }

    //boolean upgrade_postponed = false;

    private void postponeUpgrade(){
        /*AlarmManager am = (AlarmManager) getSystemService( ALARM_SERVICE );
        Intent in = new Intent( "ota_download_complete" );
        in.putExtra( "show_prompt", true );
        PendingIntent pi = PendingIntent.getBroadcast( context, 0, in, 0 );
        am.set( AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 20000, pi );
        upgrade_postponed = true;*/
        Intent in = new Intent( "postpone_ota_upgrade" );
        sendBroadcast( in );
    }

    private void calculateSizes(){
        total    = Double.parseDouble( String.format( "%.2f", (double)file_size/(double)1024/(double)1024 ) );
        tv_total_size.setText( "" + total + " MB" );
        tv_progress.setText( "" + progress + "%" );
        Log.d( TAG, "Total Size : " + total + " MB" );
    }

    private void startProgressTimer(){
        new AsyncTask< Void, Void, Void >(){

            @Override
            protected Void doInBackground(Void... params) {
                new Thread(new Runnable() {
                    public void run() {
                        while( true ){

                            if( progress == total ){
                                Log.d( TAG, "Download Completed !" );
                                break;
                            }

                            handler.post(new Runnable() {
                                public void run() {
                                    progress = (ota_zip_file.length()/1024)/1024;   // size in MB
                                    Log.d( TAG, "Progress : " + progress + " MB" );

                                    tv_progress.setText( "" + progress + " MB" );

                                }
                            });



                            try {
                                Thread.sleep( 1000 );

                            } catch ( InterruptedException e ) {
                                e.printStackTrace();
                            }

                        }
                    }
                }).start();
                return null;
            }

        }.execute();
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
