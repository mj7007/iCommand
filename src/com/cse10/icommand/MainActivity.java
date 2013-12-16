package com.cse10.icommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.cse10.icommand.R;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final int RECOGNIZE_VOICE_REQUEST_CODE = 0;
	
    private Button speakButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		 
		// initialization
		speakButton = (Button) findViewById(R.id.speakButton);
		
		// Disable button if no recognition service is present
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            speakButton.setEnabled(false);
            speakButton.setText("Recognizer not present");
        }
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void speakButtonClicked(View button) {
		//start the speech recognition intent passing required data
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		//set speech model
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		//message to display while listening
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What's your command?");
        //start listening
        startActivityForResult(intent, RECOGNIZE_VOICE_REQUEST_CODE);
	}
	
	public void startService(View button) {
		Intent intent = new Intent(getApplicationContext(), SpeachRecognitionService.class);
		startService(intent);
	}
	
	public void stopService(View button) {
		Intent intent = new Intent(getApplicationContext(), SpeachRecognitionService.class);
		stopService(intent);
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
        if (requestCode == RECOGNIZE_VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
        	
            // Populate the wordsList with the String values the recognition engine thought it heard
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            processCommand(matches);
            
        }
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
					finish();
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
		Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
	}
	
	private void callPhone(String number) {
		if(number != null) {
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.setData(Uri.parse("tel:" + number));
			startActivity(callIntent);
		}
	}

}
