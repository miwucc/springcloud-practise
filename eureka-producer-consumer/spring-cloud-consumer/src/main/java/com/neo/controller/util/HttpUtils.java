package com.neo.controller.util;

import org.apache.commons.lang.StringUtils;
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
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class HttpUtils {

    private final static Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static int MAX_TOTAL = 20;//总体可以存在的链接数量，默认值为20，如果设置小于默认值会自动设为默认值
    private static int DEFAULT_MAXPERROUTE = 5;//每一个h请求的ost(域名，如果请求的是host链接了path那么也会自动解析出真正的域名来算连接量)最多可以有多少个链接，默认值为2，如果设置小于默认值会自动设为默认值,也可以单独在链接管理器cm中设置指定host的最大链接并发数cm.setMaxPerRoute(new HttpRoute(localhost), 50);
    //基本可以认为DEFAULT_MAXPERROUTE这个值是一个host能同时支持的最大并发线程数
    private static String DEFAULT_CHARSET="UTF-8";

    private static int POOL_GET_TIMEOUT = 2*1000;//从连接池取链接超时时间，从连接池中获取连接的超时时间，超过该时间未拿到可用连接，默认每个host的最大链接数是2,会抛出org.apache.http.conn.ConnectionPoolTimeoutException: Timeout waiting for connection from pool
    private static int CONNECT_TIMEOUT = 2*1000;//connectTimeout:连接上服务器(握手成功)的时间，超出该时间抛出connect timeout
    private static int SOCKET_TIMEOUT = 2*1000;//服务器数据超时时间，超过这个时间没收到返回则抛出异常

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
        requestConfig =  RequestConfig.custom()
                .setConnectionRequestTimeout(POOL_GET_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT).build();
        //设置重试策略
        httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception,int executionCount, HttpContext context) {
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


    /**返回一个新的非池化链接httpClient*/
    public static CloseableHttpClient createNoPoolClient(){
        return HttpClients.createDefault();
    }

    /**创建指定参数的池化httpClient*/
    public static CloseableHttpClient createPoolClient(int maxConnectTotalNum,int maxConnectPerHost,int connectTimeout,int socketTimeOut){

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxConnectTotalNum);
        cm.setDefaultMaxPerRoute(maxConnectPerHost);

        RequestConfig  rc = RequestConfig.custom()
                .setConnectionRequestTimeout(POOL_GET_TIMEOUT)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeOut).build();

        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(rc)
                .setRetryHandler(httpRequestRetryHandler)
                .build();
    }

    /**创建默认参数的httpclient*/
    public static CloseableHttpClient createPoolClient(){
        return HttpClients.custom()
                .setConnectionManager(clientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(httpRequestRetryHandler)
                .build();
    }

    /**获取自己指定参数的池化client，可以在cm中单独指定某host最大并发连接数  ,示例:
     HttpHost localhost = new HttpHost("http://baidu.com",80);
     cm.setMaxPerRoute(new HttpRoute(localhost), 50); */
    public static CloseableHttpClient createPoolClient(PoolingHttpClientConnectionManager cm,RequestConfig requestConfig){
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
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse doGet(CloseableHttpClient httpClient,String host, String path,
                                              Map<String, String> headers,
                                              Map<String, String> querys)
            throws Exception {
        HttpGet request  = new HttpGet(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }
        return httpClient.execute(request);
    }

    /**默认返回string的Get，只需要关心返回值，异常情况返回null*/
    public static String doGetStringRsp(
            CloseableHttpClient httpClient,
            String host,String path,String method,
            Map<String, String> headers,
            Map<String, String> querys){
        String respStr=null;
        try {
            HttpGet request  = new HttpGet(buildUrl(host, path, querys));
            for (Map.Entry<String, String> e : headers.entrySet()) {
                request.addHeader(e.getKey(), e.getValue());
            }
            CloseableHttpResponse response = httpClient.execute(request);
            int httpStatus = response.getStatusLine().getStatusCode();
            if(httpStatus/100==2){//20x状态才说明访问成功
                HttpEntity  entity = response.getEntity();
                if(entity != null) {
                    respStr = EntityUtils.toString(entity, DEFAULT_CHARSET);
                }
                EntityUtils.consume(entity);
            }else{
                log.error("doStringRspGet请求"+host+"发生返回状态!=20X,返回=", httpStatus);
            }
            return respStr;
        } catch (Exception e1) {
            log.error("doStringRspGet请求"+host+"发生异常!", e1);
            return respStr;
        }
    }

    /**默认返回string的Post，只需要关心返回值，异常情况返回null*/
    public static String doPostStringRsp(
            CloseableHttpClient httpClient,
            String host,String path,
            Map<String, String> headers,
            Map<String, String> querys,
            String body){

        String respStr=null;
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
            if(httpStatus/100==2){//20x状态才说明访问成功
                HttpEntity  entity = response.getEntity();
                if(entity != null) {
                    respStr = EntityUtils.toString(entity, DEFAULT_CHARSET);
                }
                EntityUtils.consume(entity);
            }else{
                log.error("doStringRspPost请求"+host+"发生返回状态!=20X,返回=", httpStatus);
            }
            return respStr;
        } catch (Exception e1) {
            log.error("doStringRspPost请求"+host+"发生异常!", e1);
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
    public static HttpResponse doPost(CloseableHttpClient httpClient,String host, String path,
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
            formEntity.setContentType("application/x-www-form-urlencoded; charset="+DEFAULT_CHARSET);
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
    public static HttpResponse doPost(CloseableHttpClient httpClient,String host, String path,
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
    public static HttpResponse doPost(CloseableHttpClient httpClient,String host, String path,
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
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(CloseableHttpClient httpClient,String host, String path,
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
     * @param host
     * @param path
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(CloseableHttpClient httpClient,String host, String path,
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
    public static HttpResponse doDelete(CloseableHttpClient httpClient,String host, String path,
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
    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url
     *            发送请求的URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            log.info("响应头：{}",map);
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            log.error("发送 GET 请求出现异常！",e);
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            log.error("发送 POST 请求出现异常！",e);
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }
}
