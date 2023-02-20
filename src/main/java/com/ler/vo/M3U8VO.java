package com.ler.vo;

import java.io.File;
import java.util.List;
import lombok.Data;

/**
 * @author lww
 */
@Data
public class M3U8VO {

    private String originalUrl;
    /**
     * 最后一个 / 之前部分
     */
    private String baseUrl;

    /**
     * 域名
     */
    private String domainName;

    private List<Ts> tsList;

    private List<File> encryptFiles;

    private List<File> unEncryptFiles;

    private List<File> transFiles;

    /**
     * 总数
     */
    private Integer all;

    /**
     * 视频时长
     */
    private String time;

    /**
     * 加密方法
     */
    private String method;

    /**
     * 加密向量
     */
    private String keyIV;

    /**
     * 秘钥
     */
    private String key;

    private boolean hasEncrypt;

    public M3U8VO(List<Ts> tsList) {
        this.tsList = tsList;
    }

    public void addTs(Ts ts) {
        this.tsList.add(ts);
    }

    @Data
    public static class Ts implements Comparable<Ts> {

        private String fileName;
        private Float seconds;
        private Integer index;
        private String fileUrl;

        public Ts(String fileName, Float seconds, Integer index, String fileUrl) {
            this.fileName = fileName;
            this.seconds = seconds;
            this.index = index;
            this.fileUrl = fileUrl;
        }

        @Override
        public String toString() {
            return fileName + " (" + seconds + "sec)";
        }

        /**
         * 获取时间
         */
        public long getLongDate() {
            try {
                return Long.parseLong(fileName.substring(0, fileName.lastIndexOf(".")));
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public int compareTo(Ts o) {
            return index.compareTo(o.index);
        }
    }
}

