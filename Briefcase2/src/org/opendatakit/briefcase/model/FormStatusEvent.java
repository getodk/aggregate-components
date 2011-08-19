package org.opendatakit.briefcase.model;

public class FormStatusEvent {
	private final FormStatus status;
	
	public FormStatusEvent(FormStatus status) {
		this.status = status;
	}

	public FormStatus getStatus() {
		return status;
	}
}
