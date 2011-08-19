package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;

public class TransferFromServer implements ITransferFromSourceAction {
	
	ServerConnectionInfo originServerInfo; 
	File briefcaseDir;
	List<FormStatus> formsToTransfer;
	
	public TransferFromServer(
			ServerConnectionInfo originServerInfo, 
			File briefcaseDir, List<FormStatus> formsToTransfer) {
		this.originServerInfo = originServerInfo;
		this.briefcaseDir = briefcaseDir;
		this.formsToTransfer = formsToTransfer;
	}

	@Override
	public void doAction() throws IOException {
		
		// the scratch directory is cleared before fetching the list of
		// forms and their definitions down to the local machine.
		File scratch = CommonUtils.clearBriefcaseScratch(briefcaseDir);
		ServerFormListFetcher fetcher =	new ServerFormListFetcher(originServerInfo);
		fetcher.downloadFiles(briefcaseDir, formsToTransfer);
	}

	@Override
	public boolean isSourceDeletable() {
		return false;
	}
	
}