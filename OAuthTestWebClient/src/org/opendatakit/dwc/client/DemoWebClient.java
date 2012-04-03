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

import org.opendatakit.dwc.shared.FieldVerifier;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Basic framework is built on top of Google's GWT Greetings Service example.
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class DemoWebClient implements EntryPoint {
  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
      + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  /**
   * Create a remote service proxy to talk to the server-side Greeting service.
   */
  private final GreetingServiceAsync greetingService = GWT.create(GreetingService.class);
  
  public native void goTo(String url) /*-{
      $wnd.location = url;
  }-*/;

  enum BUTTON_FIELD_ACTION {
	  GREETING,
	  OAUTH1_FETCH,
	  OAUTH2_FETCH
  };
  
  TextBox clientWebsiteName = new TextBox();
  TextBox clientWebsitePort = new TextBox();
  TextBox clientWebsiteCodesvrPort = new TextBox();
  TextBox clientWebsiteClientId = new TextBox();
  TextBox clientWebsiteClientSecret = new TextBox();
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    final Label errorLabel = new Label();
  
    // shared error area
    RootPanel.get("errorLabelContainer").add(errorLabel);

    {
    	clientWebsiteName.setText("--clientWebsiteName--");
    	RootPanel.get("clientWebsiteNameContainer").add(clientWebsiteName);
    	clientWebsitePort.setText("--clientWebsitePort--");
    	RootPanel.get("clientWebsitePortContainer").add(clientWebsitePort);
    	clientWebsiteCodesvrPort.setText("--clientWebsiteCodesvrPort--");
    	RootPanel.get("clientWebsiteCodesvrPortContainer").add(clientWebsiteCodesvrPort);
    	clientWebsiteClientId.setText("--clientWebsiteClientId--");
    	RootPanel.get("clientWebsiteClientIdContainer").add(clientWebsiteClientId);
    	clientWebsiteClientSecret.setText("--clientWebsiteClientSecret--");
    	RootPanel.get("clientWebsiteClientSecretContainer").add(clientWebsiteClientSecret);
    	
    	final Button fetchButton = new Button("Get Configuration");
    	final Button setButton = new Button("Set Configuration");
    	RootPanel.get("fetchConfigurationButtonContainer").add(fetchButton);
    	RootPanel.get("setConfigurationButtonContainer").add(setButton);
    	
    	fetchButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				greetingService.getConfiguration(new AsyncCallback<Configuration>(){

					@Override
					public void onFailure(Throwable caught) {
						errorLabel.setText(caught.toString());
					}

					@Override
					public void onSuccess(Configuration result) {
						{
							String value = result.get(Configuration.CLIENT_WEBSITE_HOSTNAME_KEY);
							clientWebsiteName.setText((value == null) ? "" : value);
						}
						{
							String value = result.get(Configuration.CLIENT_WEBSITE_PORT_KEY);
							clientWebsitePort.setText((value == null) ? "" : value);
						}
						{
							String value = result.get(Configuration.CLIENT_WEBSITE_CODESVR_PORT_KEY);
							clientWebsiteCodesvrPort.setText((value == null) ? "" : value);
						}
						{
							String value = result.get(Configuration.CLIENT_ID_KEY);
							clientWebsiteClientId.setText((value == null) ? "" : value);
						}
						{
							String value = result.get(Configuration.CLIENT_SECRET_KEY);
							clientWebsiteClientSecret.setText((value == null) ? "" : value);
						}
						
					}});
				
			}});
    	
    	setButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Configuration config = new Configuration();
				config.put(Configuration.CLIENT_WEBSITE_HOSTNAME_KEY, 
						clientWebsiteName.getText());
				config.put(Configuration.CLIENT_WEBSITE_PORT_KEY, 
						clientWebsitePort.getText());
				config.put(Configuration.CLIENT_WEBSITE_CODESVR_PORT_KEY, 
						clientWebsiteCodesvrPort.getText());
				config.put(Configuration.CLIENT_ID_KEY, 
						clientWebsiteClientId.getText());
				config.put(Configuration.CLIENT_SECRET_KEY, 
						clientWebsiteClientSecret.getText());
				
				greetingService.setConfiguration(config, new AsyncCallback<Void>(){

					@Override
					public void onFailure(Throwable caught) {
						errorLabel.setText(caught.toString());
					}

					@Override
					public void onSuccess(Void result) {
					}});
			}});
    }

    {
      final Button sendButton = new Button("Send");
      final TextBox nameField = new TextBox();
      nameField.setText("GWT User");
      // We can add style names to widgets
      sendButton.addStyleName("sendButton");
  
      // Add the nameField and sendButton to the RootPanel
      // Use RootPanel.get() to get the entire body element
      RootPanel.get("nameFieldContainer").add(nameField);
      RootPanel.get("sendButtonContainer").add(sendButton);
  
      // Focus the cursor on the name field when the app loads
      nameField.setFocus(true);
      nameField.selectAll();
    
      defineDialogIneractions(nameField, sendButton, errorLabel, BUTTON_FIELD_ACTION.GREETING);
    }
    
    {
      final Button getUserEmailTokenButton = new Button("Get OAuth1 Token");

      RootPanel.get("userEmailTokenButton").add(getUserEmailTokenButton);
      
      // Add a handler to close the DialogBox
      getUserEmailTokenButton.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          greetingService.obtainToken("passthrough", new AsyncCallback<String>() {
              @Override
              public void onFailure(Throwable caught) {
                errorLabel.setText(caught.toString());
              }
      
              @Override
              public void onSuccess(String result) {
                 goTo(result);
              }
          }); 
        }
      });
    }
    
    {
      final Button getUserEmailOauth2TokenButton = new Button("Get OAuth2 Code");

      RootPanel.get("userEmailOauth2TokenButton").add(getUserEmailOauth2TokenButton);
      
      // Add a handler to close the DialogBox
      getUserEmailOauth2TokenButton.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          greetingService.obtainOauth2Code("passthrough", new AsyncCallback<String>() {
              @Override
              public void onFailure(Throwable caught) {
                errorLabel.setText(caught.toString());
              }
      
              @Override
              public void onSuccess(String result) {
                 goTo(result);
              }
          }); 
        }
      });
    }
    
    {
      final Button getOauth2UserEmailButtonContainer = new Button("Use Oauth2 to access Google info.email");
      final TextBox userEmailFieldContainer = new TextBox();
      userEmailFieldContainer.setText("--confirm retrieval of Oauth2 info.email -- unknown--");
      
      RootPanel.get("getOauth2UserEmailButtonContainer").add(getOauth2UserEmailButtonContainer);
      RootPanel.get("userEmailFieldContainer").add(userEmailFieldContainer);
      
      // Add a handler to close the DialogBox
      getOauth2UserEmailButtonContainer.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          greetingService.getOauth2UserEmail(new AsyncCallback<String>() {
              @Override
              public void onFailure(Throwable caught) {
                errorLabel.setText(caught.toString());
              }
      
              @Override
              public void onSuccess(String result) {
                userEmailFieldContainer.setText(result);
              }
          }); 
        }
      });
    }

    {
      final Button urlSendButton = new Button("Access site using OAuth1");
      final TextBox urlField = new TextBox();
      urlField.setText("URL to access");
  
      // We can add style names to widgets
      urlSendButton.addStyleName("sendButton");
  
      // Add the nameField and sendButton to the RootPanel
      // Use RootPanel.get() to get the entire body element
      RootPanel.get("urlOauth1FieldContainer").add(urlField);
      RootPanel.get("urlOauth1SendButtonContainer").add(urlSendButton);
  
      defineDialogIneractions(urlField, urlSendButton, errorLabel, BUTTON_FIELD_ACTION.OAUTH1_FETCH);
    }

    {
      final Button urlSendButton = new Button("Access site using Oauth2");
      final TextBox urlField = new TextBox();
      urlField.setText("URL to access");
  
      // We can add style names to widgets
      urlSendButton.addStyleName("sendButton");
  
      // Add the nameField and sendButton to the RootPanel
      // Use RootPanel.get() to get the entire body element
      RootPanel.get("urlOauth2FieldContainer").add(urlField);
      RootPanel.get("urlOauth2SendButtonContainer").add(urlSendButton);
  
      defineDialogIneractions(urlField, urlSendButton, errorLabel, BUTTON_FIELD_ACTION.OAUTH2_FETCH);
    }
  }
  
  private void defineDialogIneractions(final TextBox nameField, final Button sendButton, final Label errorLabel, final BUTTON_FIELD_ACTION api) {

    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Remote Procedure Call");
    dialogBox.setAnimationEnabled(true);
    final Button closeButton = new Button("Close");
    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    final Label textToServerLabel = new Label();
    final HTML serverResponseLabel = new HTML();
    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
    dialogVPanel.add(textToServerLabel);
    dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
    dialogVPanel.add(serverResponseLabel);
    dialogVPanel.setWidth(Integer.toString(Window.getClientWidth() - 10) + "px");
    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    dialogBox.setWidget(dialogVPanel);

    // Add a handler to close the DialogBox
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
        sendButton.setEnabled(true);
        sendButton.setFocus(true);
      }
    });
  
    // Create a handler for the sendButton and nameField
    class MyHandler implements ClickHandler, KeyUpHandler {
      /**
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {
        sendNameToServer();
      }

      /**
       * Fired when the user types in the nameField.
       */
      public void onKeyUp(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          sendNameToServer();
        }
      }

      /**
       * Send the name from the nameField to the server and wait for a response.
       */
      private void sendNameToServer() {
        // First, we validate the input.
        errorLabel.setText("");
        String textToServer = nameField.getText();
        if (!FieldVerifier.isValidName(textToServer)) {
          errorLabel.setText("Please enter at least four characters");
          return;
        }

        // Then, we send the input to the server.
        sendButton.setEnabled(false);
        textToServerLabel.setText(textToServer);
        serverResponseLabel.setText("");
        AsyncCallback<String> callback =
            new AsyncCallback<String>() {
          public void onFailure(Throwable caught) {
            // Show the RPC error message to the user
            dialogBox.setText("Remote Procedure Call - Failure");
            serverResponseLabel.addStyleName("serverResponseLabelError");
            serverResponseLabel.setHTML("<verbatim>"+SafeHtmlUtils.htmlEscape(SERVER_ERROR)+"</verbatim>");
            dialogBox.getWidget().setWidth("90%");
            dialogBox.center();
            closeButton.setFocus(true);
          }

          public void onSuccess(String result) {
            dialogBox.setText("Remote Procedure Call");
            serverResponseLabel.removeStyleName("serverResponseLabelError");
            serverResponseLabel.setHTML("<verbatim>"+SafeHtmlUtils.htmlEscape(result)+"</verbatim>");
            dialogBox.getWidget().setWidth("90%");
            dialogBox.center();
            closeButton.setFocus(true);
          }
        };
        
        if ( api == BUTTON_FIELD_ACTION.OAUTH2_FETCH ) {
            greetingService.obtainOauth2Data(textToServer, callback);
        } else if ( api == BUTTON_FIELD_ACTION.OAUTH1_FETCH ) {
          greetingService.obtainOauth1Data(textToServer, callback);
        } else {
          greetingService.greetServer(textToServer, callback);
        }
      }
    }

    // Add a handler to send the name to the server
    MyHandler handler = new MyHandler();
    sendButton.addClickHandler(handler);
    nameField.addKeyUpHandler(handler);
  }
}
