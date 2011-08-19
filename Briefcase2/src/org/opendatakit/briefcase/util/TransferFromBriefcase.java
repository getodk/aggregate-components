package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;

public class TransferFromBriefcase implements ITransferFromSourceAction {
	File briefcaseOriginDir; 
	File briefcaseDir;
	List<FormStatus> formsToTransfer;
	
	public TransferFromBriefcase(
			File briefcaseOriginDir, 
			File briefcaseDir, List<FormStatus> formsToTransfer) {
		this.briefcaseOriginDir = briefcaseOriginDir;
		this.briefcaseDir = briefcaseDir;
		this.formsToTransfer = formsToTransfer;
	}

	@Override
	public void doAction() throws IOException {
		File scratch = CommonUtils.clearBriefcaseScratch(briefcaseDir);
		for ( FormStatus fs : formsToTransfer ) {
			
			fs.setStatusString("retrieving form definition");
			EventBus.publish(new FormStatusEvent(fs));

			LocalFormDefinition formDef = (LocalFormDefinition) fs.getFormDefinition();
			File briefcaseFormDefFile = formDef.getFormDefinitionFile();
			String mediaName = briefcaseFormDefFile.getName();
			mediaName = mediaName.substring(0, mediaName.lastIndexOf(".")) + "-media";
			File briefcaseFormMediaDir = new File(briefcaseFormDefFile.getParentFile(), mediaName);
			
			String cleanFormFileName = TransferAction.cleanFormName(fs.getFormName());
			File scratchFormDir = new File(scratch, cleanFormFileName);
			if ( !scratchFormDir.mkdir() ) {
				throw new IOException("unable to create form directory under scratch");
			}
			File scratchFormDefFile = new File(scratchFormDir, cleanFormFileName + ".xml");
			File scratchFormMediaDir = new File(scratchFormDir, cleanFormFileName + "-media");
			
			FileUtils.copyFile(briefcaseFormDefFile, scratchFormDefFile);
			if ( briefcaseFormMediaDir.exists() ) {
				FileUtils.copyDirectory(briefcaseFormMediaDir, scratchFormMediaDir);
			}

			// scratch instances subdirectory...
			File scratchFormInstancesDir = new File(scratchFormDir, "instances");
			if ( !scratchFormInstancesDir.mkdir() ) {
				throw new IOException("unable to create form instances directory under scratch");
			}

			fs.setStatusString("preparing to retrieve instance data");
			EventBus.publish(new FormStatusEvent(fs));

			File briefcaseFormInstancesDir = new File(briefcaseFormDefFile.getParentFile(), "instances");
			if ( briefcaseFormInstancesDir.exists() ) {
				File[] briefcaseFormInstanceDirs = briefcaseFormInstancesDir.listFiles();
				int instanceCount = 1;
				for ( File dir : briefcaseFormInstanceDirs ) {
					
					File xml = new File( dir, "submission.xml");
					if ( xml.exists() ) {
						// OK, we can copy the directory off...
						// Already in briefcase format, so just copy the directory.
						File scratchInstance = new File(scratchFormInstancesDir, TransferAction.cleanFormName(dir.getName()) );
						int i = 2;
						while ( scratchInstance.exists() ) {
							scratchInstance = new File(scratchFormInstancesDir, TransferAction.cleanFormName(dir.getName()) + "-" + Integer.toString(i));
							i++;
						}
						FileUtils.copyDirectory(dir, scratchInstance);
						fs.putScratchFromMapping(scratchInstance, dir);

						if ( (instanceCount-1) % 100 == 0 ) {
							fs.setStatusString(String.format("retrieving (%1$d)", instanceCount));
							EventBus.publish(new FormStatusEvent(fs));
						}
						++instanceCount;
					}
				}
			}
		}
	}

	@Override
	public boolean isSourceDeletable() {
		return true;
	}
}