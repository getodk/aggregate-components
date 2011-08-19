package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;

public class TransferToBriefcase implements ITransferToDestAction {
	File briefcaseDestDir;
	File briefcaseDir;
	List<FormStatus> formsToTransfer;

	public TransferToBriefcase(
			File briefcaseDestDir, 
			File briefcaseDir, List<FormStatus> formsToTransfer) {
		this.briefcaseDestDir = briefcaseDestDir;
		this.briefcaseDir = briefcaseDir;
		this.formsToTransfer = formsToTransfer;
	}

	@Override
	public void doAction() throws IOException {

		File scratch = new File(briefcaseDir, TransferAction.SCRATCH_DIR);
		for ( FormStatus fs : formsToTransfer ) {
			
			fs.setStatusString("copying form definition");
			EventBus.publish(new FormStatusEvent(fs));

			String cleanFormFileName = TransferAction.cleanFormName(fs.getFormName());
			File scratchFormDir = new File(scratch, cleanFormFileName);
			if ( !scratchFormDir.exists() ) {
				throw new IOException("form directory under scratch does not exist");
			}
			File scratchFormDefFile = new File(scratchFormDir, cleanFormFileName + ".xml");
			File scratchFormMediaDir = new File(scratchFormDir, cleanFormFileName + "-media");

			File briefcaseParentFormDestDir = new File(briefcaseDestDir, "forms");
			if ( !briefcaseParentFormDestDir.exists() ) {
				if ( !briefcaseParentFormDestDir.mkdir() ) {
					throw new IOException("form directory could not be created");
				}
			}
			File briefcaseFormDestDir = new File(briefcaseParentFormDestDir, cleanFormFileName);
			if ( !briefcaseFormDestDir.exists() ) {
				if ( !briefcaseFormDestDir.mkdir() ) {
					throw new IOException(cleanFormFileName + " directory could not be created");
				}
			}
			File briefcaseFormDefFile = new File(briefcaseFormDestDir, cleanFormFileName + ".xml");
			File briefcaseFormMediaDir = new File(briefcaseFormDestDir, cleanFormFileName + "-media");
			
			FileUtils.copyFile(scratchFormDefFile, briefcaseFormDefFile);
			if ( scratchFormMediaDir.exists() ) {
				File[] files = scratchFormMediaDir.listFiles();
				if ( files != null && files.length != 0 ) {
					FileUtils.copyDirectory(scratchFormMediaDir, briefcaseFormMediaDir);
				}
			}

			File briefcaseInstancesDestDir = new File(briefcaseFormDestDir, "instances");
			if ( !briefcaseInstancesDestDir.exists() ) {
				if ( !briefcaseInstancesDestDir.mkdir() ) {
					throw new IOException(cleanFormFileName + " instances directory could not be created");
				}
			}
			
			fs.setStatusString("preparing to copy instance data");
			EventBus.publish(new FormStatusEvent(fs));
			
			File scratchFormInstancesDir = new File(scratchFormDir, "instances");
			if ( scratchFormInstancesDir.exists()) {
				File[] scratchInstanceDirs = scratchFormInstancesDir.listFiles();

				int instanceCount = 1;
				for ( File dir : scratchInstanceDirs ) {
					File xml = new File( dir, "submission.xml");
					if ( xml.exists() ) {
						// OK, we can copy the directory off...
						// Already in briefcase format, so just copy the directory.
						File briefcaseInstance = new File(briefcaseInstancesDestDir, TransferAction.cleanFormName(dir.getName()) );
						int i = 2;
						while ( briefcaseInstance.exists() ) {
							briefcaseInstance = new File(briefcaseInstancesDestDir, TransferAction.cleanFormName(dir.getName()) + "-" + Integer.toString(i));
							i++;
						}
						FileUtils.copyDirectory(dir, briefcaseInstance);
						fs.putScratchToMapping(dir, briefcaseInstance);

						if ( (instanceCount-1) % 100 == 0 ) {
							fs.setStatusString(String.format("copying (%1$d)", instanceCount));
							EventBus.publish(new FormStatusEvent(fs));
						}
						++instanceCount;
					}
				}
			}
			
			fs.setStatusString("copy completed");
			EventBus.publish(FormStatusEvent.class, new FormStatusEvent(fs));
		}
		
	}
}