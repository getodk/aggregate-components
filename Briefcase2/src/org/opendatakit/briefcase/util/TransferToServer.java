package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;

public class TransferToServer implements ITransferToDestAction {
	ServerConnectionInfo destServerInfo; 
	File briefcaseDir;
	List<FormStatus> formsToTransfer;

	public TransferToServer(
			ServerConnectionInfo destServerInfo, 
			File briefcaseDir, List<FormStatus> formsToTransfer) {
		this.destServerInfo = destServerInfo;
		this.briefcaseDir = briefcaseDir;
		this.formsToTransfer = formsToTransfer;
	}

	@Override
	public void doAction() throws IOException {
		// TODO Auto-generated method stub
		
	}
}