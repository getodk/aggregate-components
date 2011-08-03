package org.opendatakit.briefcase.model;

public enum EndPointType {

	AGGREGATE_0_9_X_CHOICE,
	AGGREGATE_1_0_CHOICE,
	BRIEFCASE_CHOICE,
	OTHER_LOCAL_BRIEFCASE_CHOICE,
	MOUNTED_ODK_COLLECT_DEVICE_CHOICE;
	
	public String toString() {
		switch ( this ) {
		case AGGREGATE_0_9_X_CHOICE:
			return "Aggregate 0.9.x";
		case AGGREGATE_1_0_CHOICE:
			return "Aggregate 1.0";
		case BRIEFCASE_CHOICE:
			return "This Briefcase Directory";
		case OTHER_LOCAL_BRIEFCASE_CHOICE:
			return "Other Local Briefcase Directory";
		case MOUNTED_ODK_COLLECT_DEVICE_CHOICE:
			return "Mounted ODK Collect Device Directory";
		}
		throw new IllegalStateException("Unhandled EndPointType value");
	}
	
	public static EndPointType fromString(String toStringValue) {
		EndPointType[] types = EndPointType.values();
		for ( EndPointType t : types ) {
			if ( t.toString().equals(toStringValue) ) {
				return t;
			}
		}
		return null;
	}
}
