package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CommonUtils {
	private static final Log logger = LogFactory.getLog(CommonUtils.class);

	private static final String SCRATCH_DIR = "scratch";
	
	private CommonUtils() {};
	
	public static final String getMd5Hash(File file) {
        try {
            // CTS (6/15/2010) : stream file through digest instead of handing it the byte[]
            MessageDigest md = MessageDigest.getInstance("MD5");
            int chunkSize = 256;

            byte[] chunk = new byte[chunkSize];

            // Get the size of the file
            long lLength = file.length();

            if (lLength > Integer.MAX_VALUE) {
            	logger.error("File " + file.getName() + "is too large");
                return null;
            }

            int length = (int) lLength;

            InputStream is = null;
            is = new FileInputStream(file);

            int l = 0;
            for (l = 0; l + chunkSize < length; l += chunkSize) {
                is.read(chunk, 0, chunkSize);
                md.update(chunk, 0, chunkSize);
            }

            int remaining = length - l;
            if (remaining > 0) {
                is.read(chunk, 0, remaining);
                md.update(chunk, 0, remaining);
            }
            byte[] messageDigest = md.digest();

            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);
            while (md5.length() < 32)
                md5 = "0" + md5;
            is.close();
            return md5;

        } catch (NoSuchAlgorithmException e) {
        	logger.error("MD5 calculation failed: " + e.getMessage());
            return null;

        } catch (FileNotFoundException e) {
        	logger.error("No Xml File: " + e.getMessage());
            return null;
        } catch (IOException e) {
        	logger.error("Problem reading from file: " + e.getMessage());
            return null;
        }

    }

	public static final boolean createFolder(String path) {
        boolean made = true;
        File dir = new File(path);
        if (!dir.exists()) {
            made = dir.mkdirs();
        }
        return made;
	 }
	
	public static File clearBriefcaseScratch(File briefcaseDir) throws IOException {
	
		File scratch = new File(briefcaseDir, SCRATCH_DIR);
		if ( scratch.exists() ) {
			FileUtils.deleteDirectory(scratch);
		}
		if ( !scratch.mkdir() ) {
			throw new IOException("Unable to clear scratch space");
		}
		return scratch;
	}

}
