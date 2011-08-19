package org.opendatakit.briefcase.model;

import java.io.File;

import org.opendatakit.briefcase.util.JavaRosaWrapper;
import org.opendatakit.briefcase.util.JavaRosaWrapper.BadFormDefinition;

public class LocalFormDefinition implements IFormDefinition {
	File formFile;
	JavaRosaWrapper formDefn;
	
	public LocalFormDefinition(File formFile) throws BadFormDefinition {
		this.formFile = formFile;
		if ( !formFile.exists() ) {
			throw new BadFormDefinition("Form directory does not contain form");
		}
		formDefn = new JavaRosaWrapper(formFile);
	}

	/* (non-Javadoc)
	 * @see org.opendatakit.briefcase.model.IFormDefinition#getFormName()
	 */
	@Override
	public String getFormName() {
		return formDefn.getFormName();
	}

	/* (non-Javadoc)
	 * @see org.opendatakit.briefcase.model.IFormDefinition#getFormId()
	 */
	@Override
	public String getFormId() {
		return formDefn.getSubmissionElementDefn().formId;
	}
	
	/* (non-Javadoc)
	 * @see org.opendatakit.briefcase.model.IFormDefinition#getModelVersion()
	 */
	@Override
	public Integer getModelVersion() {
		Long l = formDefn.getSubmissionElementDefn().modelVersion;
		if ( l == null ) return null;
		return l.intValue();
	}
	
	/* (non-Javadoc)
	 * @see org.opendatakit.briefcase.model.IFormDefinition#getUiVersion()
	 */
	@Override
	public Integer getUiVersion() {
		Long l = formDefn.getSubmissionElementDefn().uiVersion;
		if ( l == null ) return null;
		return l.intValue();
	}
	
	public File getFormDefinitionFile() {
		return formDefn.getFormDefinitionFile();
	}
	
	public String getMD5Hash() {
		return formDefn.getMD5Hash();
	}

	@Override
	public LocationType getFormLocation() {
		return LocationType.LOCAL;
	}
}
