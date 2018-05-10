package com.excel.displayproject.secondgen;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.excel.excelclasslibrary.UtilShell;

import java.util.Timer;
import java.util.TimerTask;

public class RebootPromptActivity extends Activity {
	
	public static final String TAG = "RebootPromptActivity";
	Context context = this;
	
	TextView tv_countdown;
	Button bt_yes, bt_no;
	Timer countdown;
	
	int counter = 30;
    boolean postpone_clicked = false;
	
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		
		setContentView( R.layout.activity_reboot_prompt );

		init();
	}

    @Override
    protected void onPause() {
        super.onPause();

        Log.d( TAG, "onPause()" );

        if( ! postpone_clicked ){
            sendBroadcast( new Intent( "postpone_reboot" ) );
        }
    }

    private void init(){
		
		bt_yes = (Button) findViewById( R.id.bt_yes );
		bt_yes.requestFocus();
		bt_no  = (Button) findViewById( R.id.bt_no );
		
		tv_countdown = (TextView) findViewById( R.id.tv_countdown );
		
		bt_yes.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
				UtilShell.executeShellCommandWithOp( "reboot" );
			}
		});

		bt_no.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick( View v ) {
				stopTimer();
				
				// Set Alarm for Next Hour of same day
				sendBroadcast( new Intent( "postpone_reboot" ) );
				
				//dispatchKeyEvent( new KeyEvent( KeyEvent.ACTION_DOWN, 3 ) );

				UtilShell.executeShellCommandWithOp( "input keyevent 3" );

                postpone_clicked = true;
				
				finish();
				
			}
		});
		
		startTimer();
		
		/*LocalBroadcastManager.getInstance(this).registerReceiver( mMessageReceiver,
			      new IntentFilter( "update-countdown" ) );*/
	}
	
	
	
	private void startTimer(){
		counter = 30;
		countdown = new Timer();
		countdown.scheduleAtFixedRate( new TimerTask() {

			@Override
			public void run() {
				Log.i( TAG, "Timer : "+counter );
				if( counter == 0 ){
					UtilShell.executeShellCommandWithOp( "reboot" );
					stopTimer();
					return;
				}
				updateCountdown();
				counter--;
			}

		}, 0, 1000 );
	}
	
	private void stopTimer(){
		Log.i( TAG, "Timer stopped" );
		countdown.cancel();
	}
	  
	private void updateCountdown(){
		/*Intent intent = new Intent( "update-countdown" );
		LocalBroadcastManager.getInstance( this ).sendBroadcast( intent );*/

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
	}
	
	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event ) {
		Log.d( TAG, "keyCode : "+keyCode );
		if( keyCode == 4 ){ // back button override
			return true;
		}
		return false; //super.onKeyDown(keyCode, event);
	}
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			tv_countdown.setText( counter+"" );
		}
	};
	
	
}
