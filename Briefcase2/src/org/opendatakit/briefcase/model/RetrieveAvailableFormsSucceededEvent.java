package org.opendatakit.briefcase.model;

import java.util.List;

/**
 * Signals the completion of the retrieval of forms to display
 * as available from a server.  The transfer of submissions is 
 * signaled by the TransferXXXEvent classes.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class RetrieveAvailableFormsSucceededEvent {
	private List<FormStatus> formsToTransfer;

	public RetrieveAvailableFormsSucceededEvent(List<FormStatus> formsToTransfer) {
		this.formsToTransfer = formsToTransfer;
	}

	public List<FormStatus> getFormsToTransfer() {
		return formsToTransfer;
	}

}
