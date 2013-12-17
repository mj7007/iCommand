package com.cse10.icommand;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

public class SpeachRecognitionService extends Service {

	protected AudioManager mAudioManager;
	protected SpeechRecognizer mSpeechRecognizer;
	protected Intent mSpeechRecognizerIntent;
	protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

	protected boolean mIsListening;
	protected volatile boolean mIsCountDownOn;

	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(this, "iCommand Service started", Toast.LENGTH_SHORT).show();
		
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
		mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
		mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "iCommand Service started", Toast.LENGTH_SHORT).show();
		
		int result = super.onStartCommand(intent, flags, startId);
		mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
		SpeechRecognitionListener listener = new SpeechRecognitionListener();
		mSpeechRecognizer.setRecognitionListener(listener);
		mSpeechRecognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()));
		
		return result;

	}

	@Override
	public void onDestroy() {

		Toast.makeText(getApplicationContext(), "iCommand Service destroyed", Toast.LENGTH_LONG).show();

		if (mIsCountDownOn) {
			mNoSpeechCountDown.cancel();
		}
		if (mSpeechRecognizer != null) {
			mSpeechRecognizer.destroy();
		}
	}

	@Override
	public IBinder onBind(Intent arg) {
		return null;
	}

	protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000) {

		@Override
		public void onTick(long millisUntilFinished) {
		}

		@Override
		public void onFinish() {
			mIsCountDownOn = false;
			Message message = Message.obtain(null, Constants.MSG_RECOGNIZER_CANCEL);
			try {
				mServerMessenger.send(message);
				message = Message.obtain(null, Constants.MSG_RECOGNIZER_START_LISTENING);
				mServerMessenger.send(message);
			} catch (RemoteException e) {

			}
		}
	};

	protected class SpeechRecognitionListener implements RecognitionListener {

		@Override
		public void onBeginningOfSpeech() {
			// speech input will be processed, so there is no need for count
			// down anymore
			if (mIsCountDownOn) {
				mIsCountDownOn = false;
				mNoSpeechCountDown.cancel();
			}
		}

		@Override
		public void onBufferReceived(byte[] buffer) {}

		@Override
		public void onEndOfSpeech() {}

		@Override
		public void onError(int error) {
			if (mIsCountDownOn) {
				mIsCountDownOn = false;
				mNoSpeechCountDown.cancel();
			}
			mIsListening = false;
			Message message = Message.obtain(null, Constants.MSG_RECOGNIZER_START_LISTENING);
			
			try {
				mServerMessenger.send(message);
			} catch (RemoteException e) {

			}
			Toast.makeText(getApplicationContext(), "error" + error, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onEvent(int eventType, Bundle params) {

		}

		@Override
		public void onPartialResults(Bundle partialResults) {

		}

		@Override
		public void onReadyForSpeech(Bundle params) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				mIsCountDownOn = true;
				mNoSpeechCountDown.start();
				mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
			}
		}

		@Override
		public void onResults(Bundle results) {
			
			ArrayList<String> strlist = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			for (int i = 0; i < strlist.size(); i++) {
				Log.d("Speech", "result=" + strlist.get(i));
			}
			processCommand(strlist);
//			mSpeechRecognizer= SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
//			SpeechRecognitionListener listener = new SpeechRecognitionListener();
		mSpeechRecognizer.setRecognitionListener(this);

			mSpeechRecognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()));

		}

		@Override
		public void onRmsChanged(float rmsdB) {

		}
		
		private void processCommand(ArrayList<String> matches) {
			if(matches != null) {
				// if call command : The names will be stored here
				ArrayList<String> nameList = null;
				boolean matchFound = false;
				
				Iterator<String> it = matches.iterator();
				while(it.hasNext()) {
					String command = it.next();
					Log.v("Command", command);
					
					// check for call command
					if(command.contains(Constants.CMD_CALL)) {
						// call command
						matchFound = true;
						if(nameList == null)	nameList = new ArrayList<String>();
						if(command.length() > Constants.SUBSTRING_CONST)	nameList.add(command.substring(Constants.SUBSTRING_CONST));
					} else if(command.contains(Constants.CMD_CLOSE)) {
						// close command
						matchFound = true;
					} 
				}
				
				// if not match found
				if(!matchFound) {
					showMessage(Constants.CMD_NOT_RECOGNIZED);
				} else if(nameList != null) {
					// if there are names 
					showSuggestedContacts(nameList);
				}
				
			} else {
				// invalid match
				showMessage(Constants.CMD_NOT_RECOGNIZED);
			}
		}
		
		private void showSuggestedContacts(ArrayList<String> nameList) {
			if(nameList != null) {
				
				Iterator<String> it = nameList.iterator();
				while(it.hasNext()) {
					String name = it.next();
					boolean matchFound = false;
					
					// query contacts
					Cursor c = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
							new String[] {
	                        	ContactsContract.CommonDataKinds.Phone.NUMBER,
	                        	ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
	                        	ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME }, 
	                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE '%"+ name +"%'", null, 
	                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
					
					while(c.moveToNext()) {
						matchFound = true;
						
						// making the call
						showMessage("Calling " + c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
						callPhone(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
						
						break;
						
					}
					
					c.close();
					
					// if no match found
					if(!matchFound) {
						showMessage(Constants.CONTACT_NOT_FOUND);
					}
					
				}
			}
		}

		private void showMessage(String text) {
			Toast.makeText(SpeachRecognitionService.this, text, Toast.LENGTH_SHORT).show();
		}
		
		private void callPhone(String number) {
			if(number != null) {
			
				
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				callIntent.setData(Uri.parse("tel:" + number));
				startActivity(callIntent);
			}
		}

	}

}
