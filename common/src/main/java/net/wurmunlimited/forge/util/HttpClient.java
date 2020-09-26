package net.wurmunlimited.forge.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HttpClient {

    private static final Logger logger = Logger.getLogger(HttpClient.class.getName());

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 10000;

    private static final String GET = "GET";
    private static final String POST = "POST";

    public static boolean download(String url,Path file) {
        try {
            URL u = new URL(url);
            InputStream in = u.openStream();
            Files.copy(in,file,StandardCopyOption.REPLACE_EXISTING);
            in.close();
            return true;
        } catch(IOException e) {
            logger.log(Level.SEVERE,"HttpClient.download: "+e.getMessage(),e);
        }
        return false;
    }

    private int status;
    private Map<String,List<String>> responseHeaders;
    private String response;

    public int request(String url) {
        return request(url,null);
    }

    public int request(String url,String data) {
        try {
            logger.info("HttpClient Request(url: "+url+")");
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)u.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setInstanceFollowRedirects(false);
            if(data==null || data.length()==0) {
                conn.setRequestMethod(GET);
                conn.setDoOutput(false);
                conn.connect();
            } else {
                String encodedData = URLEncoder.encode(data,"UTF-8");
                conn.setRequestMethod(POST);
                conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length",String.valueOf(encodedData.length()));
                conn.setDoOutput(true);
                conn.connect();
                OutputStream os = conn.getOutputStream();
                os.write(encodedData.getBytes());
                os.flush();
                os.close();
            }
            status = conn.getResponseCode();
            responseHeaders = conn.getHeaderFields();
            logger.info("HttpClient Request(status: "+status+")");
            if(status!=204) {
                InputStream is = status<400? conn.getInputStream() : conn.getErrorStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = br.readLine())!=null)
                    sb.append(line).append("\n");
                isr.close();
                br.close();
                response = sb.toString();
                logger.info("HttpClient Request(response: "+response+")");
            }
            conn.disconnect();
        } catch(IOException e) {
            logger.log(Level.SEVERE,"HttpClient.request: "+e.getMessage(),e);
        }
        return status;
    }

    public int getStatus() { return status; }

    public Map<String,List<String>> getResponseHeaders() { return responseHeaders; }

    public String getResponse() { return response; }
}

