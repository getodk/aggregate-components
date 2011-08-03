package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.opendatakit.briefcase.model.FormDefinition;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.ui.FormTransferTable;

public class TransferAction {

	private static final String SCRATCH_DIR = "scratch";

	private static File clearBriefcaseScratch(File briefcaseDir) throws IOException {
	
		File scratch = new File(briefcaseDir, SCRATCH_DIR);
		if ( scratch.exists() ) {
			FileUtils.deleteDirectory(scratch);
		}
		if ( !scratch.mkdir() ) {
			throw new IOException("Unable to clear scratch space");
		}
		return scratch;
	}
	
	/**
	 * 
	 * @param formName
	 * @return name without any forward or backward slashes
	 */
	private static String cleanFormName(String formName) {
		return formName.replaceAll("[/\\\\]", "");
	}
	
	/**
	 * 
	 * @param formName
	 * @return name without any forward slashes (Android target)
	 */
	private static String cleanODKFormName(String formName) {
		return formName.replace("/","");
	}
	private static void transferFromServer(
			ServerConnectionInfo originServerInfo, 
			File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {
		File scratch = clearBriefcaseScratch(briefcaseDir);
		// TODO: logic to pull down form definition from server...
	}

	private static void transferFromODK(
			File odkOriginDir, 
			File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {
		File scratch = clearBriefcaseScratch(briefcaseDir);
		for ( FormStatus fs : formsToTransfer ) {
			FormDefinition formDef = fs.getFormDefinition();
			File fdf = formDef.getFormDefinitionFile();
			String mediaName = fdf.getName();
			mediaName = mediaName.substring(0, mediaName.lastIndexOf(".")) + "-media";
			File fdMedia = new File(fdf.getParent(), mediaName);
			
			String cleanFormFileName = cleanFormName(fs.getFormName());
			File briefcaseFormDir = new File(scratch, cleanFormFileName);
			if ( !briefcaseFormDir.mkdir() ) {
				throw new IOException("unable to create form directory under scratch");
			}
			File formFile = new File(briefcaseFormDir, cleanFormFileName + ".xml");
			File formMediaDir = new File(briefcaseFormDir, cleanFormFileName + "-media");
			
			FileUtils.copyFile(fdf, formFile);
			if ( fdMedia.exists() ) {
				FileUtils.copyDirectory(fdMedia, formMediaDir);
			}
			
			// TODO: pull down instances
		}
	}

	private static void transferFromBriefcase(
			File briefcaseOriginDir, 
			File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {
		File scratch = clearBriefcaseScratch(briefcaseDir);
		for ( FormStatus fs : formsToTransfer ) {
			FormDefinition formDef = fs.getFormDefinition();
			File fdf = formDef.getFormDefinitionFile();
			String mediaName = fdf.getName();
			mediaName = mediaName.substring(0, mediaName.lastIndexOf(".")) + "-media";
			File fdMedia = new File(fdf.getParent(), mediaName);
			
			String cleanFormFileName = cleanFormName(fs.getFormName());
			File briefcaseFormDir = new File(scratch, cleanFormFileName);
			if ( !briefcaseFormDir.mkdir() ) {
				throw new IOException("unable to create form directory under scratch");
			}
			File formFile = new File(briefcaseFormDir, cleanFormFileName + ".xml");
			File formMediaDir = new File(briefcaseFormDir, cleanFormFileName + "-media");
			
			FileUtils.copyFile(fdf, formFile);
			if ( fdMedia.exists() ) {
				FileUtils.copyDirectory(fdMedia, formMediaDir);
			}
			
			// TODO: pull down instances
		}
	}

	private static void transferToServer(
			ServerConnectionInfo destServerInfo, 
			File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) {
		
		// TODO: ...
		
	}

	private static void transferToODK(
			File odkDestDir, 
			File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		File scratch = new File(briefcaseDir, SCRATCH_DIR);
		for ( FormStatus fs : formsToTransfer ) {
			String cleanFormFileName = cleanFormName(fs.getFormName());
			File briefcaseFormDir = new File(scratch, cleanFormFileName);
			if ( !briefcaseFormDir.exists() ) {
				throw new IOException("form directory under scratch does not exist");
			}
			File formFile = new File(briefcaseFormDir, cleanFormFileName + ".xml");
			File formMediaDir = new File(briefcaseFormDir, cleanFormFileName + "-media");

			String cleanODKFormName = cleanODKFormName(fs.getFormName());
			File odkForms = new File(odkDestDir, "forms");
			File fdf = new File(odkForms, cleanODKFormName + ".xml");
			File fdMedia = new File(odkForms, cleanODKFormName + "-media");
			
			FileUtils.copyFile(formFile, fdf);
			if ( formMediaDir.exists() ) {
				File[] files = formMediaDir.listFiles();
				if ( files != null && files.length != 0 ) {
					FileUtils.copyDirectory(formMediaDir, fdMedia);
				}
			}
			
			// TODO: push down instances
		}
	}

	private static void transferToBriefcase(
			File briefcaseDestDir, 
			File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		File scratch = new File(briefcaseDir, SCRATCH_DIR);
		for ( FormStatus fs : formsToTransfer ) {
			String cleanFormFileName = cleanFormName(fs.getFormName());
			File briefcaseFormDir = new File(scratch, cleanFormFileName);
			if ( !briefcaseFormDir.exists() ) {
				throw new IOException("form directory under scratch does not exist");
			}
			File formFile = new File(briefcaseFormDir, cleanFormFileName + ".xml");
			File formMediaDir = new File(briefcaseFormDir, cleanFormFileName + "-media");

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
			File fdf = new File(briefcaseFormDestDir, cleanFormFileName + ".xml");
			File fdMedia = new File(briefcaseFormDestDir, cleanFormFileName + "-media");
			
			FileUtils.copyFile(formFile, fdf);
			if ( formMediaDir.exists() ) {
				File[] files = formMediaDir.listFiles();
				if ( files != null && files.length != 0 ) {
					FileUtils.copyDirectory(formMediaDir, fdMedia);
				}
			}
			
			// TODO: push down instances
		}
		
	}
	
	public static void transferServerViaToServer(
			ServerConnectionInfo originServerInfo,
			ServerConnectionInfo destinationServerInfo, File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager ) throws IOException {

		transferFromServer(originServerInfo, briefcaseDir, formsToTransfer, progressManager);
		transferToServer(destinationServerInfo, briefcaseDir, formsToTransfer, progressManager);
	}

	public static void transferServerViaToODK(
			ServerConnectionInfo originServerInfo, File odkDestDir, File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		transferFromServer(originServerInfo, briefcaseDir, formsToTransfer, progressManager);
		transferToODK(odkDestDir, briefcaseDir, formsToTransfer, progressManager);
	}

	public static void transferServerViaToBriefcase(
			ServerConnectionInfo originServerInfo, File briefcaseDestDir, File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		transferFromServer(originServerInfo, briefcaseDir, formsToTransfer, progressManager);
		transferToBriefcase(briefcaseDestDir, briefcaseDir, formsToTransfer, progressManager);
	}

	public static void transferODKViaToServer(File odkSrcDir,
			ServerConnectionInfo destinationServerInfo, File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		transferFromODK(odkSrcDir, briefcaseDir, formsToTransfer, progressManager);
		transferToServer(destinationServerInfo, briefcaseDir, formsToTransfer, progressManager);
	}

	public static void transferODKViaToODK(File odkSrcDir, File odkDestDir, File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		transferFromODK(odkSrcDir, briefcaseDir, formsToTransfer, progressManager);
		transferToODK(odkDestDir, briefcaseDir, formsToTransfer, progressManager);
	}

	public static void transferODKViaToBriefcase(File odkSrcDir, File briefcaseDestDir,
			File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		transferFromODK(odkSrcDir, briefcaseDir, formsToTransfer, progressManager);
		transferToBriefcase(briefcaseDestDir, briefcaseDir, formsToTransfer, progressManager);
	}

	public static void transferBriefcaseViaToServer(File briefcaseSrcDir,
			ServerConnectionInfo destinationServerInfo, File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		transferFromBriefcase(briefcaseSrcDir, briefcaseDir, formsToTransfer, progressManager);
		transferToServer(destinationServerInfo, briefcaseDir, formsToTransfer, progressManager);
	}

	public static void transferBriefcaseViaToODK(File briefcaseSrcDir, File odkDestDir,
			File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		transferFromBriefcase(briefcaseSrcDir, briefcaseDir, formsToTransfer, progressManager);
		transferToODK(odkDestDir, briefcaseDir, formsToTransfer, progressManager);
	}

	public static void transferBriefcaseViaToBriefcase(File briefcaseSrcDir, File briefcaseDestDir,
			File briefcaseDir, List<FormStatus> formsToTransfer, FormTransferTable progressManager) throws IOException {

		transferFromBriefcase(briefcaseSrcDir, briefcaseDir, formsToTransfer, progressManager);
		transferToBriefcase(briefcaseDestDir, briefcaseDir, formsToTransfer, progressManager);
	}

}
