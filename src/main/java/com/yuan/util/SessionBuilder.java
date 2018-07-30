package com.yuan.util;

import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultRedirectStrategy;

public class SessionBuilder {

    private HttpHost proxyHost;
    private boolean isAutoRedirect = true;
    private String globalUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";
    private String globalAcceptLanguage = "en,pt-BR;q=0.4,pt;q=0.2";
    private int socketTimeout = 10000;
    private int connectTimeout = 10000;
    private int connectionRequestTimeout = 10000;
    private BasicCookieStore cookieStore;
    private CredentialsProvider credsProvider;


    public CredentialsProvider getCredsProvider() {
        return credsProvider;
    }

    public void setCredsProvider(CredentialsProvider credsProvider) {
        this.credsProvider = credsProvider;
    }

    public BasicCookieStore getCookieStore() {
        return cookieStore;
    }

    public void setCookieStore(BasicCookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    public HttpHost getProxyHost() {
        return proxyHost;
    }

    /**
     * set a http proxy host that will be used
     * @param proxyHost
     */
    public void setProxyHost(HttpHost proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * set if the http client auto redirect when get 302 response
     * default true, will use {@link org.apache.http.impl.client.LaxRedirectStrategy}
     * false, will use {@link DefaultRedirectStrategy}
     * @return
     */
    public boolean isAutoRedirect() {
        return isAutoRedirect;
    }

    public void setAutoRedirect(boolean autoRedirect) {
        isAutoRedirect = autoRedirect;
    }

    /**
     * set a global user agent, you can also change the user agent by add a header before execute request
     * @return
     */
    public String getGlobalUserAgent() {
        return globalUserAgent;
    }

    public void setGlobalUserAgent(String globalUserAgent) {
        this.globalUserAgent = globalUserAgent;
    }


    /**
     * set a global accept language, you can also change the accept language by add a header before execute request
     * @return
     */
    public String getGlobalAcceptLanguage() {
        return globalAcceptLanguage;
    }

    public void setGlobalAcceptLanguage(String globalAcceptLanguage) {
        this.globalAcceptLanguage = globalAcceptLanguage;
    }


    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }



    public Session build() throws SessionException {
        return new Session(proxyHost, isAutoRedirect, globalUserAgent, globalAcceptLanguage,
                socketTimeout, connectTimeout, connectionRequestTimeout, cookieStore, credsProvider);
    }
}
