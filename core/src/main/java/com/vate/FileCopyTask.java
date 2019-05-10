package com.vate;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileCopyTask {

    private String sourceDir;
    private String targetDir;

    public static void main(String[] args) {
        String sourcePath = "D:\\SOFT\\Apaches\\tomcats\\apache-tomcat-9.0.12\\webapps\\saiku\\repository";
        String targetPath = "C:\\SOFTCACHE\\ideaWorkspace\\rqsaiku\\core\\src\\main\\webapp/repository/";
        try {
            copyDir(sourcePath,targetPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(){

    }

    /**
     * @param sourcePath
     * @param newPath
     * @throws IOException
     */
    public static void copyDir(String sourcePath, String newPath) throws IOException {
        File file = new File(sourcePath);
        String[] filePath = file.list();

        if (!(new File(newPath)).exists()) {
            (new File(newPath)).mkdir();
        }

        for (int i = 0; i < filePath.length; i++) {
            if ((new File(sourcePath + file.separator + filePath[i])).isDirectory()) {
                copyDir(sourcePath  + file.separator  + filePath[i], newPath  + file.separator + filePath[i]);
            }

            if (new File(sourcePath  + file.separator + filePath[i]).isFile()) {
                copyFile(sourcePath + file.separator + filePath[i], newPath + file.separator + filePath[i]);
            }
        }
    }

    public static void copyFile(String oldPath, String newPath) throws IOException {
        Files.deleteIfExists(Paths.get(newPath));
        Files.copy(Paths.get(oldPath),Paths.get(newPath));
    }
}
