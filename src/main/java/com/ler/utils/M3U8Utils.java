package com.ler.utils;

import com.ler.vo.M3U8VO;
import com.ler.vo.M3U8VO.Ts;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @author lww
 */
@Slf4j
public class M3U8Utils {

    private static final Boolean DELETE_OLD = true;

    /**
     * 从下载 解密 合并 到 转换为mp4
     *
     * @param url      m3u8连接
     * @param dir      存储目录
     * @param fileName 保存的文件名
     */
    public static void download(String url, String dir, String fileName) throws Exception {
        M3U8VO vo = analyseUrl(url);
        getAllTs(vo);
        String dirTs = dir + File.separator + fileName + ".tmp";
        downloadAllTs(vo, dirTs);
        decryptAllTs(vo, dirTs);
        //先合并为ts
        mergeAllTs(vo, dirTs + File.separator + fileName + ".ts");
        //再整个转为mp4
        FfmpegUtils.convert2Mp4(dirTs + File.separator + fileName + ".ts", ".mp4", DELETE_OLD);
    }

    private static void decryptAllTs(M3U8VO vo, String tempDir) throws Exception {
        if (!vo.isHasEncrypt()) {
            vo.setUnEncryptFiles(vo.getEncryptFiles());
            return;
        }
        log.info("开始解密");
        String iv = vo.getKeyIV();
        String method = vo.getMethod();
        List<File> files = vo.getEncryptFiles();
        byte[] bytes;
        List<File> unEncryptFiles = new ArrayList<>();
        FileInputStream ins;
        FileOutputStream ous;
        for (File file : files) {
            if (file == null || file.getName().endsWith(".DS_Store")) {
                continue;
            }
            ins = new FileInputStream(file);
            int available = ins.available();
            bytes = new byte[available];
            ins.read(bytes);
            //String dir = file.getName().substring(0, file.getName().lastIndexOf("/") + 1);
            String name = file.getName().substring(file.getName().lastIndexOf("/") + 1);
            File newFile = new File(tempDir + File.separator + "0" + name);
            ous = new FileOutputStream(newFile);
            byte[] decrypt = decrypt(bytes, available, vo.getKey(), iv, method);
            if (decrypt == null) {
                ous.write(bytes, 0, available);
            } else {
                ous.write(decrypt);
            }
            ous.flush();
            ous.close();
            ins.close();
            file.delete();
            unEncryptFiles.add(newFile);
        }
        vo.setUnEncryptFiles(unEncryptFiles);
        log.info("解密完成");
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 解密ts
     *
     * @param sSrc   ts文件字节数组
     * @param length
     * @param sKey   密钥
     * @return 解密后的字节数组
     */
    public static byte[] decrypt(byte[] sSrc, int length, String sKey, String iv, String method) throws Exception {
        if (StringUtils.isNotEmpty(method) && !method.contains("AES")) {
            throw new IllegalArgumentException("未知的算法！");
        }
        // 判断Key是否正确
        if (StringUtils.isEmpty(sKey)) {
            throw new IllegalArgumentException("解密Key错误！");
        }
        // 判断Key是否为16位
        if (sKey.length() != 16) {
            throw new IllegalArgumentException("Key长度不是16位！");
        }
        //Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(sKey.getBytes(StandardCharsets.UTF_8), "AES");
        byte[] ivByte = new byte[16];
        if (iv != null) {
            if (iv.startsWith("0x")) {
                ivByte = CommonUtil.hexStringToByteArray(iv.substring(2));
            } else {
                ivByte = iv.getBytes();
            }
        }
        if (ivByte.length != 16) {
            ivByte = new byte[16];
        }
        //如果m3u8有IV标签，那么IvParameterSpec构造函数就把IV标签后的内容转成字节数组传进去
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(ivByte);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
        return cipher.doFinal(sSrc, 0, length);
    }

    private static M3U8VO analyseUrl(String m3u8Url) {
        M3U8VO vo = new M3U8VO(new ArrayList<>());
        vo.setOriginalUrl(m3u8Url);
        vo.setBaseUrl(CommonUtil.getBaseUrl(m3u8Url));
        vo.setDomainName(CommonUtil.getDomainName(m3u8Url));
        return vo;
    }

    private static M3U8VO getAllTs(M3U8VO vo) throws IOException {
        //HttpURLConnection conn = (HttpURLConnection) new URL(vo.getOriginalUrl()).openConnection(ProxyFactory.getProxy());
        HttpURLConnection conn = (HttpURLConnection) new URL(vo.getOriginalUrl()).openConnection();
        String realUrl = conn.getURL().toString();
        String baseUrl = realUrl.substring(0, realUrl.lastIndexOf("/") + 1);
        vo.setBaseUrl(baseUrl);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        float seconds = 0;
        int mIndex;
        int index = 0;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#")) {
                if (line.startsWith("#EXTINF:")) {
                    line = line.substring(8);
                    if ((mIndex = line.indexOf(",")) != -1) {
                        line = line.substring(0, mIndex);
                    }
                    try {
                        index++;
                        seconds = Float.parseFloat(line);
                    } catch (Exception e) {
                        seconds = 0;
                    }
                }
                if (line.startsWith("#EXT-X-KEY")) {
                    line = line.substring(11);
                    String[] split = line.split(",");
                    for (String s : split) {
                        String[] split1 = s.split("=");
                        if ("METHOD".equalsIgnoreCase(split1[0].trim())) {
                            vo.setMethod(split1[1].trim());
                            continue;
                        }
                        if ("URI".equalsIgnoreCase(split1[0].trim())) {
                            String trim = split1[1].replace("\"", "");
                            String key;
                            if (trim.startsWith("http")) {
                                key = HttpUtils.get(trim);
                            } else {
                                key = HttpUtils.get(vo.getDomainName() + trim);
                            }
                            vo.setKey(key);
                            vo.setHasEncrypt(true);
                            continue;
                        }
                        if ("IV".equalsIgnoreCase(split1[0].trim())) {
                            vo.setKeyIV(split1[1].trim());
                        }
                    }
                }
                continue;
            }
            //if (line.endsWith("m3u8")) {
            //    return getAllTs(analyseUrl(baseUrl + line));
            //}
            String name = line.substring(line.lastIndexOf("/") + 1);
            vo.addTs(new M3U8VO.Ts(name, seconds, index, baseUrl + name));
            seconds = 0;
        }
        reader.close();
        vo.setAll(index);
        double sum = vo.getTsList().stream().mapToDouble(Ts::getSeconds).sum();
        String time = CommonUtil.second2Time(sum);
        vo.setTime(time);
        return vo;
    }

    private static void downloadAllTs(M3U8VO vo, String dir) {
        if (vo == null) {
            return;
        }
        Integer all = vo.getAll();
        String time = vo.getTime();

        log.info("共 " + all + "个片段");
        log.info("总时长 = " + time);

        List<File> encryptFiles = new ArrayList<>();
        //设置并发数
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");
        File accountFile = new File(dir);
        if (!accountFile.exists()) {
            accountFile.mkdirs();
        }
        vo.getTsList().stream().parallel().forEach(ts -> {
            int dowmloaded = accountFile.listFiles().length;
            accountFile.delete();
            File file = null;
            try {
                log.info("共 " + all + "个片段: " + "开始下载第 " + ts.getIndex() + " 个" + " : " + ts.getFileName() + " 还剩: " + (all - dowmloaded));
                file = CommonUtil.downloadFile2Loacl(CommonUtil.add0(ts.getIndex(), 5) + ".ts", ts.getFileUrl(), dir);
            } catch (IOException e) {
                try {
                    Thread.sleep(1000);
                    log.info("共 " + all + "个片段: " + "开始下载第 " + ts.getIndex() + " 个" + " : " + ts.getFileName() + " 还剩: " + (all - dowmloaded));
                    file = CommonUtil.downloadFile2Loacl(CommonUtil.add0(ts.getIndex(), 5) + ".ts", ts.getFileUrl(), dir);
                } catch (InterruptedException | IOException ex) {
                    log.error("M3U8Utils_downloadAllTs_ex: ", ex);
                }
            }
            encryptFiles.add(file);
        });
        vo.setEncryptFiles(encryptFiles);
        log.info("文件下载完毕");
    }

    /**
     * @param vo          源目录
     * @param destination 合并之后的文件地址
     */
    private static void mergeAllTs(M3U8VO vo, String destination) throws IOException {
        List<File> files = vo.getUnEncryptFiles();
        files.sort(Comparator.comparing(File::getName));
        File des = new File(destination);
        log.info("开始合并...");
        FileOutputStream out = new FileOutputStream(des);
        byte[] bytes = new byte[4096];
        for (File file : files) {
            if (file.getName().endsWith(".DS_Store")) {
                continue;
            }
            FileInputStream in = new FileInputStream(file);
            int len;
            while ((len = in.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
            in.close();
            out.flush();
            //完成后删除
            file.delete();
        }
        out.close();
        log.info("合并完成");
    }

}
