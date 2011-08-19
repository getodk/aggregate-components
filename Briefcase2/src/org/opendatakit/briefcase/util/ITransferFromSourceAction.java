package org.opendatakit.briefcase.util;

import java.io.IOException;

interface ITransferFromSourceAction {
	void doAction() throws IOException;
	
	boolean isSourceDeletable();
}