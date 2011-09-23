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
import java.util.List;

import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;

public class TransferToServer implements ITransferToDestAction {
  ServerConnectionInfo destServerInfo;
  File briefcaseDir;
  List<FormStatus> formsToTransfer;
  boolean fromScratch;

  public TransferToServer(ServerConnectionInfo destServerInfo, File briefcaseDir,
      List<FormStatus> formsToTransfer, boolean fromScratch) {
    this.destServerInfo = destServerInfo;
    this.briefcaseDir = briefcaseDir;
    this.formsToTransfer = formsToTransfer;
    this.fromScratch = fromScratch;
  }

  @Override
  public boolean doAction() {
    File briefcaseFormsDir;
    if (fromScratch) {
      briefcaseFormsDir = FileSystemUtils.getScratchFolder(briefcaseDir);
    } else {
      briefcaseFormsDir = FileSystemUtils.getFormsFolder(briefcaseDir);
    }

    ServerUploader uploader = new ServerUploader(destServerInfo);
    
    return uploader.uploadFormAndSubmissionFiles( briefcaseFormsDir,
                                                  formsToTransfer);
  }
}