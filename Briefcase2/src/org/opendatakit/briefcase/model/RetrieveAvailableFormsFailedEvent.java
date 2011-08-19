package org.opendatakit.briefcase.model;

public class RetrieveAvailableFormsFailedEvent {
	private Exception e;

	public RetrieveAvailableFormsFailedEvent( Exception e ) {
		this.e = e;
	}

	public String getReason() {
		if ( e != null ) {
			return "Exception: " + e.getMessage();
		} else {
			return "unknown";
		}
	}
}
