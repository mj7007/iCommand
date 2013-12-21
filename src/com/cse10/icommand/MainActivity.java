package com.cse10.icommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import com.cse10.icommand.R;
import com.cse10.icommand.objects.Contact;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.action_settings) {
			showCommandsGuide();
		}
		return super.onOptionsItemSelected(item);
	}

	private void showCommandsGuide() {
		Dialog commandsDialog = new Dialog(this);
		commandsDialog.setContentView(R.layout.help);
		commandsDialog.setTitle("Commands Guide");
		commandsDialog.show();
		
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.copyFrom(commandsDialog.getWindow().getAttributes());
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		commandsDialog.getWindow().setAttributes(lp);
		
	}

	public void speakButtonClicked(View button) {
		//start the speech recognition intent passing required data
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		//set speech model
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		//message to display while listening
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What's your command?");
        //calling package
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        //start listening
        startActivityForResult(intent, RECOGNIZE_VOICE_REQUEST_CODE);
	}
	
	public void startService(View button) {
		Intent intent = new Intent(getApplicationContext(), SpeechRecognitionService.class);
		startService(intent);
	}
	
	public void stopService(View button) {
		Intent intent = new Intent(getApplicationContext(), SpeechRecognitionService.class);
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
			Vector<Contact> setOfSuggestions = null;
			
			Iterator<String> it = nameList.iterator();
			while(it.hasNext()) {
				String name = it.next();
				
				// query contacts
				Cursor c = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
						new String[] {
                        	ContactsContract.CommonDataKinds.Phone.NUMBER,
                        	ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME }, 
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE '%"+ name +"%'", null, 
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
				
				while(c.moveToNext()) {
					// initialize set if null
					if(setOfSuggestions == null)	setOfSuggestions = new Vector<Contact>();
					
					// obtaining details
					String displayName = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME ));
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
			if(setOfSuggestions == null) {
				showMessage(Constants.CONTACT_NOT_FOUND);
			} else {
				// match found
				// check whether there is only one match
				if(setOfSuggestions.size() == 1) {
					Contact tempContact = setOfSuggestions.get(0);
					showMessage("Calling " + tempContact.getDisplayName());
					callPhone(tempContact.getPhoneNumber());
				} else {
					// promt user to pick one
					showDialogOfSuggestedContacts(setOfSuggestions);
				}
			}
			
		}
	}

	private void showDialogOfSuggestedContacts(final Vector<Contact> vector) {
		if(vector != null) {
			// create content for alert
			ArrayList<String> array = new ArrayList<String>();
			Iterator<Contact> it = vector.iterator();
			while(it.hasNext()) {
				Contact contact = it.next();
				array.add(contact.getDisplayName() + "\n" + contact.getPhoneNumber());
			}
			
			AlertDialog.Builder alertContacts = new AlertDialog.Builder(this);
			alertContacts.setTitle("Suggessted Contacts : Pick One");
			alertContacts.setSingleChoiceItems(array.toArray(new String[]{}), -1, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					
					Contact tempContact = vector.get(which);
					showMessage("Calling " + tempContact.getDisplayName());
					callPhone(tempContact.getPhoneNumber());
				}
			});
			
			alertContacts.create();
			alertContacts.show();
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
