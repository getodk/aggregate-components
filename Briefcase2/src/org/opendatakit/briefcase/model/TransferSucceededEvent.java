package org.opendatakit.briefcase.model;

import java.util.List;

public class TransferSucceededEvent {

	boolean isDeletableSource;
	private List<FormStatus> formsToTransfer;

	public TransferSucceededEvent(boolean isDeletableSource, List<FormStatus> formsToTransfer) {
		this.isDeletableSource = isDeletableSource;
		this.formsToTransfer = formsToTransfer;
	}
}
