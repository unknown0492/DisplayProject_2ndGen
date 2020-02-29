package com.excel.displayproject.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class MD5 {
    public static byte[] createChecksum(File file) throws Exception {
        int numRead;
        InputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    public static String getMD5Checksum(File file) throws Exception {
        byte[] b = createChecksum(file);
        String result = "";
        for (byte b2 : b) {
            StringBuilder sb = new StringBuilder();
            sb.append(result);
            sb.append(Integer.toString((b2 & 255) + 256, 16).substring(1));
            result = sb.toString();
        }
        return result;
    }
}
