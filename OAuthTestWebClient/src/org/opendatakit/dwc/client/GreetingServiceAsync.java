/*
 * Copyright (C) 2012 University of Washington.
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

package org.opendatakit.dwc.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface GreetingServiceAsync {
  void greetServer(String input, AsyncCallback<String> callback);

  void obtainToken(String destinationUrl, AsyncCallback<String> callback);
  
  void obtainOauth1Data(String destinationUrl, AsyncCallback<String> callback);

  void obtainOauth2Data(String destinationUrl, AsyncCallback<String> callback);

  void obtainOauth2Code(String destinationUrl, AsyncCallback<String> callback);

  void getOauth2UserEmail(AsyncCallback<String> callback);

  void setConfiguration(Configuration config, AsyncCallback<Void> callback);

  void getConfiguration(AsyncCallback<Configuration> callback);

  void obtainOauth2ServiceAccountCode(String string, AsyncCallback<String> asyncCallback);

  void getOauth2ServiceAccountUserEmail(AsyncCallback<String> asyncCallback);
}
