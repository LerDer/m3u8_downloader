package com.ler.utils;

import com.ler.exception.UserFriendlyException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author lww
 */
@Slf4j
public class CommonUtil {

    private static int CONN_TIMEOUT = 30 * 60 * 1000;
    private static int READ_TIMEOUT = 30 * 60 * 1000;

    public static void isTrue(Boolean ex, String msg) {
        if (!ex) {
            throw new UserFriendlyException(msg);
        }
    }

    public static String second2Time(Double sum) {
        String ssum = sum.toString();
        int isum;
        if (ssum.contains(".")) {
            isum = Integer.parseInt(ssum.substring(0, ssum.indexOf(".")));
        } else {
            isum = Integer.parseInt(ssum);
        }
        int i = isum / 60;
        int i1 = isum % 60;
        return i + "分钟" + i1 + "秒";
    }

    public static String add0(Integer val, Integer bit) {
        int length = val.toString().length();
        if (length < bit) {
            int i = bit - length;
            switch (i) {
                case 1:
                    return "0" + val;
                case 2:
                    return "00" + val;
                case 3:
                    return "000" + val;
                case 4:
                    return "0000" + val;
                case 5:
                    return "000000" + val;
                case 6:
                    return "0000000" + val;
                case 7:
                    return "00000000" + val;
                case 8:
                    return "000000000" + val;
                case 9:
                    return "0000000000" + val;
                default:
                    return "" + val;
            }
        }
        return "";
    }

    public static String getDomainName(String url) {
        if (url.contains(":")) {
            String[] split = url.split(":");
            String pre = split[0];
            String substring = url.substring(url.indexOf("//") + 2);
            String domain = substring.substring(0, substring.indexOf("/"));
            return pre + "://" + domain;
        } else {
            CommonUtil.isTrue(false, "非法链接！");
        }
        return "";
    }

    public static String getBaseUrl(String url) {
        return url.substring(0, url.lastIndexOf("/") + 1);
    }

    public static boolean isUrl(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        str = str.trim();
        return str.matches("^(http|https)://.+");
    }

    public static String convertToDownloadSpeed(BigDecimal bigDecimal, int scale) {
        BigDecimal unit = new BigDecimal(1);
        BigDecimal kb = new BigDecimal(1 << 10);
        BigDecimal mb = new BigDecimal(1 << 10).multiply(kb);
        BigDecimal gb = new BigDecimal(1 << 10).multiply(mb);
        BigDecimal tb = new BigDecimal(1 << 10).multiply(gb);
        BigDecimal pb = new BigDecimal(1 << 10).multiply(tb);
        BigDecimal eb = new BigDecimal(1 << 10).multiply(pb);
        if (bigDecimal.divide(kb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0) {
            return bigDecimal.divide(unit, scale, BigDecimal.ROUND_HALF_UP).toString() + " B";
        } else if (bigDecimal.divide(mb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0) {
            return bigDecimal.divide(kb, scale, BigDecimal.ROUND_HALF_UP).toString() + " KB";
        } else if (bigDecimal.divide(gb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0) {
            return bigDecimal.divide(mb, scale, BigDecimal.ROUND_HALF_UP).toString() + " MB";
        } else if (bigDecimal.divide(tb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0) {
            return bigDecimal.divide(gb, scale, BigDecimal.ROUND_HALF_UP).toString() + " GB";
        } else if (bigDecimal.divide(pb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0) {
            return bigDecimal.divide(tb, scale, BigDecimal.ROUND_HALF_UP).toString() + " TB";
        } else if (bigDecimal.divide(eb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0) {
            return bigDecimal.divide(pb, scale, BigDecimal.ROUND_HALF_UP).toString() + " PB";
        }
        return bigDecimal.divide(eb, scale, BigDecimal.ROUND_HALF_UP).toString() + " EB";
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if ((len & 1) == 1) {
            s = "0" + s;
            len++;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String date2String(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    public static Date string2Date(String date) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.parse(date);
    }

    public static File downloadFile2Loacl(String fileName, String fileUrl, String path) throws IOException {
        Map<String, Object> headers = new HashMap<>(16);
        headers.put("Connection", "keep-alive");
        headers.put("Host", "aweme.snssdk.com");
        headers.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16D57 Version/12.0 Safari/604.1");

        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONN_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            conn.addRequestProperty(entry.getKey(), entry.getValue().toString());
        }
        conn.setDoInput(true);
        conn.setUseCaches(false);
        InputStream in = conn.getInputStream();
        File file = new File(path + "/" + fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        int b;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        out.close();//关闭输出流
        out.flush();
        in.close(); //关闭输入流
        return file;
    }
}
