package org.opendatakit.briefcase.model;

public class TransferProgressEvent {
	private final String text;
	
	public TransferProgressEvent(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

}
