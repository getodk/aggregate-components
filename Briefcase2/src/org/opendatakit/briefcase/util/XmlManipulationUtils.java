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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javarosa.xform.parse.XFormParser;
import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.opendatakit.briefcase.model.MetadataUpdateException;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.util.ServerFetcher.MediaFile;
import org.opendatakit.briefcase.util.ServerFetcher.SubmissionDownloadChunk;
import org.opendatakit.briefcase.util.ServerFetcher.SubmissionManifest;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlManipulationUtils {

  private static final Log logger = LogFactory.getLog(XmlManipulationUtils.class);

  private static final String BAD_OPENROSA_FORMLIST = "The server has not provided an available-forms document compliant with the OpenRosa version 1.0 standard.";

  private static final String BAD_LEGACY_FORMLIST = "The server has not provided an available-forms document compatible with Aggregate 0.9.x.";

  private static final String BAD_NOT_OPENROSA_MANIFEST = "The server did not return an OpenRosa compliant manifest.";

  private static final String NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_MANIFEST = "http://openrosa.org/xforms/xformsManifest";

  private static final String NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_LIST = "http://openrosa.org/xforms/xformsList";

  private static final String NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS = "http://opendatakit.org/submissions";
  
  private static final String NAMESPACE_ODK = "http://www.opendatakit.org/xforms";

  // NOTE: the only transfered metadata is the instanceID and the submissionDate
  
  //private static final String FORM_ID_ATTRIBUTE_NAME = "id";
  //private static final String MODEL_VERSION_ATTRIBUTE_NAME = "version";
  //private static final String UI_VERSION_ATTRIBUTE_NAME = "uiVersion";
  private static final String INSTANCE_ID_ATTRIBUTE_NAME = "instanceID";
  private static final String SUBMISSION_DATE_ATTRIBUTE_NAME = "submissionDate";
  //private static final String IS_COMPLETE_ATTRIBUTE_NAME = "isComplete";
  //private static final String MARKED_AS_COMPLETE_DATE_ATTRIBUTE_NAME = "markedAsCompleteDate";


  private static final boolean isXformsListNamespacedElement(Element e) {
    return e.getNamespace().equalsIgnoreCase(NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_LIST);
  }

  private static final boolean isXformsManifestNamespacedElement(Element e) {
    return e.getNamespace().equalsIgnoreCase(NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_MANIFEST);
  }

  public static final List<RemoteFormDefinition> parseFormListResponse(boolean isOpenRosaResponse, Document formListDoc)
      throws ParsingException {
    // This gets a list of available forms from the specified server.
    List<RemoteFormDefinition> formList = new ArrayList<RemoteFormDefinition>();

    if (isOpenRosaResponse) {
      // Attempt OpenRosa 1.0 parsing
      Element xformsElement = formListDoc.getRootElement();
      if (!xformsElement.getName().equals("xforms")) {
        logger.error("Parsing OpenRosa reply -- root element is not <xforms> :"
            + xformsElement.getName());
        throw new ParsingException(BAD_OPENROSA_FORMLIST);
      }
      String namespace = xformsElement.getNamespace();
      if (!isXformsListNamespacedElement(xformsElement)) {
        logger.error("Parsing OpenRosa reply -- root element namespace is incorrect:" + namespace);
        throw new ParsingException(BAD_OPENROSA_FORMLIST);
      }
      int nElements = xformsElement.getChildCount();
      for (int i = 0; i < nElements; ++i) {
        if (xformsElement.getType(i) != Element.ELEMENT) {
          // e.g., whitespace (text)
          continue;
        }
        Element xformElement = (Element) xformsElement.getElement(i);
        if (!isXformsListNamespacedElement(xformElement)) {
          // someone else's extension?
          continue;
        }
        String name = xformElement.getName();
        if (!name.equalsIgnoreCase("xform")) {
          // someone else's extension?
          continue;
        }

        // this is something we know how to interpret
        String formId = null;
        String formName = null;
        String majorMinorVersion = null;
        String description = null;
        String downloadUrl = null;
        String manifestUrl = null;
        // don't process descriptionUrl
        int fieldCount = xformElement.getChildCount();
        for (int j = 0; j < fieldCount; ++j) {
          if (xformElement.getType(j) != Element.ELEMENT) {
            // whitespace
            continue;
          }
          Element child = xformElement.getElement(j);
          if (!isXformsListNamespacedElement(child)) {
            // someone else's extension?
            continue;
          }
          String tag = child.getName();
          if (tag.equals("formID")) {
            formId = XFormParser.getXMLText(child, true);
            if (formId != null && formId.length() == 0) {
              formId = null;
            }
          } else if (tag.equals("name")) {
            formName = XFormParser.getXMLText(child, true);
            if (formName != null && formName.length() == 0) {
              formName = null;
            }
          } else if (tag.equals("majorMinorVersion")) {
            majorMinorVersion = XFormParser.getXMLText(child, true);
            if (majorMinorVersion != null && majorMinorVersion.length() == 0) {
              majorMinorVersion = null;
            }
          } else if (tag.equals("descriptionText")) {
            description = XFormParser.getXMLText(child, true);
            if (description != null && description.length() == 0) {
              description = null;
            }
          } else if (tag.equals("downloadUrl")) {
            downloadUrl = XFormParser.getXMLText(child, true);
            if (downloadUrl != null && downloadUrl.length() == 0) {
              downloadUrl = null;
            }
          } else if (tag.equals("manifestUrl")) {
            manifestUrl = XFormParser.getXMLText(child, true);
            if (manifestUrl != null && manifestUrl.length() == 0) {
              manifestUrl = null;
            }
          }
        }
        if (formId == null || downloadUrl == null || formName == null) {
          logger.error("Parsing OpenRosa reply -- Forms list entry " + Integer.toString(i)
              + " is missing one or more tags: formId, name, or downloadUrl");
          formList.clear();
          throw new ParsingException(BAD_OPENROSA_FORMLIST);
        }
        Integer modelVersion = null;
        Integer uiVersion = null;
        try {
          if (majorMinorVersion == null || majorMinorVersion.length() == 0) {
            modelVersion = null;
            uiVersion = null;
          } else {
            int idx = majorMinorVersion.indexOf(".");
            if (idx == -1) {
              modelVersion = Integer.parseInt(majorMinorVersion);
              uiVersion = null;
            } else {
              modelVersion = Integer.parseInt(majorMinorVersion.substring(0, idx));
              uiVersion = (idx == majorMinorVersion.length() - 1) ? null : Integer
                  .parseInt(majorMinorVersion.substring(idx + 1));
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          logger.error("Parsing OpenRosa reply -- Forms list entry " + Integer.toString(i)
              + " has an invalid majorMinorVersion: " + majorMinorVersion);
          formList.clear();
          throw new ParsingException(BAD_OPENROSA_FORMLIST);
        }
        formList.add(new RemoteFormDefinition(formName, formId, modelVersion, uiVersion,
            downloadUrl, manifestUrl));
      }
    } else {
      // Aggregate 0.9.x mode...
      // populate HashMap with form names and urls
      Element formsElement = formListDoc.getRootElement();
      int formsCount = formsElement.getChildCount();
      for (int i = 0; i < formsCount; ++i) {
        if (formsElement.getType(i) != Element.ELEMENT) {
          // whitespace
          continue;
        }
        Element child = formsElement.getElement(i);
        String tag = child.getName();
        if (tag.equalsIgnoreCase("form")) {
          String formName = XFormParser.getXMLText(child, true);
          if (formName != null && formName.length() == 0) {
            formName = null;
          }
          String downloadUrl = child.getAttributeValue(null, "url");
          downloadUrl = downloadUrl.trim();
          if (downloadUrl != null && downloadUrl.length() == 0) {
            downloadUrl = null;
          }
          if (downloadUrl == null || formName == null) {
            logger.error("Parsing OpenRosa reply -- Forms list entry " + Integer.toString(i)
                + " is missing form name or url attribute");
            formList.clear();
            throw new ParsingException(BAD_LEGACY_FORMLIST);
          }
          formList.add(new RemoteFormDefinition(formName, null, null, null, downloadUrl, null));
        }
      }
    }
    return formList;
  }

  public static final List<MediaFile> parseFormManifestResponse( boolean isOpenRosaResponse, Document doc) throws ParsingException {

    List<MediaFile> files = new ArrayList<MediaFile>();
    
    if (!isOpenRosaResponse) {
      logger.error("Manifest reply doesn't report an OpenRosa version -- bad server?");
      throw new ParsingException(BAD_NOT_OPENROSA_MANIFEST);
    }

    // Attempt OpenRosa 1.0 parsing
    Element manifestElement = doc.getRootElement();
    if (!manifestElement.getName().equals("manifest")) {
      logger.error("Root element is not <manifest> -- was " + manifestElement.getName());
      throw new ParsingException(BAD_NOT_OPENROSA_MANIFEST);
    }
    String namespace = manifestElement.getNamespace();
    if (!isXformsManifestNamespacedElement(manifestElement)) {
      logger.error("Root element Namespace is incorrect: " + namespace);
      throw new ParsingException(BAD_NOT_OPENROSA_MANIFEST);
    }
    int nElements = manifestElement.getChildCount();
    for (int i = 0; i < nElements; ++i) {
      if (manifestElement.getType(i) != Element.ELEMENT) {
        // e.g., whitespace (text)
        continue;
      }
      Element mediaFileElement = (Element) manifestElement.getElement(i);
      if (!isXformsManifestNamespacedElement(mediaFileElement)) {
        // someone else's extension?
        continue;
      }
      String name = mediaFileElement.getName();
      if (name.equalsIgnoreCase("mediaFile")) {
        String filename = null;
        String hash = null;
        String downloadUrl = null;
        // don't process descriptionUrl
        int childCount = mediaFileElement.getChildCount();
        for (int j = 0; j < childCount; ++j) {
          if (mediaFileElement.getType(j) != Element.ELEMENT) {
            // e.g., whitespace (text)
            continue;
          }
          Element child = mediaFileElement.getElement(j);
          if (!isXformsManifestNamespacedElement(child)) {
            // someone else's extension?
            continue;
          }
          String tag = child.getName();
          if (tag.equals("filename")) {
            filename = XFormParser.getXMLText(child, true);
            if (filename != null && filename.length() == 0) {
              filename = null;
            }
          } else if (tag.equals("hash")) {
            hash = XFormParser.getXMLText(child, true);
            if (hash != null && hash.length() == 0) {
              hash = null;
            }
          } else if (tag.equals("downloadUrl")) {
            downloadUrl = XFormParser.getXMLText(child, true);
            if (downloadUrl != null && downloadUrl.length() == 0) {
              downloadUrl = null;
            }
          }
        }
        if (filename == null || downloadUrl == null || hash == null) {
          logger.error("Manifest entry " + Integer.toString(i)
              + " is missing one or more tags: filename, hash, or downloadUrl");
          throw new ParsingException(BAD_NOT_OPENROSA_MANIFEST);
        }
        files.add(new MediaFile(filename, hash, downloadUrl));
      }
    }
    return files;
  }

  public static final SubmissionDownloadChunk parseSubmissionDownloadListResponse(Document doc) throws ParsingException {
    List<String> uriList = new ArrayList<String>();
    String websafeCursorString = "";
    
    // Attempt parsing
    Element idChunkElement = doc.getRootElement();
    if (!idChunkElement.getName().equals("idChunk")) {
      String msg = "Parsing submissionList reply -- root element is not <idChunk> :"
          + idChunkElement.getName();
      logger.error(msg);
      throw new ParsingException(msg);
    }
    String namespace = idChunkElement.getNamespace();
    if (!namespace.equalsIgnoreCase(NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS)) {
      String msg = "Parsing submissionList reply -- root element namespace is incorrect:"
          + namespace;
      logger.error(msg);
      throw new ParsingException(msg);
    }
    int nElements = idChunkElement.getChildCount();
    for (int i = 0; i < nElements; ++i) {
      if (idChunkElement.getType(i) != Element.ELEMENT) {
        // e.g., whitespace (text)
        continue;
      }
      Element subElement = (Element) idChunkElement.getElement(i);
      namespace = subElement.getNamespace();
      if (!namespace.equalsIgnoreCase(NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS)) {
        // someone else's extension?
        continue;
      }
      String name = subElement.getName();
      if (name.equalsIgnoreCase("idList")) {
        // parse the idList
        int nIdElements = subElement.getChildCount();
        for (int j = 0; j < nIdElements; ++j) {
          if (subElement.getType(j) != Element.ELEMENT) {
            // e.g., whitespace (text)
            continue;
          }
          Element idElement = (Element) subElement.getElement(j);
          namespace = idElement.getNamespace();
          if (!namespace.equalsIgnoreCase(NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS)) {
            // someone else's extension?
            continue;
          }
          name = idElement.getName();
          if (name.equalsIgnoreCase("id")) {
            // gather the uri
            String uri = XFormParser.getXMLText(idElement, true);
            if (uri != null) {
              uriList.add(uri);
            }
          } else {
            logger.warn("Unrecognized tag inside idList: " + name);
          }
        }
      } else if (name.equalsIgnoreCase("resumptionCursor")) {
        // gather the resumptionCursor
        websafeCursorString = XFormParser.getXMLText(subElement, true);
        if (websafeCursorString == null) {
          websafeCursorString = "";
        }
      } else {
        logger.warn("Unrecognized tag inside idChunk: " + name);
      }
    }

    return new SubmissionDownloadChunk( uriList, websafeCursorString );
  }

  public static final SubmissionManifest parseDownloadSubmissionResponse( Document doc ) throws ParsingException {

    // and parse the document...
    List<MediaFile> attachmentList = new ArrayList<MediaFile>();
    Element rootSubmissionElement = null;
    String instanceID = null;

    // Attempt parsing
    Element submissionElement = doc.getRootElement();
    if (!submissionElement.getName().equals("submission")) {
      String msg = "Parsing downloadSubmission reply -- root element is not <submission> :"
          + submissionElement.getName();
      logger.error(msg);
      throw new ParsingException(msg);
    }
    String namespace = submissionElement.getNamespace();
    if (!namespace.equalsIgnoreCase(NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS)) {
      String msg = "Parsing downloadSubmission reply -- root element namespace is incorrect:"
          + namespace;
      logger.error(msg);
      throw new ParsingException(msg);
    }
    int nElements = submissionElement.getChildCount();
    for (int i = 0; i < nElements; ++i) {
      if (submissionElement.getType(i) != Element.ELEMENT) {
        // e.g., whitespace (text)
        continue;
      }
      Element subElement = (Element) submissionElement.getElement(i);
      namespace = subElement.getNamespace();
      if (!namespace.equalsIgnoreCase(NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS)) {
        // someone else's extension?
        continue;
      }
      String name = subElement.getName();
      if (name.equalsIgnoreCase("data")) {
        // find the root submission element and get its instanceID attribute
        int nIdElements = subElement.getChildCount();
        for (int j = 0; j < nIdElements; ++j) {
          if (subElement.getType(j) != Element.ELEMENT) {
            // e.g., whitespace (text)
            continue;
          }
          rootSubmissionElement = (Element) subElement.getElement(j);
          break;
        }
        if (rootSubmissionElement == null) {
          throw new ParsingException("no submission body found in submissionDownload response");
        }

        instanceID = rootSubmissionElement.getAttributeValue(null, "instanceID");
        if (instanceID == null) {
          throw new ParsingException("instanceID attribute value is null");
        }
      } else if (name.equalsIgnoreCase("mediaFile")) {
        int nIdElements = subElement.getChildCount();
        String filename = null;
        String hash = null;
        String downloadUrl = null;
        for (int j = 0; j < nIdElements; ++j) {
          if (subElement.getType(j) != Element.ELEMENT) {
            // e.g., whitespace (text)
            continue;
          }
          Element mediaSubElement = (Element) subElement.getElement(j);
          name = mediaSubElement.getName();
          if (name.equalsIgnoreCase("filename")) {
            filename = XFormParser.getXMLText(mediaSubElement, true);
          } else if (name.equalsIgnoreCase("hash")) {
            hash = XFormParser.getXMLText(mediaSubElement, true);
          } else if (name.equalsIgnoreCase("downloadUrl")) {
            downloadUrl = XFormParser.getXMLText(mediaSubElement, true);
          }
        }
        attachmentList.add(new MediaFile(filename, hash, downloadUrl));
      } else {
        logger.warn("Unrecognized tag inside submission: " + name);
      }
    }

    if (rootSubmissionElement == null) {
      throw new ParsingException("No submission body found");
    }
    if (instanceID == null) {
      throw new ParsingException("instanceID attribute value is null");
    }
    
    
    // write submission to a string
    StringWriter fo = new StringWriter();
    KXmlSerializer serializer = new KXmlSerializer();

    serializer.setOutput(fo);
    // setting the response content type emits the xml header.
    // just write the body here...
    // this has the xmlns of the submissions download, indicating that it
    // originated from a briefcase download. Might be useful for discriminating
    // real vs. recovered data?
    rootSubmissionElement.setPrefix(null, NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS);
    try {
      rootSubmissionElement.write(serializer);
      serializer.flush();
      serializer.endDocument();
      fo.close();
    } catch ( IOException e ) {
      e.printStackTrace();
      throw new ParsingException("Unexpected IOException: " + e.getMessage());
    }

    return new SubmissionManifest(instanceID, fo.toString(), attachmentList);
  }

  public static final String updateSubmissionMetadata( File submissionFile, Document doc ) throws MetadataUpdateException {
    
    Element root = doc.getRootElement();
    Element metadata = root.getElement(NAMESPACE_ODK, "submissionMetadata");
    
    // and get the instanceID and submissionDate from the metadata.
    // we need to put that back into the instance file if not already present
    String instanceID = metadata.getAttributeValue("", INSTANCE_ID_ATTRIBUTE_NAME);
    String submissionDate = metadata.getAttributeValue("", SUBMISSION_DATE_ATTRIBUTE_NAME);
    
    // read the original document...
    Document originalDoc = null;
    try {
      FileInputStream fs = new FileInputStream(submissionFile);
      InputStreamReader fr = new InputStreamReader(fs, "UTF-8");
      originalDoc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(fr);
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
      originalDoc.parse(parser);
      fr.close();
      fs.close();
    } catch (IOException e) {
      e.printStackTrace();
      String msg = "Original submission file could not be opened " + submissionFile.getAbsolutePath();
      logger.error(msg);
      throw new MetadataUpdateException(msg);
    } catch (XmlPullParserException e) {
      e.printStackTrace();
      String msg = "Original submission file could not be parsed as XML file " + submissionFile.getAbsolutePath();
      logger.error(msg);
      throw new MetadataUpdateException(msg);
    }

    // determine whether it has the attributes already added.
    // if they are already there, they better match the values returned by Aggregate 1.0
    boolean hasInstanceID = false;
    boolean hasSubmissionDate = false;
    root = originalDoc.getRootElement();
    for ( int i = 0 ; i < root.getAttributeCount() ; ++i ) {
      String name = root.getAttributeName(i);
      if ( name.equals(INSTANCE_ID_ATTRIBUTE_NAME) ) {
        if ( !root.getAttributeValue(i).equals(instanceID) ) {
          String msg = "Original submission file's instanceID does not match that on server! " + submissionFile.getAbsolutePath();
          logger.error(msg);
          throw new MetadataUpdateException(msg);
        } else {
          hasInstanceID = true;
        }
      }
      
      if ( name.equals(SUBMISSION_DATE_ATTRIBUTE_NAME) ) {
        if ( !root.getAttributeValue(i).equals(submissionDate) ) {
          String msg = "Original submission file's submissionDate does not match that on server! " + submissionFile.getAbsolutePath();
          logger.error(msg);
          throw new MetadataUpdateException(msg);
        } else {
          hasSubmissionDate = true;
        }
      }
    }
  
    if ( hasInstanceID && hasSubmissionDate ) {
      logger.info("submission already has instanceID and submissionDate attributes: " + submissionFile.getAbsolutePath());
      return instanceID;
    }
    
    if ( !hasInstanceID ) {
      root.setAttribute("", INSTANCE_ID_ATTRIBUTE_NAME, instanceID);
    }
    if ( !hasSubmissionDate ) {
      root.setAttribute("", SUBMISSION_DATE_ATTRIBUTE_NAME, submissionDate);
    }
    
    // and write out the changes...

    // write the file out...
    File revisedFile = new File( submissionFile.getParentFile(), "." + submissionFile.getName());
    try {
      FileOutputStream fos = new FileOutputStream(revisedFile, false);
      
      KXmlSerializer serializer = new KXmlSerializer();
      serializer.setOutput(fos, "UTF-8");
      originalDoc.write(serializer);
      serializer.flush();
      fos.close();
      
      // and swap files...
      boolean restoreTemp = false;
      File temp = new File( submissionFile.getParentFile(), ".back." + submissionFile.getName() );
      
      try {
        if ( temp.exists() ) {
          if ( !temp.delete() ) {
            String msg = "Unable to remove temporary submission backup file " + temp.getAbsolutePath();
            logger.error(msg);
            throw new MetadataUpdateException(msg);
          }
        }
        if ( !submissionFile.renameTo(temp) ) {
          String msg = "Unable to rename submission to temporary submission backup file " + temp.getAbsolutePath();
          logger.error(msg);
          throw new MetadataUpdateException(msg);
        }
        
        // recovery is possible...
        restoreTemp = true;
        
        if ( !revisedFile.renameTo(submissionFile) ) {
          String msg = "Original submission file could not be updated " + submissionFile.getAbsolutePath();
          logger.error(msg);
          throw new MetadataUpdateException(msg);
        }
        
        // we're successful...
        restoreTemp = false;
      } finally {
        if ( restoreTemp ) {
          if ( !temp.renameTo(submissionFile) ) {
            String msg = "Unable to restore submission from temporary submission backup file " + temp.getAbsolutePath();
            logger.error(msg);
            throw new MetadataUpdateException(msg);
          }
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      String msg = "Temporary submission file could not be opened " + revisedFile.getAbsolutePath();
      logger.error(msg);
      throw new MetadataUpdateException(msg);
    } catch (IOException e) {
      e.printStackTrace();
      String msg = "Temporary submission file could not be written " + revisedFile.getAbsolutePath();
      logger.error(msg);
      throw new MetadataUpdateException(msg);
    }
    return instanceID;
  }
}
