package org.opendatakit.briefcase.model;


public class FormStatus {
	private boolean isSelected = false;
	private final FormDefinition form;
	private String statusString = "";
	
	public FormStatus(FormDefinition form) {
		this.form = form;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public String getStatusString() {
		return statusString;
	}

	public void setStatusString(String statusString) {
		this.statusString = statusString;
	}

	public String getFormName() {
		return form.getFormName();
	}
	
	public FormDefinition getFormDefinition() {
		return form;
	}
}
