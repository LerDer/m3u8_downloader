package com.ler.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;

/**
 * @author lww
 */
@Slf4j
public class FfmpegUtils {

    /**
     * 本地需要安装ffmpeg
     *
     * @param file 文件路径
     * @param type 转换后的格式 .mp4
     */
    public static File convert2Mp4(String file, String type, boolean deleteOld) throws IOException {
        log.info("开始转码...");
        long l = System.currentTimeMillis();
        if (!checkfile(file)) {
            log.error(file + " is not file");
            return null;
        }
        //String subFileName = file.substring(0, file.lastIndexOf("."));
        File pFile = new File(file).getParentFile();
        String ppFile = pFile.getParentFile().toString();

        String fileName = file.substring(file.lastIndexOf("/") + 1);
        String fileNameSub = fileName.substring(0, fileName.lastIndexOf("."));

        List<String> commend = new ArrayList<>();
        commend.add("ffmpeg");
        commend.add("-i");
        commend.add(file);
        //commend.add("-ab");
        //commend.add("64");
        //commend.add("-acodec");
        //commend.add("mp3");
        //commend.add("-ac");
        //commend.add("2");
        //commend.add("-ar");
        //commend.add("22050");
        //commend.add("-b");
        //commend.add("230");
        //commend.add("-r");
        //commend.add("24");
        //commend.add("-y");
        commend.add(ppFile + "/" + fileNameSub + type);

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commend);
        Process p = builder.start();
        doWaitFor(p);
        p.destroy();
        if (deleteOld) {
            deleteFile(pFile);
        }
        long l1 = System.currentTimeMillis() - l;
        log.info("转码完成,耗时: " + (l1 / 1000) + "秒");
        return new File(ppFile + "/" + fileNameSub + type);
    }

    private static void deleteFile(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else {
                file.delete();
            }
        }
    }

    private static int doWaitFor(Process p) {
        // returned to caller when p is finished
        int exitValue = -1;
        try (
                InputStream in = p.getInputStream();
                InputStream err = p.getErrorStream();
        ) {
            System.out.println("comeing");
            // Set to true when p is finished
            boolean finished = false;
            while (!finished) {
                try {
                    while (in.available() > 0) {
                        Character c = (char) in.read();
                        System.out.print(c);
                    }
                    while (err.available() > 0) {
                        Character c = (char) err.read();
                        System.out.print(c);
                    }
                    exitValue = p.exitValue();
                    finished = true;
                } catch (IllegalThreadStateException e) {
                    Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            System.err.println("doWaitFor();: unexpected exception - " + e.getMessage());
        }
        return exitValue;
    }

    /**
     * 检查文件是否存在
     *
     * @param path
     * @return
     */
    private static boolean checkfile(String path) {
        File file = new File(path);
        return file.isFile();
    }

}
