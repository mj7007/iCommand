package com.cse10.icommand;

import java.util.ArrayList;
import com.cse10.icommand.objects.Contact;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class SuggestedContactsDialog extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		// obtain args
		Bundle bundle = getIntent().getExtras();
		if(bundle != null) {
			ArrayList<Contact> content = bundle.getParcelableArrayList(Constants.CONTACTS);
			showDialogOfSuggestedContacts(content);
		}
		
	}

	private void showDialogOfSuggestedContacts(final ArrayList<Contact> content) {
		if(content != null) {			
			AlertDialog.Builder alertContacts = new AlertDialog.Builder(getApplicationContext());
			alertContacts.setTitle("Suggessted Contacts : Pick One");
			alertContacts.setSingleChoiceItems(content.toArray(new String[]{}), -1, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					
					Contact tempContact = content.get(which);
					Toast.makeText(getApplicationContext(), "Calling " + tempContact.getDisplayName(), Toast.LENGTH_SHORT).show();
					callPhone(tempContact.getPhoneNumber());
					
				}
			});
			
			alertContacts.create();
			alertContacts.show();
		}
	}
	
	private void callPhone(String number) {
		if(number != null) {
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.setData(Uri.parse("tel:" + number));
			startActivity(callIntent);
			finish();
		}
	}
	
}
