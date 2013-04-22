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
package com.google.gwt.maps.client.event;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.maps.client.base.HasLatLng;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.event.impl.MouseEventImpl;

/**
 * This class implements {@link HasMouseEvent}.
 *
 * @author vinay.sekhri@gmail.com (Vinay Sekhri)
 */
public class MouseEvent implements HasMouseEvent {

  final private JavaScriptObject jso;
  
  public MouseEvent(JavaScriptObject jso) {
    this.jso = jso;
  }
  
  @Override
  public HasLatLng getLatLng() {
    return new LatLng(MouseEventImpl.impl.getLatLng(jso));
  }

  @Override
  public JavaScriptObject getJso() {
    return jso;
  }

}
