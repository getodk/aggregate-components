/*
 * Copyright (C) 2011 University of Washington.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcasePreferences;
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

  private static class TransferRunnable implements Runnable {
    ITransferFromSourceAction src;
    ITransferToDestAction dest;
    List<FormStatus> formsToTransfer;

    TransferRunnable(ITransferFromSourceAction src, ITransferToDestAction dest,
        List<FormStatus> formsToTransfer) {
      this.src = src;
      this.dest = dest;
      this.formsToTransfer = formsToTransfer;
    }

    @Override
    public void run() {
      try {
        boolean allSuccessful = true;
        if (src != null) {
          allSuccessful = allSuccessful & // do not short-circuit! 
            src.doAction();
        }
        if (dest != null) {
          allSuccessful = allSuccessful & // do not short-circuit!
            dest.doAction();
        }
        if ( allSuccessful ) {
          EventBus.publish(new TransferSucceededEvent(src.isSourceDeletable(), formsToTransfer));
        } else {
          EventBus.publish(new TransferFailedEvent(src.isSourceDeletable(), formsToTransfer));
        }
      } catch (Exception e) {
        e.printStackTrace();
        EventBus.publish(new TransferFailedEvent(src.isSourceDeletable(), formsToTransfer));
      }
    }

  }

  private static void showDialogAndRun(ITransferFromSourceAction src, ITransferToDestAction dest,
      List<FormStatus> formsToTransfer) {
    // create the dialog first so that the background task will always have a
    // listener for its completion events...
    final TransferInProgressDialog dlg = new TransferInProgressDialog("Transfer in Progress...");

    backgroundExecutorService.execute(new TransferRunnable(src, dest, formsToTransfer));

    dlg.setVisible(true);
  }

  private static class RetrieveAvailableFormsRunnable implements Runnable {
    RetrieveAvailableFormsFromServer src;

    RetrieveAvailableFormsRunnable(RetrieveAvailableFormsFromServer src) {
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

  private static void showDialogAndRun(RetrieveAvailableFormsFromServer src) {
    // create the dialog first so that the background task will always have a
    // listener for its completion events...
    final TransferInProgressDialog dlg = new TransferInProgressDialog("Fetching Available Forms...");

    backgroundExecutorService.execute(new RetrieveAvailableFormsRunnable(src));

    dlg.setVisible(true);
  }

  public static void retrieveAvailableFormsFromServer(ServerConnectionInfo originServerInfo) {
    RetrieveAvailableFormsFromServer source = new RetrieveAvailableFormsFromServer(originServerInfo);
    showDialogAndRun(source);
  }

  public static void transferServerViaToServer(ServerConnectionInfo originServerInfo,
      ServerConnectionInfo destinationServerInfo, 
      List<FormStatus> formsToTransfer) throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferFromServer source = new TransferFromServer(originServerInfo, briefcaseDir,
        formsToTransfer, true);
    TransferToServer dest = new TransferToServer(destinationServerInfo, briefcaseDir,
        formsToTransfer, true);
    showDialogAndRun(source, dest, formsToTransfer);
  }

  public static void transferServerViaToBriefcase(ServerConnectionInfo originServerInfo,
      List<FormStatus> formsToTransfer)
      throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferFromServer source = new TransferFromServer(originServerInfo, briefcaseDir,
        formsToTransfer, false);
    showDialogAndRun(source, null, formsToTransfer);
  }

  public static void transferODKViaToServer(File odkSrcDir,
      ServerConnectionInfo destinationServerInfo,
      List<FormStatus> formsToTransfer) throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferFromODK source = new TransferFromODK(odkSrcDir, briefcaseDir, formsToTransfer, true);
    TransferToServer dest = new TransferToServer(destinationServerInfo, briefcaseDir,
        formsToTransfer, true);
    showDialogAndRun(source, dest, formsToTransfer);
  }

  public static void transferODKViaToBriefcase(File odkSrcDir,
      List<FormStatus> formsToTransfer) throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferFromODK source = new TransferFromODK(odkSrcDir, briefcaseDir, formsToTransfer, false);
    showDialogAndRun(source, null, formsToTransfer);
  }

  public static void transferBriefcaseViaToServer(
      ServerConnectionInfo destinationServerInfo,
      List<FormStatus> formsToTransfer) throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferToServer dest = new TransferToServer(destinationServerInfo, briefcaseDir,
        formsToTransfer, false);
    showDialogAndRun(null, dest, formsToTransfer);
  }
}
