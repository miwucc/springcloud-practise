package com.neo.insaware;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * httpUtils,基于HttpClient-4.5最新写法，同一个host支持同时最大并发http链接数为10，总数最高为200
 * 暂时没有支持https请求
 *
 * @Description:
 * @author: xionglin
 * @date 2018年5月4日下午3:57:05
 */
public class HttpUtils {

    private final static Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static int MAX_TOTAL = 50;//总体可以存在的链接数量，默认值为20，如果设置小于默认值会自动设为默认值
    private static int DEFAULT_MAXPERROUTE = 5;//每一个h请求的ost(域名，如果请求的是host链接了path那么也会自动解析出真正的域名来算连接量)最多可以有多少个链接，默认值为2，如果设置小于默认值会自动设为默认值,也可以单独在链接管理器cm中设置指定host的最大链接并发数cm.setMaxPerRoute(new HttpRoute(localhost), 50);
    //基本可以认为DEFAULT_MAXPERROUTE这个值是一个host能同时支持的最大并发线程数
    private static String DEFAULT_CHARSET = "UTF-8";

    private static int POOL_GET_TIMEOUT = 2 * 1000;//从连接池取链接超时时间，从连接池中获取连接的超时时间，超过该时间未拿到可用连接，默认每个host的最大链接数是2,会抛出org.apache.http.conn.ConnectionPoolTimeoutException: Timeout waiting for connection from pool
    private static int CONNECT_TIMEOUT = 2 * 1000;//connectTimeout:连接上服务器(握手成功)的时间，超出该时间抛出connect timeout
    private static int SOCKET_TIMEOUT = 2 * 1000;//服务器数据超时时间，超过这个时间没收到返回则抛出异常

    //在高并发的情况下，如果短时间有大量的线程并发建立http链接，1可能短时间占用大量端口，2每次重复进行三次握手和链接断开时候的4次挥手都非常消耗资源。
    //所以这里采用PoolingHttpClientConnectionManager进行管理，这也是httpClient4.5之后的custom创建默认处理方式，非常适合并发情况，比如爬虫,多个线程使用一个httpClient实例（一个连接池）就可以了
    private static PoolingHttpClientConnectionManager clientConnectionManager;//连接池管理器
    private static RequestConfig requestConfig;//请求参数配置
    private static HttpRequestRetryHandler httpRequestRetryHandler;//重试handler

