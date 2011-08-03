package org.opendatakit.briefcase.model;

import java.io.File;

import org.opendatakit.briefcase.util.JavaRosaWrapper;
import org.opendatakit.briefcase.util.JavaRosaWrapper.BadFormDefinition;

public class FormDefinition {
	File formFile;
	JavaRosaWrapper formDefn;
	
	public FormDefinition(File formFile) throws BadFormDefinition {
		this.formFile = formFile;
		if ( !formFile.exists() ) {
			throw new BadFormDefinition("Form directory does not contain form");
		}
		formDefn = new JavaRosaWrapper(formFile);
	}

	public String getFormName() {
		return formDefn.getFormName();
	}

	public String getFormId() {
		return formDefn.getSubmissionElementDefn().formId;
	}
	
	public Long getModelVersion() {
		return formDefn.getSubmissionElementDefn().modelVersion;
	}
	
	public Long getUiVersion() {
		return formDefn.getSubmissionElementDefn().uiVersion;
	}
	
	public File getFormDefinitionFile() {
		return formDefn.getFormDefinitionFile();
	}
	
	public String getMD5Hash() {
		return formDefn.getMD5Hash();
	}
}
