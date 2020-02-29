package com.excel.displayproject.util;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Compress {
    private static final int BUFFER = 2048;
    private String[] _files;
    private String _zipFile;

    public Compress(String[] files, String zipFile) {
        this._files = files;
        this._zipFile = zipFile;
    }

    public static void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
            return;
        }
        byte[] buf = new byte[1024];
        FileInputStream in = new FileInputStream(srcFile);
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append("/");
        sb.append(folder.getName());
        zip.putNextEntry(new ZipEntry(sb.toString()));
        while (true) {
            int read = in.read(buf);
            int len = read;
            if (read > 0) {
                zip.write(buf, 0, len);
            } else {
                return;
            }
        }
    }

    public static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        String[] list;
        File folder = new File(srcFolder);
        for (String fileName : folder.list()) {
            String str = "/";
            if (path.equals("")) {
                String name = folder.getName();
                StringBuilder sb = new StringBuilder();
                sb.append(srcFolder);
                sb.append(str);
                sb.append(fileName);
                addFileToZip(name, sb.toString(), zip);
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(path);
                sb2.append(str);
                sb2.append(folder.getName());
                String sb3 = sb2.toString();
                StringBuilder sb4 = new StringBuilder();
                sb4.append(srcFolder);
                sb4.append(str);
                sb4.append(fileName);
                addFileToZip(sb3, sb4.toString(), zip);
            }
        }
    }

    public static void zipFolder(String srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destZipFile));
        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
    }

    public void zip() {
        try {
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(this._zipFile)));
            byte[] data = new byte[2048];
            for (int i = 0; i < this._files.length; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append("Adding: ");
                sb.append(this._files[i]);
                Log.v("Compress", sb.toString());
                BufferedInputStream origin = new BufferedInputStream(new FileInputStream(this._files[i]), 2048);
                out.putNextEntry(new ZipEntry(this._files[i].substring(this._files[i].lastIndexOf("/") + 1)));
                while (true) {
                    int read = origin.read(data, 0, 2048);
                    int count = read;
                    if (read == -1) {
                        break;
                    }
                    out.write(data, 0, count);
                }
                origin.close();
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unZipIt(String zipFile, String outputFolder) {
        byte[] buffer = new byte[1024];
        try {
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
                String fileName = ze.getName();
                StringBuilder sb = new StringBuilder();
                sb.append(outputFolder);
                sb.append(File.separator);
                sb.append(fileName);
                File newFile = new File(sb.toString());
                PrintStream printStream = System.out;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("file unzip : ");
                sb2.append(newFile.getAbsoluteFile());
                printStream.println(sb2.toString());
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                while (true) {
                    int read = zis.read(buffer);
                    int len = read;
                    if (read <= 0) {
                        break;
                    }
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zis.closeEntry();
            zis.close();
            System.out.println("Done");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
