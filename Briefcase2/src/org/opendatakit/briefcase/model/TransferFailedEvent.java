package org.opendatakit.briefcase.model;

import java.util.List;

public class TransferFailedEvent {
	boolean isDeletableSource;
	List<FormStatus> formsToTransfer;
	Exception e;
	
	public TransferFailedEvent(boolean isDeletableSource, List<FormStatus> formsToTransfer, Exception e) {
		this.isDeletableSource = isDeletableSource;
		this.formsToTransfer = formsToTransfer;
		this.e = e;
	}
}
