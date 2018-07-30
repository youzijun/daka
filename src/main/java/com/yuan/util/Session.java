package com.yuan.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Session {
    private static final Logger logger = LoggerFactory.getLogger(Session.class);

    private String defaultUserAgent;
    private String defaultAcceptLanguage;

    public CloseableHttpClient httpClient;
    public HttpClientContext httpClientContext = new HttpClientContext();

    private String nextStepUrl;
    private Map<String, String> nextStepHeaders;
    private Map<String, String> nextPostStepParams = new HashMap<>();
    private String requestBody;

    public CloseableHttpClient getHttpClient() {
        return this.httpClient;
    }


    /**
     * create a session
     * @param proxyHost http proxy host
     * @param isRedirect is auto redirect
     * @param defaultUserAgent the default user agent of httpclient
     * @param defaultAcceptLanguage the default accept language of httpclient
     */
    Session(HttpHost proxyHost, Boolean isRedirect,
            String defaultUserAgent, String defaultAcceptLanguage,
            int socketTimeout, int connectTimeout, int connectionRequestTimeout,
            BasicCookieStore cookieStore, CredentialsProvider credsProvider) throws SessionException {

        this.defaultUserAgent =  defaultUserAgent;
        this.defaultAcceptLanguage = defaultAcceptLanguage;

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .setProxy(proxyHost)
                .build();

        if (proxyHost != null) {
            logger.info("使用代理: {}", proxyHost);
        }

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        if (isRedirect) {
            //LaxRedirectStrategy可以自动重定向所有的HEAD，GET，POST请求，解除了http规范对post请求重定向的限制。
            httpClientBuilder = httpClientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
        } else {
            httpClientBuilder = httpClientBuilder.setRedirectStrategy(new DefaultRedirectStrategy());

        }


        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new SessionException("session create error", e);
        }

        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE);

        httpClient = httpClientBuilder
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCookieStore(cookieStore)
                .setDefaultCredentialsProvider(credsProvider)
                .build();

    }



    /**
     * 设置url
     *
     * @param url
     * @return
     */
    public Session next(String url) {
        this.nextStepUrl = url;
        return this;
    }

    /**
     * 设置请求头
     *
     * @param name
     * @param value
     * @return
     */
    public Session addHeader(String name, String value) {
        if (nextStepHeaders == null) {
            nextStepHeaders = new HashMap<>();
        }

        this.nextStepHeaders.put(name, value);
        return this;
    }


    /**
     * 设置请求数据
     *
     * @param name
     * @param value
     * @return
     */
    public Session addFormParam(String name, String value) {
        if (nextPostStepParams == null) {
            nextPostStepParams = new HashMap<>();
        }

        this.nextPostStepParams.put(name, value);
        return this;
    }

    public Session addRequestBody(String requesrBody) {

        this.requestBody = requesrBody;

        return this;
    }


    /**
     * 以 get 方式继续会话请求
     *
     * @return
     */
    public HttpResponse get(int requestCount) throws IOException, SessionException {
//        logger.info("请求方法: GET, 请求地址: {}",nextStepUrl);
        checkRetryCount(requestCount);
        HttpResponse response = get(httpClient, nextStepUrl, nextStepHeaders, requestCount);
        consumeNext();
        return response;
    }

    public String getString(int requestCount) throws IOException, SessionException {
//        logger.info("请求方法: GET, 请求地址: {}",nextStepUrl);
        checkRetryCount(requestCount);
        HttpResponse response = get(httpClient, nextStepUrl, nextStepHeaders, requestCount);
        String responseStr = EntityUtils.toString(response.getEntity());
        consumeNext();
//        logger.info(responseStr);
        return responseStr;
    }


    /**
     * 以 post 方式继续会话请求
     *
     * @return
     */
    public HttpResponse post(int requestCount) throws IOException, SessionException {
        checkRetryCount(requestCount);
        if (nextPostStepParams == null) {
            nextPostStepParams = new HashMap<>();
        }
//        logger.info("请求方法: POST, 请求地址: {}",nextStepUrl);
//        logger.info("请求参数: {}", transMapToString(nextPostStepParams));
        if (this.requestBody == null || this.requestBody.isEmpty()) {
            HttpResponse response = post(httpClient, nextStepUrl, nextStepHeaders, nextPostStepParams, requestCount);
            consumeNext();
            return response;
        } else {
            HttpResponse response = post(httpClient, nextStepUrl, nextStepHeaders, requestBody, requestCount);
            consumeNext();
            return response;
        }
    }

    /**
     * 以 post 方式继续会话请求
     *
     * @return
     */
    public String postStr(int requestCount) throws IOException, SessionException {
        checkRetryCount(requestCount);
        if (nextPostStepParams == null) {
            nextPostStepParams = new HashMap<>();
        }
//        logger.info("请求方法: POST,请求地址: {}",nextStepUrl);
//        logger.info("请求参数: {}", transMapToString(nextPostStepParams));
        if (this.requestBody == null || this.requestBody.isEmpty()) {
            HttpResponse response = post(httpClient, nextStepUrl, nextStepHeaders, nextPostStepParams, requestCount);
            consumeNext();
            String res = EntityUtils.toString(response.getEntity());
//            logger.info(res);
            return res;
        } else {
            HttpResponse response = post(httpClient, nextStepUrl, nextStepHeaders, requestBody, requestCount);
            consumeNext();
            String res = EntityUtils.toString(response.getEntity());
//            logger.info(res);
            return res;
        }
    }


    private void checkRetryCount(int requestCount) throws SessionException {
        if (requestCount < 0 || requestCount > 10) {
            throw new SessionException("incorrect requestCount: " + requestCount + ", should be 1 - 10");
        }
    }










    private void consumeNext() {
        nextStepUrl = null;
        nextStepHeaders = null;
        nextPostStepParams = null;
    }


    /**
     * 发送 get 请求
     *
     * @param url
     * @return
     */
    private HttpResponse get(HttpClient httpClient, String url, Map<String, String> headers, int requestCount) throws SessionException {
        HttpGet httpGet = new HttpGet(url);

        httpGet.addHeader("User-Agent", defaultUserAgent);
        httpGet.addHeader("Accept-Language", defaultAcceptLanguage);


        if (headers != null) {
            headers.forEach(httpGet::addHeader);
        }

        return executeRequest(httpClient, requestCount, httpGet);
    }



    private HttpResponse executeRequest(HttpClient httpClient, int requestCount, HttpUriRequest request) throws SessionException {
        HttpResponse httpResponse = null;

        for (int i = requestCount; i > 0; --i) {
            try {
                httpResponse = httpClient.execute(request, httpClientContext);
                break;
            } catch (IOException e) {
                if (i <= 1)  {
                    throw new SessionException("经过 " + requestCount + " 次尝试, 爬虫爬取失败", e);
                }
            }
        }

        return httpResponse;
    }


    /**
     * 发送 post 请求
     */
    private HttpResponse post(HttpClient httpClient, String url, Map<String, String> headers,
                              Map<String, String> formMap, int requestCount) throws SessionException, UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);

        httpPost.addHeader("User-Agent", defaultUserAgent);
        httpPost.addHeader("Accept-Language", defaultAcceptLanguage);

        if (headers != null) {
            headers.forEach(httpPost::addHeader);
        }

        httpPost.setEntity(new UrlEncodedFormEntity(map2Pair(formMap)));

        return executeRequest(httpClient, requestCount, httpPost);
    }



    /**
     * post 发送Request payload
     *
     * @param url         请求url
     * @param headers     请求体
     * @param requestBody 请求体
     * @throws SessionException
     */
    private HttpResponse post(HttpClient httpClient, String url, Map<String, String> headers,
                              String requestBody, int requestCount) throws SessionException {

        HttpPost httpPost = new HttpPost(url);

        httpPost.addHeader("User-Agent", defaultUserAgent);
        httpPost.addHeader("Accept-Language", defaultAcceptLanguage);

        if (headers != null) {
            headers.forEach(httpPost::addHeader);
        }

        StringEntity entity = new StringEntity(requestBody, ContentType.TEXT_PLAIN);
        httpPost.setEntity(entity);

        return executeRequest(httpClient, requestCount, httpPost);
    }


    /**
     * get cookie store
     * @return
     */
    public CookieStore getCookieStore() {
        CookieStore cookieStore = httpClientContext.getCookieStore();
        return cookieStore;
    }


    /**
     * 关闭会话
     *
     * @throws IOException
     */
    public void close() throws IOException {
        httpClient.close();
    }


    private static List<NameValuePair> map2Pair(Map<String, String> formMap) {
        final List<NameValuePair> params = new ArrayList<>();
        formMap.forEach((k, v) -> params.add(new BasicNameValuePair(k, v)));

        return params;
    }

    public String transMapToString(Map map){

        JSONObject json = new JSONObject(map);
        return json.toString();
    }
}