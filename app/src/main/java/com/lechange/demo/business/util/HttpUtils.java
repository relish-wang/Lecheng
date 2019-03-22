package com.lechange.demo.business.util;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author 31833
 * @version Version    Time                 Description<br>
 *          1.0        2017/3/4 13:45      封装HTTP的相关请求
 */

public class HttpUtils {

    private static final String ALLOWED_URL_CHARS = "@#&=*+-_.,:!?()/~'%";

    private static final String CHARSET = "UTF-8";

    private static final int connectTimeout = 5 * 1000;

    private static final int readTimout = 10 * 1000;

    private static HttpUtils instance;

    private TrustManager[] trustAllCerts = {new TrustAllX509TrustManager()};

    public static HttpUtils getInstance() {
        if (instance == null) {
            synchronized (HttpUtils.class) {
                if (instance == null) {
                    instance = new HttpUtils();
                }
            }
        }
        return instance;
    }

    private HttpUtils() {

        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new NullhostNameVerifier());
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpURLConnection.setFollowRedirects(true);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public HttpURLConnection createConnection(String url) throws IOException {

        String encodeUrl = Uri.encode(url, ALLOWED_URL_CHARS);
        HttpURLConnection conn = (HttpURLConnection) new URL(encodeUrl).openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimout);

        return conn;
    }

    public InputStream getInputStream(String url) {
        InputStream is = null;

        try {
            HttpURLConnection conn = createConnection(url);
            conn.setRequestMethod("GET");
            is = conn.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is;
    }

    public String getString(String url) {
        String result = null;
        InputStream is = null;
        BufferedReader br = null;

        try {
            is = getInputStream(url);
            br = new BufferedReader(new InputStreamReader(is, CHARSET));
            String line = null;
            StringBuffer sb = new StringBuffer();

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            result = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {

            }

            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {

            }

        }
        return result;
    }

    public String postString(String url, String params) {
        String result = null;
        OutputStream os = null;
        InputStream is = null;
        BufferedReader br = null;

        try {
            HttpURLConnection conn = createConnection(url);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);

            conn.setRequestProperty("Content_Type", "application/json;charset= " + CHARSET);

            if (params != null) {
                os = conn.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                dos.write(params.getBytes(CHARSET));
                dos.flush();
                dos.close();
                ;
            }

            is = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(is, CHARSET));
            String line = null;

            StringBuffer sb = new StringBuffer();

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            result = sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }


    private boolean shouldBeProcessed(HttpURLConnection conn) throws IOException {
        return conn.getResponseCode() == 200;
    }

    private static class NullhostNameVerifier implements HostnameVerifier {

        public NullhostNameVerifier() {

        }

        @Override
        public boolean verify(String host, SSLSession session) {
           return true;
        }
    }

    private static class TrustAllX509TrustManager implements X509TrustManager {


        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
