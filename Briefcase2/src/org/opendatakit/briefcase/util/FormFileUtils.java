package org.opendatakit.briefcase.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.util.JavaRosaWrapper.BadFormDefinition;

public class FormFileUtils {

	public static final String getMountPoint() {
		return System.getProperty("os.name").startsWith("Win") ? File.separator + ".." :
			(System.getProperty("os.name").startsWith("Mac") ? "/Volumes/" : "/mnt/");
	}
	
	public static final boolean isUnderBriefcaseFolder(File pathname) {
		File parent = (pathname == null ? null : pathname.getParentFile());
		while ( parent != null ) {
			if ( isBriefcaseFolder(parent, false) ) return true;
			parent = parent.getParentFile();
		}
		return false;
	}
	
	private static final boolean isBriefcaseFolder(File pathname, boolean strict) {
		String[] contents = pathname.list();
		int len = (contents == null) ? 0 : contents.length;
		File foi = getScratchFormsPath( pathname);
		File fof = getStableFormsPath( pathname);
		return pathname.exists() && 
			((len == 0) || 
			((len == 1) && (foi.exists() || fof.exists())) ||
			 (((len == 2) || (!strict && (len == 3))) &&
				foi.exists() && fof.exists()));
	}

	public static final boolean isValidBriefcaseFolder(File pathname) {
		return pathname != null && isBriefcaseFolder(pathname, true);
	}
	
	public static final boolean isUnderODKFolder(File pathname) {
		File parent = (pathname == null ? null : pathname.getParentFile());
		while ( parent != null ) {
			if ( isODKDevice(parent) ) return true;
			parent = parent.getParentFile();
		}
		return false;
	}
	
	private static final boolean isODKDevice(File pathname) {
		File fo = new File( pathname, "odk");
		File foi = new File( fo, "instances");
		File fof = new File( fo, "forms");
		return fo.exists() && foi.exists() && fof.exists();
	}
	
	public static final boolean isValidODKFolder(File pathname) {
		return pathname != null && isODKDevice(pathname); 
				
	}
	
	public static final List<LocalFormDefinition> getBriefcaseFormList(String briefcaseDirectory ) {
		List<LocalFormDefinition> formsList = new ArrayList<LocalFormDefinition>();
		File briefcase = new File(briefcaseDirectory);
		File forms = getStableFormsPath(briefcase);
		if ( forms.exists() ) {
			File[] formDirs = forms.listFiles();
			for ( File f : formDirs ) {
				if ( f.isDirectory() ) {
					try {
						File formFile = new File(f, f.getName() + ".xml");
						formsList.add(new LocalFormDefinition(formFile));
					} catch (BadFormDefinition e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					// junk?
					f.delete();
				}
			}
		}
		return formsList;
	}
	
	public static final List<LocalFormDefinition> getODKFormList(String odkDeviceDirectory ) {
		List<LocalFormDefinition> formsList = new ArrayList<LocalFormDefinition>();
		File sdcard = new File(odkDeviceDirectory);
		File odk = new File(sdcard, "odk");
		File forms = new File(odk, "forms");
		if ( forms.exists() ) {
			File[] formDirs = forms.listFiles();
			for ( File f : formDirs ) {
				if ( f.isFile() && f.getName().endsWith(".xml") ) {
					try {
						formsList.add(new LocalFormDefinition(f));
					} catch (BadFormDefinition e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return formsList;
	}
	
	public static File getScratchFormsPath(File briefcaseDir) {
		return new File(briefcaseDir, "scratch");
	}

	public static File getStableFormsPath(File briefcaseDir) {
		return new File(briefcaseDir, "forms");
	}
	
	public static String getInstanceFilePath(File instanceDir) {
    	File instance = new File(instanceDir, instanceDir.getName() + ".xml");
    	return instance.getAbsolutePath();
	}

	public static String getSubmissionEnvelopePath(File instanceDir) {
    	File instance = new File(instanceDir, instanceDir.getName() + ".xml.envelope");
    	return instance.getAbsolutePath();
	}
}
