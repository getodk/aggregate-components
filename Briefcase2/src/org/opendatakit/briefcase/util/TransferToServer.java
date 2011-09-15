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
		for ( FormStatus formToTransfer : formsToTransfer ) {
			String cleanFormFileName = TransferAction.cleanFormName(formToTransfer.getFormName());

			File briefcaseParentFormDestDir = new File(briefcaseDir, "forms");
			if ( !briefcaseParentFormDestDir.exists() ) {
				if ( !briefcaseParentFormDestDir.mkdir() ) {
					throw new IOException("form directory could not be created");
				}
			}
			File briefcaseFormDestDir = new File(briefcaseParentFormDestDir, cleanFormFileName);
			if ( !briefcaseFormDestDir.exists() ) {
				formToTransfer.setStatusString("Folder does not exist");
				continue;
			}
			
			File briefcaseFormDefFile = new File(briefcaseFormDestDir, cleanFormFileName + ".xml");
			File briefcaseFormMediaDir = new File(briefcaseFormDestDir, cleanFormFileName + "-media");
			
			Aggregate10Utils.uploadFormToServerConnection( destServerInfo, briefcaseFormDefFile, briefcaseFormMediaDir );
			
			File briefcaseFormInstancesDir = new File(briefcaseFormDestDir, "instances");
			File[] briefcaseInstances = briefcaseFormInstancesDir.listFiles();
			for ( File briefcaseInstance : briefcaseInstances ) {
				if ( !briefcaseInstance.isDirectory() || briefcaseInstance.getName().startsWith(".") ) {
					formToTransfer.setStatusString("instance directory skipped: " + briefcaseInstance.getName());
					continue;
				}
				Aggregate10Utils.submitInstanceToServerConnection(destServerInfo, briefcaseInstance);
			}
		}
	}
}