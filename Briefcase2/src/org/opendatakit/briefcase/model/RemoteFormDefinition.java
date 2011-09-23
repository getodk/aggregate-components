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

package org.opendatakit.briefcase.model;

public class RemoteFormDefinition implements IFormDefinition {

  final String formName;
  final String formId;
  final Integer modelVersion;
  final Integer uiVersion;
  final String downloadUrl;
  final String manifestUrl;

  public RemoteFormDefinition(String formName, String formId, Integer modelVersion,
      Integer uiVersion, String downloadUrl, String manifestUrl) {
    this.formName = formName;
    this.formId = formId;
    this.modelVersion = modelVersion;
    this.uiVersion = uiVersion;
    this.downloadUrl = downloadUrl;
    this.manifestUrl = manifestUrl;
  }

  @Override
  public LocationType getFormLocation() {
    return LocationType.REMOTE;
  }

  @Override
  public String getFormName() {
    return formName;
  }

  @Override
  public String getFormId() {
    return formId;
  }

  @Override
  public Integer getModelVersion() {
    return modelVersion;
  }

  @Override
  public Integer getUiVersion() {
    return uiVersion;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public String getManifestUrl() {
    return manifestUrl;
  }
}
