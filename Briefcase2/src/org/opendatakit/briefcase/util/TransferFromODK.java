package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;

public class TransferFromODK implements ITransferFromSourceAction {
	
	File odkOriginDir;
	File briefcaseDir;
	List<FormStatus> formsToTransfer;
	
	public TransferFromODK(	File odkOriginDir, 
			File briefcaseDir, List<FormStatus> formsToTransfer) {
		this.odkOriginDir = odkOriginDir;
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
			File odkFormDefFile = formDef.getFormDefinitionFile();
			
			// compose the ODK media directory...
			final String odkFormName = odkFormDefFile.getName().substring(0, odkFormDefFile.getName().lastIndexOf("."));
			String odkMediaName = odkFormName + "-media";
			File odkFormMediaDir = new File(odkFormDefFile.getParentFile(), odkMediaName);
			
			// create the path for the briefcase (scratch) tree...
			String cleanFormFileName = TransferAction.cleanFormName(fs.getFormName());
			File scratchFormDir = new File(scratch, cleanFormFileName);
			if ( !scratchFormDir.mkdir() ) {
				throw new IOException("unable to create form directory under scratch");
			}
			File scratchFormDefFile = new File(scratchFormDir, cleanFormFileName + ".xml");
			File scratchFormMediaDir = new File(scratchFormDir, cleanFormFileName + "-media");
			
			// copy form definition files from ODK to briefcase (scratch area)
			FileUtils.copyFile(odkFormDefFile, scratchFormDefFile);
			if ( odkFormMediaDir.exists() ) {
				FileUtils.copyDirectory(odkFormMediaDir, scratchFormMediaDir);
			}

			// scratch instances subdirectory...
			File scratchFormInstancesDir = new File(scratchFormDir, "instances");
			if ( !scratchFormInstancesDir.mkdir() ) {
				throw new IOException("unable to create form instances directory under scratch");
			}

			fs.setStatusString("preparing to retrieve instance data");
			EventBus.publish(new FormStatusEvent(fs));
			
			// construct up the list of folders that might have ODK form data.
			File odkFormInstancesDir = new File( odkFormDefFile.getParentFile().getParentFile(), "instances");
			// rely on ODK naming conventions to identify form data files...
			File[] odkFormInstanceDirs = odkFormInstancesDir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.getName().startsWith(odkFormName + "-");
				}});
			
			int instanceCount = 1;
			for ( File dir : odkFormInstanceDirs ) {
			
				File xml = new File( dir, dir.getName() + ".xml");
				if ( xml.exists() ) {
					// OK, we can copy the directory off...
					// Briefcase instances directory name is arbitrary.
					// Rename the xml within that to always be "submission.xml"
					// to remove the correspondence to the directory name.
					File scratchInstance = new File(scratchFormInstancesDir, TransferAction.cleanFormName(dir.getName()) );
					int i = 2;
					while ( scratchInstance.exists() ) {
						scratchInstance = new File(scratchFormInstancesDir, TransferAction.cleanFormName(dir.getName()) + "-" + Integer.toString(i));
						i++;
					}
					FileUtils.copyDirectory(dir, scratchInstance);
					File odkSubmissionFile = new File( scratchInstance, dir.getName() + ".xml");
					File scratchSubmissionFile = new File( scratchInstance, "submission.xml");
					
					FileUtils.moveFile(odkSubmissionFile, scratchSubmissionFile);
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

	@Override
	public boolean isSourceDeletable() {
		return true;
	}
}