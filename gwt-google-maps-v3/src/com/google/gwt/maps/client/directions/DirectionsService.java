/* Copyright (c) 2010 Vinay Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gwt.maps.client.directions;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.maps.client.directions.impl.DirectionsServiceImpl;

/**
 * This class implements {@link HasDirectionsService}.
 *
 * @author vinay.sekhri@gmail.com (Vinay Sekhri)
 */
public class DirectionsService implements HasDirectionsService {
  
  private JavaScriptObject jso;
  
  public DirectionsService(JavaScriptObject jso) {
    this.jso = jso;
  }
  
  public DirectionsService() {
    this(DirectionsServiceImpl.impl.construct());
  }

  @Override
  public void route(HasDirectionsRequest request, DirectionsCallback callback) {
    DirectionsServiceImpl.impl.route(jso, request.getJso(), callback);
  }

  @Override
  public JavaScriptObject getJso() {
    return jso;
  }

}