    //静态初始化
    static {
        //设置连接池
        clientConnectionManager = new PoolingHttpClientConnectionManager();
        clientConnectionManager.setMaxTotal(MAX_TOTAL);
        clientConnectionManager.setDefaultMaxPerRoute(DEFAULT_MAXPERROUTE);
        //设置请求参数
        requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(POOL_GET_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT).build();
        //设置重试策略
        httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount >= 2) {// 如果已经重试了2次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时 不重试
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达   不重试
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝  不重试
                    return false;
                }
                if (exception instanceof SSLException) {// ssl握手异常 不重试
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };
    }


    /**
     * 返回一个新的非池化链接httpClient
     */
    public static CloseableHttpClient createNoPoolClient() {
        return HttpClients.createDefault();
    }

    /**
     * 创建指定参数的池化httpClient
     */
    public static CloseableHttpClient createPoolClient(int maxConnectTotalNum, int maxConnectPerHost, int connectTimeout, int socketTimeOut) {

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxConnectTotalNum);
        cm.setDefaultMaxPerRoute(maxConnectPerHost);

        RequestConfig rc = RequestConfig.custom()
                .setConnectionRequestTimeout(POOL_GET_TIMEOUT)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeOut).build();

        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(rc)
                .setRetryHandler(httpRequestRetryHandler)
                .build();
    }

    /**
     * 创建默认参数的httpclient
     */
    public static CloseableHttpClient createPoolClient() {
        return HttpClients.custom()
                .setConnectionManager(clientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(httpRequestRetryHandler)
                .build();
    }

    /**
     * 获取自己指定参数的池化client，可以在cm中单独指定某host最大并发连接数  ,示例:
     * HttpHost localhost = new HttpHost("http://baidu.com",80);
     * cm.setMaxPerRoute(new HttpRoute(localhost), 50);
     */
    public static CloseableHttpClient createPoolClient(PoolingHttpClientConnectionManager cm, RequestConfig requestConfig) {
        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(httpRequestRetryHandler)
                .build();
    }

//	/**包装http请求，并进行默认配置*/
//	private static CloseableHttpClient wrapClient(CloseableHttpClient httpClient) {
////		if (host.startsWith("https://")) {
////			sslClient(getHttpClient());
////		}
////		return getHttpClient(usePool);
//		return httpClient;
//	}

    /**
     * get,传入自己的client
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse doGet(CloseableHttpClient httpClient, String host, String path,
                                              Map<String, String> headers,
                                              Map<String, String> querys)
            throws Exception {
        HttpGet request = new HttpGet(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }
        return httpClient.execute(request);
    }

    /**
     * 默认返回string的Get，只需要关心返回值，异常情况返回null
     */
    public static String doGetStringRsp(
            CloseableHttpClient httpClient,
            String host, String path, String method,
            Map<String, String> headers,
            Map<String, String> querys) {
        String respStr = null;
        try {
            HttpGet request = new HttpGet(buildUrl(host, path, querys));
            for (Map.Entry<String, String> e : headers.entrySet()) {
                request.addHeader(e.getKey(), e.getValue());
            }
            CloseableHttpResponse response = httpClient.execute(request);
            int httpStatus = response.getStatusLine().getStatusCode();
            if (httpStatus / 100 == 2) {//20x状态才说明访问成功
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    respStr = EntityUtils.toString(entity, DEFAULT_CHARSET);
                }
                EntityUtils.consume(entity);
            } else {
                log.error("doStringRspGet请求" + host + "发生返回状态!=20X,返回=", httpStatus);
            }
            return respStr;
        } catch (Exception e1) {
            log.error("doStringRspGet请求" + host + "发生异常!", e1);
            return respStr;
        }
    }

    /**
     * 默认返回string的Post，只需要关心返回值，异常情况返回null
     */
    public static String doPostStringRsp(
            CloseableHttpClient httpClient,
            String host, String path,
            Map<String, String> headers,
            Map<String, String> querys,
            String body) {

        String respStr = null;
        try {
            HttpPost request = new HttpPost(buildUrl(host, path, querys));
            for (Map.Entry<String, String> e : headers.entrySet()) {
                request.addHeader(e.getKey(), e.getValue());
            }

            if (StringUtils.isNotBlank(body)) {
                request.setEntity(new StringEntity(body, DEFAULT_CHARSET));
            }

            CloseableHttpResponse response = httpClient.execute(request);
            int httpStatus = response.getStatusLine().getStatusCode();
            if (httpStatus / 100 == 2) {//20x状态才说明访问成功
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    respStr = EntityUtils.toString(entity, DEFAULT_CHARSET);
                }
                EntityUtils.consume(entity);
            } else {
                log.error("doStringRspPost请求" + host + "发生返回状态!=20X,返回=", httpStatus);
            }
            return respStr;
        } catch (Exception e1) {
            log.error("doStringRspPost请求" + host + "发生异常!", e1);
            return respStr;
        }
    }


    /**
     * post form
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param bodys
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(CloseableHttpClient httpClient, String host, String path,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      Map<String, String> bodys)
            throws Exception {

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (bodys != null) {
            List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();

            for (String key : bodys.keySet()) {
                nameValuePairList.add(new BasicNameValuePair(key, bodys.get(key)));
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairList, DEFAULT_CHARSET);
            formEntity.setContentType("application/x-www-form-urlencoded; charset=" + DEFAULT_CHARSET);
            request.setEntity(formEntity);
        }

        return httpClient.execute(request);
    }

    /**
     * Post String
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(CloseableHttpClient httpClient, String host, String path,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      String body)
            throws Exception {

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, DEFAULT_CHARSET));
        }

        return httpClient.execute(request);
    }

    /**
     * Post stream
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(CloseableHttpClient httpClient, String host, String path,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      byte[] body)
            throws Exception {
        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    /**
     * Put String
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(CloseableHttpClient httpClient, String host, String path,
                                     Map<String, String> headers,
                                     Map<String, String> querys,
                                     String body)
            throws Exception {

        HttpPut request = new HttpPut(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, DEFAULT_CHARSET));
        }

        return httpClient.execute(request);
    }

    /**
     * Put stream
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(CloseableHttpClient httpClient, String host, String path,
                                     Map<String, String> headers,
                                     Map<String, String> querys,
                                     byte[] body)
            throws Exception {
        HttpPut request = new HttpPut(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    /**
     * Delete
     *
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @return
     * @throws Exception
     */
    public static HttpResponse doDelete(CloseableHttpClient httpClient, String host, String path,
                                        Map<String, String> headers,
                                        Map<String, String> querys)
            throws Exception {
        HttpDelete request = new HttpDelete(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return httpClient.execute(request);
    }

    private static String buildUrl(String host, String path, Map<String, String> querys) throws UnsupportedEncodingException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(host);
        if (!StringUtils.isBlank(path)) {
            sbUrl.append(path);
        }
        if (null != querys) {
            StringBuilder sbQuery = new StringBuilder();
            for (Map.Entry<String, String> query : querys.entrySet()) {
                if (0 < sbQuery.length()) {
                    sbQuery.append("&");
                }
                if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
                    sbQuery.append(query.getValue());
                }
                if (!StringUtils.isBlank(query.getKey())) {
                    sbQuery.append(query.getKey());
                    if (!StringUtils.isBlank(query.getValue())) {
                        sbQuery.append("=");
                        sbQuery.append(URLEncoder.encode(query.getValue(), DEFAULT_CHARSET));
                    }
                }
            }
            if (0 < sbQuery.length()) {
                sbUrl.append("?").append(sbQuery);
            }
        }

        return sbUrl.toString();
    }

    public static HttpResponse doPostJson(CloseableHttpClient client, String host, String path,
                                           Map<String, String> headers, Map<String, String> querys,
                                           JSONObject jsonObject) throws IOException {

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }
        request.addHeader("Content-Type","application/json;charset=utf-8");
        request.setHeader("Accept","application/json");
        request.setEntity(new StringEntity(jsonObject.toString(), Charset.forName("UTF-8")));
        HttpResponse res = client.execute(request);
        return res;
    }


