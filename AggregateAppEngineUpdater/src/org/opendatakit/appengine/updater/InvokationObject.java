package org.opendatakit.appengine.updater;

import java.util.Map;

import org.opendatakit.apache.commons.exec.CommandLine;
import org.opendatakit.apache.commons.exec.DefaultExecutor;

public class InvokationObject {
  Map<String,Object> substitutionMap;
  Map<String,String> envMap;
  DefaultExecutor executor;
  CommandLine cmdLine;
}