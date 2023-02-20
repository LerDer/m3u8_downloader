package com.ler.utils;

import com.alibaba.fastjson.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.activation.MimetypesFileTypeMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Http工具类
 *
 * @author lww
 */
@Slf4j
public class HttpUtils {

    private static final String PNG_END = ".png";
    private static final String JPG_END = ".jpg";
    private static final String ENCODE = "UTF-8";

    /**
     * 发送get请求
     */
    public static String get(String url) {
        String result = "";
        InputStream in = null;
        try {
            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("GET");
            // 建立实际的连接
            conn.connect();
            // 定义输入流来读取URL的响应
            in = conn.getInputStream();
            result = StreamUtils.copyToString(in, Charset.forName("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 发送post请求
     */
    public static String post(String url, String paramStr) {
        InputStream in = null;
        OutputStream os = null;
        String result = "";
        try {
            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection) new URL(url)
                    .openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            // 发送POST请求须设置
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            os = conn.getOutputStream();
            // 注意编码格式，防止中文乱码
            if (StringUtils.hasText(paramStr)) {
                os.write(paramStr.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
            in = conn.getInputStream();
            result = StreamUtils.copyToString(in, Charset.forName("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    private HttpUtils() {
    }

    /**
     * 向指定URL发送GET方法的请求 http
     *
     * @param url     发送请求的URL
     * @param param   请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param headers 可为null
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGetHttp(String url, String param, Map<String, String> headers) {
        HttpGet httpGet = new HttpGet(StringUtils.isEmpty(param) ? url : url + "?" + param);
        headers = initHeader(headers);
        //设置header
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpGet.setHeader(entry.getKey(), entry.getValue());
        }
        String content = null;
        try (CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build()) {
            CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();
            content = EntityUtils.toString(entity, ENCODE);
        } catch (IOException e) {
            log.error("HttpRequest_getForm_e:{}", e);
        }
        return content;
    }

    /**
     * 向指定URL发送GET方法的请求 https
     *
     * @param url     发送请求的URL
     * @param param   请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param headers 可为null
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGetHttps(String url, String param, Map<String, String> headers) {
        HttpGet httpGet = new HttpGet(StringUtils.isEmpty(param) ? url : url + "?" + param);
        headers = initHeader(headers);
        //设置header
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpGet.setHeader(entry.getKey(), entry.getValue());
        }
        String content = null;
        try (CloseableHttpClient closeableHttpClient = sslHttpClientBuild()) {
            CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();
            content = EntityUtils.toString(entity, ENCODE);
        } catch (IOException e) {
            log.error("HttpRequest_getForm_e:{}", e);
        }
        return content;
    }

    /**
     * 向指定 URL 发送POST方法的请求 form参数 http
     *
     * @param url     发送请求的 URL
     * @param param   请求参数，请求参数可以 ?name1=value1&name2=value2 拼在url后，也可以放在param中。
     * @param headers 可为null
     * @return 所代表远程资源的响应结果
     */
    public static String sendPostFormHttp(String url, Map<String, String> param, Map<String, String> headers) {
        HttpPost httpPost = new HttpPost(url);
        headers = initHeader(headers);
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }
        String content = null;
        List<NameValuePair> pairList = new ArrayList<>();
        if (param != null) {
            for (Map.Entry<String, String> entry : param.entrySet()) {
                pairList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        try (CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build()) {
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, ENCODE));
            CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            content = EntityUtils.toString(entity, ENCODE);
        } catch (IOException e) {
            log.error("HttpRequest_getForm_e:{}", e);
        }
        return content;
    }

    /**
     * 向指定 URL 发送POST方法的请求 form参数 https
     *
     * @param url     发送请求的 URL
     * @param param   请求参数，请求参数可以 ?name1=value1&name2=value2 拼在url后，也可以放在param中。
     * @param headers 可为null
     * @return 所代表远程资源的响应结果
     */
    public static String sendPostFormHttps(String url, Map<String, String> param, Map<String, String> headers) {
        HttpPost httpPost = new HttpPost(url);
        headers = initHeader(headers);
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }
        String content = null;
        List<NameValuePair> pairList = new ArrayList<>();
        if (param != null) {
            for (Map.Entry<String, String> entry : param.entrySet()) {
                pairList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        try (CloseableHttpClient closeableHttpClient = sslHttpClientBuild()) {
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, ENCODE));
            CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            content = EntityUtils.toString(entity, ENCODE);
        } catch (IOException e) {
            log.error("HttpRequest_getForm_e:{}", e);
        }
        return content;
    }

    /**
     * 发送post，参数为json字符串 放在body中 requestBody http
     *
     * @param url     url
     * @param params  参数
     * @param headers 可为null
     */
    public static String sendPostJsonHttp(String url, JSONObject params, Map<String, String> headers) {
        HttpPost httpPost = new HttpPost(url);
        headers = initHeader(headers);
        headers.put("Content-Type", "application/json");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }
        StringEntity stringEntity = new StringEntity(params.toString(), ENCODE);
        httpPost.setEntity(stringEntity);
        String content = null;
        try (CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build()) {
            CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            content = EntityUtils.toString(entity, ENCODE);
        } catch (IOException e) {
            log.error("HttpUtil_sendPostJsonHttp_e:{}", e);
        }
        return content;
    }

    /**
     * 发送post，参数为json字符串 放在body中 requestBody https
     *
     * @param url     url
     * @param params  参数
     * @param headers 可为null
     */
    public static String sendPostJsonHttps(String url, JSONObject params, Map<String, String> headers) {
        HttpPost httpPost = new HttpPost(url);
        headers = initHeader(headers);
        headers.put("Content-Type", "application/json");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }
        StringEntity stringEntity = new StringEntity(params.toString(), ENCODE);
        httpPost.setEntity(stringEntity);
        String content = null;
        try (CloseableHttpClient closeableHttpClient = sslHttpClientBuild()) {
            CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            content = EntityUtils.toString(entity, ENCODE);
        } catch (IOException e) {
            log.error("HttpUtil_sendPostJsonHttps_e:{}", e);
        }
        return content;
    }

    /**
     * 发送post，返回数据流 参数为json字符串 放在body中
     */
    public static BufferedInputStream getStream(String actionUrl) {
        BufferedInputStream is;
        try {
            // 创建连接
            URL url = new URL(actionUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 允许输入
            connection.setDoOutput(true);
            // 允许输出
            connection.setDoInput(true);
            // 设置请求方式
            connection.setRequestMethod("GET");
            connection.connect();
            // 读取响应, 服务器端无权限创建file文件
            is = new BufferedInputStream(connection.getInputStream());
            return is;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("生成失败！");
    }

    /**
     * 发送post，返回数据流 参数为json字符串 放在body中
     */
    public static BufferedInputStream postJSON(String actionUrl, JSONObject params) {

        PrintWriter pw = null;
        BufferedInputStream is;
        try {
            // 创建连接
            URL url = new URL(actionUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 允许输入
            connection.setDoOutput(true);
            // 允许输出
            connection.setDoInput(true);
            // 设置请求方式
            connection.setRequestMethod("POST");
            connection.connect();

            pw = new PrintWriter(connection.getOutputStream());
            pw.write(params.toString());
            pw.close();

            // 读取响应, 服务器端无权限创建file文件
            is = new BufferedInputStream(connection.getInputStream());
            return is;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new IllegalArgumentException("生成失败！");
    }

    /**
     * 上传图片
     */
    public static String formUpload(String urlStr, File file) {
        String res = "";
        HttpURLConnection conn = null;
        // boundary就是request头和上传文件内容的分隔符
        String boundary = "---------------------------123821742118716";
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            OutputStream out = new DataOutputStream(conn.getOutputStream());
            // file
            String filename = file.getName();
            String contentType = new MimetypesFileTypeMap().getContentType(file);
            if (filename.endsWith(PNG_END) || filename.endsWith(JPG_END)) {
                contentType = "image/png";
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(contentType)) {
                contentType = "application/octet-stream";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("\r\n").append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"" + "buffer" + "\"; filename=\"").append(filename).append("\"\r\n");
            sb.append("Content-Type:").append(contentType).append("\r\n\r\n");
            out.write(sb.toString().getBytes());
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            int bytes;
            byte[] bufferOut = new byte[1024];
            while ((bytes = in.read(bufferOut)) != -1) {
                out.write(bufferOut, 0, bytes);
            }
            in.close();
            byte[] endData = ("\r\n--" + boundary + "--\r\n").getBytes();
            out.write(endData);
            out.flush();
            out.close();
            // 读取返回数据
            StringBuilder resSb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                resSb.append(line).append("\n");
            }
            res = resSb.toString();
            reader.close();
        } catch (Exception e) {
            System.out.println("发送POST请求出错。" + urlStr);
            e.printStackTrace();
        } finally {
            file.deleteOnExit();
            if (conn != null) {
                conn.disconnect();
            }
        }
        return res;
    }

    private static Map<String, String> initHeader(Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<>(16);
        }
        headers.put("accept", "*/*");
        headers.put("connection", "Keep-Alive");
        headers.put("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        return headers;
    }

    private static CloseableHttpClient sslHttpClientBuild() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                        .register("https", trustAllHttpsCertificates()).build();
        //创建ConnectionManager，添加Connection配置信息
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }

    private static SSLConnectionSocketFactory trustAllHttpsCertificates() {
        SSLConnectionSocketFactory socketFactory = null;
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new Mitm();
        trustAllCerts[0] = tm;
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, null);
            socketFactory = new SSLConnectionSocketFactory(sc, NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("HttpUtil_trustAllHttpsCertificates_e:{}", e);
        }
        return socketFactory;
    }

    static class Mitm implements TrustManager, X509TrustManager {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
            //don't check
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
            //don't check
        }
    }
}