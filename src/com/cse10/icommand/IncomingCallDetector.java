package com.cse10.icommand;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class IncomingCallDetector extends BroadcastReceiver {
	
	private Context context;
	
	@Override
	public void onReceive(Context context, Intent intent) {

		this.context = context;
		
		MyPhoneStateListener phoneListener = new MyPhoneStateListener();
		TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);

	}

	// call state handler
	private class MyPhoneStateListener extends PhoneStateListener {

//		private boolean isRunning;
		
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);

			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				Log.v("DEBUG", "IDLE");
				break;

			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.v("DEBUG", "OFFHOOK");
				
//				if(!isRunning) {
//					Intent intent = new Intent(context, SpeechRecognitionService.class);
//					context.stopService(intent);
//					isRunning = false;
//				}
				
				// stop service
				Intent stopIntent = new Intent(context, SpeechRecognitionService.class);
				context.stopService(stopIntent);
				
				break;

			case TelephonyManager.CALL_STATE_RINGING:
				Log.v("DEBUG", "RINGING");
				
//				// start listening service
//				if(isMyServiceRunning()) {
//					isRunning = true;
//				} else {
//					isRunning = false;
//				}
				
				Intent startIntent = new Intent(context, SpeechRecognitionService.class);
				context.startService(startIntent);
				
				break;
			}

		}
		
//		private boolean isMyServiceRunning() {
//		    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//		    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//		        if (SpeechRecognitionService.class.getName().equals(service.service.getClassName())) {
//		            return true;
//		        }
//		    }
//		    return false;
//		}

	}

}