//		/**https这个方法还没试过，先写在这儿看*/
//		private static void sslClient(HttpClient httpClient) {
//	        try {
//	            SSLContext ctx = SSLContext.getInstance("TLS");
//	            X509TrustManager tm = new X509TrustManager() {
//	                public X509Certificate[] getAcceptedIssuers() {
//	                    return null;
//	                }
//	                public void checkClientTrusted(X509Certificate[] xcs, String str) {
//	                	
//	                }
//	                public void checkServerTrusted(X509Certificate[] xcs, String str) {
//	                	
//	                }
//	            };
//	            ctx.init(null, new TrustManager[] { tm }, null);
//	            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
//	            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//	            ClientConnectionManager ccm = httpClient.getConnectionManager();
//	            SchemeRegistry registry = ccm.getSchemeRegistry();
//	            registry.register(new Scheme("https", 443, ssf));
//	        } catch (KeyManagementException ex) {
//	            throw new RuntimeException(ex);
//	        } catch (NoSuchAlgorithmException ex) {
//	        	throw new RuntimeException(ex);
//	        }
//	    }


/**
 * 使用池化http链接，必须要在服务器支持keep-alive的情况下才能达到链接重用的效果
 * 比如服务器设置了keep-alive=180,则180秒内这个链接服务器不会关闭，则客户端连接池则可以重用这个链接
 * 在服务器启用了keep-alive的情况下，如果客户端不使用连接池，则会迅速占用服务器连接资源，比如上面180秒内服务器都不发起关闭连接，则180秒内会占用服务器连接
 * 这种情况下可以客户端主动发起关闭，调用response.close()或者httpclient.close();这种是客户端发起关闭，发起后该链接会处于TIME_WAIT状态(服务器发起则是服务器TIME_WAIT)，等数据传输完毕后则会关闭连接
 * 完全依赖服务器的话则看服务器keepalive时间。
 * 客户端直接请求完后立刻关闭的方式可以利用http协议request中添加头信息Connection: close，服务器收到这个header则response完会立刻发起关闭
 *
 * 示例1：
 * 			CloseableHttpClient client = HttpUtils.createPoolClient();//获取默认池化客户端
 String host = "http://ir.baidu.com";
 String path = "/phoenix.zhtml?c=188488&p=irol-searchengine";
 Map<String, String> headers = new HashMap<String, String>();
 Map<String, String> querys = new HashMap<String, String>();
 try {
 for(int i=0;i<10;i++){
 String resStr = HttpUtils.doGetStringRsp(client, host, path, headers, querys);
 if(resStr!=null){
 //do xxx
 }
 //如果觉得请求快了可以在这儿sleep一会
 }
 } catch (Exception e) {
 e.printStackTrace();
 } finally {
 try {
 client.close();//里面会调用cm.close(),关闭连接池,一旦调用该client实例会不可用，下次必须使用新的client
 } catch (IOException e) {
 e.printStackTrace();
 }
 }
 }


 * 示例2：
 * 			CloseableHttpClient client = HttpUtils.createPoolClient();//获取默认池化客户端
 String host = "http://ir.baidu.com";
 String path = "/phoenix.zhtml?c=188488&p=irol-searchengine";
 Map<String, String> headers = new HashMap<String, String>();
 Map<String, String> querys = new HashMap<String, String>();

 for(int i=0;i<20;i++){
 try {
 CloseableHttpResponse response = HttpUtils.doGet(client,host, path, headers, querys);
 System.out.println("num="+i+",StatusCode="+response.getStatusLine().getStatusCode());
 HttpEntity  entity = response.getEntity();
 if(entity != null) {
 String respStr = EntityUtils.toString(entity, DEFAULT_CHARSET);//获取返回结果字符串,这个方法里面有默认的io.close()方法会释放资源进行链接重用
 System.out.println("respStr="+respStr);
 }
 // 释放资源
 EntityUtils.consume(entity);//采用这个方式释放资源，不会关闭连接，PoolingHttpClientConnectionManager的pool中的链接就可以复用
 //					response.close();//这个方法会直接关闭连接，则无法进行pool连接池复用,当然使用是没问题，但是没了效率，
 //所以才多线程并发的时候,不要在这儿来关闭连接,可以在全部做完后在外部调用httpClient.close()来进行关闭
 } catch (Exception e) {
 e.printStackTrace();
 } finally {
 //				httpclient.close();
 }
 }

 //最后关闭连接
 try {
 client.close();//里面会调用cm.close(),关闭连接池,一旦调用该client实例会不可用，下次必须使用新的client
 } catch (IOException e) {
 e.printStackTrace();
 }

 //几个资源释放方法的区别
 //			EntityUtils.consume(entity);//采用这个方式释放资源，不会关闭连接，PoolingHttpClientConnectionManager的pool中的链接就可以复用
 //			response.close();//这个方法会直接当前连接，则无法进行pool连接池复用,当然使用是没问题，但是没了效率
 //			httpclient.close();//关闭client的所有链接
 *
 *
 * */

}
