package org.opendatakit.briefcase.model;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class FormStatus {
	private boolean isSelected = false;
	private IFormDefinition form;
	private final Map<File,File> scratchFromMap = new HashMap<File,File>();
	private final Map<File,File> scratchToMap = new HashMap<File,File>();
	private String statusString = "";
	
	public FormStatus(IFormDefinition form) {
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
	
	public IFormDefinition getFormDefinition() {
		return form;
	}
	
	/**
	 * Only called after the completion of the successful download
	 * of files from a server into briefcase.
	 * @param fd
	 */
	public void setFormDefinition(LocalFormDefinition fd) {
		form = fd;
	}
	
	public void clearInstanceMap() {
		scratchFromMap.clear();
		scratchToMap.clear();
	}
	
	public void putScratchFromMapping(File source, File dest) {
		scratchFromMap.put(source, dest);
	}
	
	public void putScratchToMapping(File source, File dest) {
		scratchToMap.put(source, dest);
	}
	
	public Map<File,File> getFromToMapping() {
		Map<File,File> fromToMap = new HashMap<File,File>();
		for ( File scratch : scratchFromMap.keySet()) {
			File from = scratchFromMap.get(scratch);
			File to = scratchToMap.get(scratch);
			if ( to != null ) {
				fromToMap.put(from, to);
			}
		}
		return fromToMap;
	}
}
