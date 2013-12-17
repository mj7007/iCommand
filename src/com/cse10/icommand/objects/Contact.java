package com.cse10.icommand.objects;

public class Contact {

	private String displayName;
	private String phoneNumber;

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

}
