package org.opendatakit.briefcase.model;

public interface IFormDefinition {

	enum LocationType {
		LOCAL,
		REMOTE
	};
	
	public abstract LocationType getFormLocation();
	
	public abstract String getFormName();

	public abstract String getFormId();

	public abstract Integer getModelVersion();

	public abstract Integer getUiVersion();

}