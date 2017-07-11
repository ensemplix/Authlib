package com.mojang.authlib;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Map;

import static org.apache.commons.io.Charsets.UTF_8;

public abstract class HttpAuthenticationService extends BaseAuthenticationService {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Proxy proxy;
    
    protected HttpAuthenticationService(Proxy proxy) {
        super();

        Validate.notNull(proxy);
        this.proxy = proxy;
    }
    
    public Proxy getProxy() {
        return this.proxy;
    }
    
    protected HttpURLConnection createUrlConnection(URL url) throws IOException {
        Validate.notNull(url);

        LOGGER.debug("Opening connection to " + url);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);

        return connection;
    }
    
    public String performPostRequest(URL url, String post, String contentType) throws IOException {
        Validate.notNull(url);
        Validate.notNull(post);
        Validate.notNull(contentType);

        byte[] postAsBytes = post.getBytes(UTF_8);

        HttpURLConnection connection = createUrlConnection(url);
        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", "" + postAsBytes.length);
        connection.setDoOutput(true);

        LOGGER.debug("Writing POST data to " + url + ": " + post);

        try(OutputStream outputStream = connection.getOutputStream()) {
            IOUtils.write(postAsBytes, outputStream);
        }

        LOGGER.debug("Reading data from " + url);

        try(InputStream inputStream = connection.getInputStream()) {
            String result = IOUtils.toString(inputStream, UTF_8);

            LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
            LOGGER.debug("Response: " + result);

            return result;
        } catch(IOException e) {
            InputStream errorStream = connection.getErrorStream();

            if(errorStream != null) {
                LOGGER.debug("Reading error page from " + url);

                String result2 = IOUtils.toString(errorStream, UTF_8);
                LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                LOGGER.debug("Response: " + result2);

                return result2;
            }

            LOGGER.debug("Request failed", e);
            throw e;
        }
    }
    
    public String performGetRequest(URL url) throws IOException {
        Validate.notNull(url);

        HttpURLConnection connection = this.createUrlConnection(url);
        LOGGER.debug("Reading data from " + url);

        try(InputStream inputStream = connection.getInputStream()) {
            String result = IOUtils.toString(inputStream, UTF_8);

            LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
            LOGGER.debug("Response: " + result);

            return result;
        } catch(IOException e) {
            InputStream errorStream = connection.getErrorStream();

            if(errorStream != null) {
                LOGGER.debug("Reading error page from " + url);

                String result2 = IOUtils.toString(errorStream, UTF_8);
                LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                LOGGER.debug("Response: " + result2);

                return result2;
            }

            LOGGER.debug("Request failed", e);
            throw e;
        }
    }

    public static URL constantURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException ex) {
            throw new Error("Couldn't create constant for " + url, ex);
        }
    }

    public static String buildQuery(Map<String, Object> query) {
        if(query == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for(Map.Entry<String, Object> entry : query.entrySet()) {
            if(builder.length() > 0) {
                builder.append('&');
            }

            try{
                builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            } catch(UnsupportedEncodingException e) {
                LOGGER.error("Unexpected exception building query", e);
            }

            if(entry.getValue() != null) {
                builder.append('=');

                try {
                    builder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
                } catch(UnsupportedEncodingException e) {
                    LOGGER.error("Unexpected exception building query", e);
                }
            }
        }

        return builder.toString();
    }
    
    public static URL concatenateURL(final URL url, final String query) {
        try {
            if(url.getQuery() != null && url.getQuery().length() > 0) {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&" + query);
            }

            return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + query);
        } catch(MalformedURLException ex) {
            throw new IllegalArgumentException("Could not concatenate given URL with GET arguments!", ex);
        }
    }

}
