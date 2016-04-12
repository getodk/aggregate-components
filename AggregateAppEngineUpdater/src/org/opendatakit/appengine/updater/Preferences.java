/*
 * Copyright (C) 2016 University of Washington.
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

package org.opendatakit.appengine.updater;

public class Preferences {
  public static final String VERSION = "1.4.8";
  public static final String JAR_NAME = "ODKAggregateAppEngineUpdater.jar";
  public static final String APP_ENGINE_OAUTH2_JAVA_FILENAME = ".appcfg_oauth2_tokens_java";
  // SDK location under installer
  public static final String APP_ENGINE_SDK_DIRNAME = "appengine-java-sdk";
  public static final String LEGACY_REMOVAL_DIRNAME = "LegacyRemoval";
  public static final String ODK_AGGREGATE_INSTALLATION_DIRNAME = "ODK Aggregate";
  public static final String ODK_AGGREGATE_EAR_DIRNAME = "ODKAggregate";
}
