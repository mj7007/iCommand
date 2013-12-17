package com.cse10.icommand.objects;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable {

	private String displayName;
	private String phoneNumber;

	public Contact() {}
	
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Override
	public boolean equals(Object contact) {
		return ((Contact)contact).getPhoneNumber().equals(phoneNumber);
	}

	@Override
	public int hashCode() {
		return phoneNumber.hashCode();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(displayName);
		dest.writeString(phoneNumber);
	}
	
	// this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
	public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {

		@Override
		public Contact createFromParcel(Parcel source) {
			return new Contact(source);
		}

		@Override
		public Contact[] newArray(int size) {
			return new Contact[size];
		}
	};
	
	// constructor that takes a Parcel and gives you an object populated with it's values
	public Contact(Parcel parcel) {
		displayName = parcel.readString();
		phoneNumber = parcel.readString();
	}

}
