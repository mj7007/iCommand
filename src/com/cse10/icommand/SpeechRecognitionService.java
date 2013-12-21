package com.cse10.icommand;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import com.android.internal.telephony.ITelephony;
import com.cse10.icommand.objects.Contact;
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
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class SpeechRecognitionService extends Service {

	private static final String TAG = "SpeechRecognitionService";
	
	protected AudioManager mAudioManager;
	protected SpeechRecognizer mSpeechRecognizer;
	protected Intent mSpeechRecognizerIntent;
	protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

	protected boolean mIsListening;
	protected volatile boolean mIsCountDownOn;

	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(this, "Speach Recognition Service started", Toast.LENGTH_SHORT).show();
		Log.v(TAG, "OnCreate");
		
		// initialization
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
 
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "Speach Recognition Service started", Toast.LENGTH_SHORT).show();
		Log.v(TAG, "OnStartCommand");
		
		int result = super.onStartCommand(intent, flags, startId);
		
		mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
		mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
		startListening();
		
		return result;

	}

	private void startListening() {
		mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
		mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
	}

	@Override
	public void onDestroy() {
		Toast.makeText(getApplicationContext(), "Speach Recognition Service destroyed", Toast.LENGTH_LONG).show();
		Log.v(TAG, "OnDestroy");
		
		if (mIsCountDownOn) {
			mNoSpeechCountDown.cancel();
		}
		
		if (mSpeechRecognizer != null) {
			mSpeechRecognizer.stopListening();
			mSpeechRecognizer.cancel();
			mSpeechRecognizer.destroy();
		}
		
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000) {

		@Override
		public void onTick(long millisUntilFinished) {}

		@Override
		public void onFinish() {
			mIsCountDownOn = false;
			
			Message message = Message.obtain(null,Constants.MSG_RECOGNIZER_CANCEL);
			
			try {
				mServerMessenger.send(message);
				message = Message.obtain(null, Constants.MSG_RECOGNIZER_START_LISTENING);
				mServerMessenger.send(message);
			} catch (RemoteException e) {}
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
			
			Log.v(TAG, "error" + error);
			
		}

		@Override
		public void onEvent(int eventType, Bundle params) {}

		@Override
		public void onPartialResults(Bundle partialResults) {}

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
			processCommand(strlist);
			
			startListening();
			
		}

		@Override
		public void onRmsChanged(float rmsdB) {}

		private void processCommand(ArrayList<String> matches) {
			if (matches != null) {
				// if call command : The names will be stored here
				ArrayList<String> nameList = null;
				boolean matchFound = false;

				Iterator<String> it = matches.iterator();
				while (it.hasNext()) {
					String command = it.next();
					Log.v("Command", command);

					// check for call command
					if (command.contains(Constants.CMD_CALL)) {
						// call command
						matchFound = true;
						if (nameList == null)
							nameList = new ArrayList<String>();
						if (command.length() > Constants.SUBSTRING_CONST)
							nameList.add(command
									.substring(Constants.SUBSTRING_CONST));
					} else if(command.contains(Constants.CMD_REJECT)) {
						matchFound = true;
						rejectCall();
					} else if(command.contains(Constants.CMD_ANSWER)) {
						matchFound = true;
						
					} else if (command.contains(Constants.CMD_CLOSE)) {
						// close command
						matchFound = true;
					}
				}

				// if not match found
				if (!matchFound) {
					showMessage(Constants.CMD_NOT_RECOGNIZED);
				} else if (nameList != null) {
					// if there are names
					showSuggestedContacts(nameList);
				}

			} else {
				// invalid match
				showMessage(Constants.CMD_NOT_RECOGNIZED);
			}
		}

		private void showSuggestedContacts(ArrayList<String> nameList) {
			if (nameList != null) {
				Vector<Contact> setOfSuggestions = null;

				Iterator<String> it = nameList.iterator();
				while (it.hasNext()) {
					String name = it.next();

					// query contacts
					Cursor c = getContentResolver()
							.query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
									new String[] {
											ContactsContract.CommonDataKinds.Phone.NUMBER,
											ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME },
									ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
											+ " LIKE '%" + name + "%'",
									null,
									ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

					while (c.moveToNext()) {
						// initialize set if null
						if (setOfSuggestions == null)
							setOfSuggestions = new Vector<Contact>();

						// obtaining details
						String displayName = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
						String phoneNumber = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

						Log.v("Name", displayName + " - " + phoneNumber);

						// create new contact
						Contact contact = new Contact();
						contact.setDisplayName(displayName);
						contact.setPhoneNumber(phoneNumber);
						setOfSuggestions.add(contact);

					}

					c.close();
				}

				// if no match found
				if (setOfSuggestions == null) {
					showMessage(Constants.CONTACT_NOT_FOUND);
				} else {
					// match found
					// check whether there is only one match
					if (setOfSuggestions.size() == 1) {
						Contact tempContact = setOfSuggestions.get(0);
						showMessage("Calling " + tempContact.getDisplayName());
						callPhone(tempContact.getPhoneNumber());

					} else {
						// promt user to pick one

						// creating new intent to display dialog
						Bundle bundle = new Bundle();
						bundle.putParcelableArrayList(Constants.CONTACTS, new ArrayList<Contact>(setOfSuggestions));
						Intent intent = new Intent(getApplicationContext(), SuggestedContactsDialog.class);
						intent.putExtras(bundle);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);

					}
				}

			}
		}

		private void showMessage(String text) {
			Toast.makeText(SpeechRecognitionService.this, text,
					Toast.LENGTH_SHORT).show();
		}

		private void callPhone(String number) {
			if (number != null) {
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				callIntent.setData(Uri.parse("tel:" + number));
				startActivity(callIntent);
			}
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private void rejectCall() {
			try {
				TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				Class c = Class.forName(telephony.getClass().getName());
				Method m = c.getDeclaredMethod("getITelephony");
				m.setAccessible(true);
				ITelephony telephonyService;telephonyService = (ITelephony) m.invoke(telephony);
				telephonyService.endCall();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
