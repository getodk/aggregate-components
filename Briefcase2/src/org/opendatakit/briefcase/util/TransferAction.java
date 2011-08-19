package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsSucceededEvent;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.ui.TransferInProgressDialog;

public class TransferAction {

	static final String SCRATCH_DIR = "scratch";

	private static ExecutorService backgroundExecutorService = Executors.newCachedThreadPool();
	
	/**
	 * 
	 * @param formName
	 * @return name without any forward or backward slashes
	 */
	static String cleanFormName(String formName) {
		return formName.replaceAll("[/\\\\]", "");
	}
	
	/**
	 * 
	 * @param formName
	 * @return name without any forward slashes (Android target)
	 */
	static String cleanODKFormName(String formName) {
		return formName.replace("/","");
	}
	
	private static class TransferRunnable implements Runnable {
		ITransferFromSourceAction src;
		ITransferToDestAction dest;
		List<FormStatus> formsToTransfer;
		 
		TransferRunnable( ITransferFromSourceAction src, ITransferToDestAction dest, List<FormStatus> formsToTransfer) {
			this.src = src;
			this.dest = dest;
			this.formsToTransfer = formsToTransfer;
		}
		@Override
		public void run() {
			try {
				src.doAction();
				dest.doAction();
				EventBus.publish(new TransferSucceededEvent(src.isSourceDeletable(), formsToTransfer));
			} catch (Exception e) {
				e.printStackTrace();
				EventBus.publish(new TransferFailedEvent(src.isSourceDeletable(), formsToTransfer, e));
			}
		}
		
	}

	private static void showDialogAndRun( ITransferFromSourceAction src, ITransferToDestAction dest, List<FormStatus> formsToTransfer ) {
		// create the dialog first so that the background task will always have a 
		// listener for its completion events...
		final TransferInProgressDialog dlg = new TransferInProgressDialog("Transfer in Progress...");
		
		backgroundExecutorService.execute(new TransferRunnable(src, dest, formsToTransfer));
		
		dlg.setVisible(true);
	}
	
	private static class RetrieveAvailableFormsRunnable implements Runnable {
		RetrieveAvailableFormsFromServer src;
		 
		RetrieveAvailableFormsRunnable( RetrieveAvailableFormsFromServer src ) {
			this.src = src;
		}
		
		@Override
		public void run() {
			try {
				src.doAction();
				EventBus.publish(new RetrieveAvailableFormsSucceededEvent(src.getAvailableForms()));
			} catch (Exception e) {
				e.printStackTrace();
				EventBus.publish(new RetrieveAvailableFormsFailedEvent(e));
			}
		}
		
	}

	private static void showDialogAndRun( RetrieveAvailableFormsFromServer src ) {
		// create the dialog first so that the background task will always have a 
		// listener for its completion events...
		final TransferInProgressDialog dlg = new TransferInProgressDialog("Fetching Available Forms...");
		
		backgroundExecutorService.execute(new RetrieveAvailableFormsRunnable(src));
		
		dlg.setVisible(true);
	}
	
	public static void retrieveAvailableFormsFromServer(
			ServerConnectionInfo originServerInfo) {
		RetrieveAvailableFormsFromServer source = new RetrieveAvailableFormsFromServer(originServerInfo);
		showDialogAndRun( source);
	}

	public static void transferServerViaToServer(
			ServerConnectionInfo originServerInfo,
			ServerConnectionInfo destinationServerInfo, File briefcaseDir, List<FormStatus> formsToTransfer) throws IOException {

		TransferFromServer source = new TransferFromServer(originServerInfo, briefcaseDir, formsToTransfer);
		TransferToServer dest = new TransferToServer(destinationServerInfo, briefcaseDir, formsToTransfer);
		showDialogAndRun(source, dest, formsToTransfer);
	}

	public static void transferServerViaToBriefcase(
			ServerConnectionInfo originServerInfo, File briefcaseDestDir, File briefcaseDir, List<FormStatus> formsToTransfer) throws IOException {

		TransferFromServer source = new TransferFromServer(originServerInfo, briefcaseDir, formsToTransfer);
		TransferToBriefcase dest = new TransferToBriefcase(briefcaseDestDir, briefcaseDir, formsToTransfer);
		showDialogAndRun(source, dest, formsToTransfer);
	}

	public static void transferODKViaToServer(File odkSrcDir,
			ServerConnectionInfo destinationServerInfo, File briefcaseDir, List<FormStatus> formsToTransfer) throws IOException {

		TransferFromODK source = new TransferFromODK(odkSrcDir, briefcaseDir, formsToTransfer);
		TransferToServer dest = new TransferToServer(destinationServerInfo, briefcaseDir, formsToTransfer);
		showDialogAndRun(source, dest, formsToTransfer);
	}

	public static void transferODKViaToBriefcase(File odkSrcDir, File briefcaseDestDir,
			File briefcaseDir, List<FormStatus> formsToTransfer) throws IOException {

		TransferFromODK source = new TransferFromODK(odkSrcDir, briefcaseDir, formsToTransfer);
		TransferToBriefcase dest = new TransferToBriefcase(briefcaseDestDir, briefcaseDir, formsToTransfer);
		showDialogAndRun(source, dest, formsToTransfer);
	}

	public static void transferBriefcaseViaToServer(File briefcaseSrcDir,
			ServerConnectionInfo destinationServerInfo, File briefcaseDir, List<FormStatus> formsToTransfer) throws IOException {

		TransferFromBriefcase source = new TransferFromBriefcase(briefcaseSrcDir, briefcaseDir, formsToTransfer);
		TransferToServer dest = new TransferToServer(destinationServerInfo, briefcaseDir, formsToTransfer);
		showDialogAndRun(source, dest, formsToTransfer);
	}

	public static void transferBriefcaseViaToBriefcase(final File briefcaseSrcDir, final File briefcaseDestDir,
			final File briefcaseDir, final List<FormStatus> formsToTransfer) throws IOException {

		TransferFromBriefcase source = new TransferFromBriefcase(briefcaseSrcDir, briefcaseDir, formsToTransfer);
		TransferToBriefcase dest = new TransferToBriefcase(briefcaseDestDir, briefcaseDir, formsToTransfer);
		showDialogAndRun(source, dest, formsToTransfer);
	}
}
