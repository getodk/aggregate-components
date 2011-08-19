package org.opendatakit.briefcase.model;

public class RemoteFormDefinition implements IFormDefinition {

	final String formName;
	final String formId;
	final Integer modelVersion;
	final Integer uiVersion;
	final String downloadUrl;
	final String manifestUrl;
	
	public RemoteFormDefinition( String formName, 
			String formId, Integer modelVersion, Integer uiVersion, 
			String downloadUrl, String manifestUrl ) {
		this.formName = formName;
		this.formId = formId;
		this.modelVersion = modelVersion;
		this.uiVersion = uiVersion;
		this.downloadUrl = downloadUrl;
		this.manifestUrl = manifestUrl;
	}
	
	@Override
	public LocationType getFormLocation() {
		return LocationType.REMOTE;
	}

	@Override
	public String getFormName() {
		return formName;
	}

	@Override
	public String getFormId() {
		return formId;
	}

	@Override
	public Integer getModelVersion() {
		return modelVersion;
	}

	@Override
	public Integer getUiVersion() {
		return uiVersion;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public String getManifestUrl() {
		return manifestUrl;
	}
}
