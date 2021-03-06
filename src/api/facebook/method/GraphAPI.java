package api.facebook.method;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.net.ssl.SSLException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import api.facebook.http.ClientFactory;
import api.facebook.http.NormalClient;
import api.facebook.http.ProxyClient;
import api.facebook.util.JsonAnalyze;

/**
 * 封装graphAPI中获取公共主页的贴文、评论和涂鸦墙信息的方法和数据解析
 * @author chenkedi
 *
 */
public abstract class GraphAPI
{
	public final static String BASE_URL="graph.facebook.com";
	
	@Value("#{paramsUtil['clientId']}")
	protected String clientId;
	@Value("#{paramsUtil['clientSecret']}")
	protected String clientSecret;
	@Value("#{paramsUtil['params.useProxy']}")
	protected String useProxy;
	
	@Resource
	private ProxyClient proxyClient;
	@Resource
	private NormalClient normalClient;
	
	protected JsonAnalyze json=new JsonAnalyze();
	//通过这里来确定制造何种类型的client(normal or proxy),本类中在构造方法中根据配置文件初始化对应的client
//	private ClientFactory clientFactory=new ProxyClient();
	protected ClientFactory clientFactory;
	protected static final Logger log =  Logger.getLogger(GraphAPI.class);
	
	public GraphAPI() {
		//从配置文件读取accessToken
		//loadAccesToken();
		//根据配置文件初始化clientFactory
//		obtainClientFactory();
	}

	/**
	 * 根据所给的node和要获取的fields请求API,获得返回的HTML实体的json对象
	 * @param node 公共主页名 或者 一条post的id，或者 一条feed的id
	 * @param fields posts、comments、feeds
	 * @return JSONObject
	 */
	public JSONObject callAPI(String node,String fields){
		obtainClientFactory();
		CloseableHttpClient httpClient=clientFactory.createClient();
		JSONObject jsonObject=null;
		
		//动态创建API链接地址
		URI uri=null;
		try {
			uri = new URIBuilder()
			.setScheme("https")
			.setHost(BASE_URL)
			.setPath("/"+node)
			.setParameter("fields", fields)
			.setParameter("access_token", clientId+"|"+clientSecret)
			.build();
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//请求URL并获取response
		CloseableHttpResponse response= getResponse(httpClient,uri);
		//获得请求的实体
		HttpEntity entity = response.getEntity();
		//打印请求状态
		log.info("获取 "+node+"的 "+fields+" 属性的状态："+response.getStatusLine().toString());
		//将实体转为字符串
		String entityString=null;
		
		entityString = getEntityString(entity);
	
		log.info("================获得的"+fields+"的json对象================");

		log.info(entityString.length()>200?entityString.substring(0, 200):entityString);		
		
		try{
			jsonObject=new JSONObject(entityString);
	
		}catch(JSONException e){
			log.error("请求返回的不是一个json字符串！可能是代理软件或者VPN虽然开启，但是与境外服务器连接错误，导致返回错误的HTML实体！");
			log.error("睡眠30分钟后再重试请求API，错误信息："+e.getCause()+e.getMessage());
			log.error("堆栈信息："+e.getStackTrace().toString());
			try {
				Thread.sleep(1800*1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			//先关闭callAPI已经创建的httpClient对象，释放资源
			try {  
	            httpClient.close();
	            response.close();
	        } catch (IOException e1) {  
	            e1.printStackTrace();  
	        }
			//再重新执行本方法
			/*************这里不知道为何会在网络由异常恢复正常后直接返回一个null的JosnObject*************/
			//callAPI(node,fields)的写法属于递归调用，如果不向下面这样进行赋值的话，最终代理正常后返回的json不会通过方法的栈一层一层的返回的最初出错的那一层方法
			jsonObject=callAPI(node,fields);
		}
		
		try {  
            httpClient.close();
            response.close();
        } catch (IOException e) {  
            e.printStackTrace();  
        }
		return jsonObject;
	}
	
	
	/**
	 * 根据首次遍历所获得的翻页链接,获得返回的HTML实体的json对象
	 * @param node 公共主页名 或者 一条post的id，或者 一条feed的id
	 * @param fields posts、comments、feeds
	 * @return JSONObject
	 */
	public JSONObject callAPI(String node,String fields,String url){
		obtainClientFactory();
		CloseableHttpClient httpClient=clientFactory.createClient();
		JSONObject jsonObject=null;
		
		//编码链接
		URI uri=null;
		url=url.replaceAll("\\|", "%7c");
		try {
			uri = new URIBuilder(url).build();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		//请求URL并获取response
		CloseableHttpResponse response= getResponse(httpClient,uri);
		//获得请求的实体
		HttpEntity entity = response.getEntity();
		//打印请求状态
		log.info("获取 "+node+"的 "+fields+" 属性的状态："+response.getStatusLine().toString());
		//将实体转为字符串
		String entityString=null;
	
		entityString = getEntityString(entity);
	
		log.info("================获得的"+fields+"json对象===============");
		
		log.info(entityString.length()>200?entityString.substring(0, 200):entityString);
				
		try{
			jsonObject=new JSONObject(entityString);
		}catch(Exception e){
			log.error("请求返回的不是一个json字符串！可能是代理软件或者VPN虽然开启，但是与境外服务器连接错误，导致返回错误的HTML实体！");
			log.error("可能是代理软件被关闭或者vpn已断开，睡眠2分钟再尝试连接: "+e.getCause()+"，"+e.getMessage());
			log.error("堆栈信息："+e.getStackTrace().toString());
			try {
				Thread.sleep(300*1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			//先关闭callAPI已经创建的httpClient对象，释放资源
			try {  
	            httpClient.close();  
	            response.close();
	        } catch (IOException e1) {  
	            e1.printStackTrace();  
	        }
			//重新进行一次访问
			//callAPI(node,fields)的写法属于递归调用，如果不向下面这样进行赋值的话，最终代理正常后返回的json不会通过方法的栈一层一层的返回的最初出错的那一层方法
			jsonObject=callAPI(node,fields,url);
		}		
		//返回之前先释放资源
        try {  
            httpClient.close(); 
            response.close();
        } catch (IOException e) {  
            e.printStackTrace();  
        }
		return jsonObject;
	}
	
	/**
	 * 根据APP ID 和 APP Secret 获取APP类型的AccessToken
	 * 后期优化需要将APP ID 和 APP Secret队列写入配置文件
	 * @return
	 */
	public Map<String,String> obtainAccessToken(){
		
		CloseableHttpClient httpClient=clientFactory.createClient();//这里创建的是ProxyClient
		
		//动态创建API链接地址
		URI uri=null;
		try {
			uri = new URIBuilder()
			.setScheme("https")
			.setHost(BASE_URL)
			.setPath("/oauth/access_token")
			.setParameter("client_id", clientId)
			.setParameter("client_secret", clientSecret)
			.setParameter("grant_type", "client_credentials")
			.build();
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//请求URL并获取response
		CloseableHttpResponse response= getResponse(httpClient,uri);
		//获得请求的实体
		HttpEntity entity = response.getEntity();
		//打印请求状态
		log.info("获取access token状态："+response.getStatusLine().toString());
		//将实体转为字符串
		//String entityString=getEntityString(entity);
		String entityString=null;
		Map<String,String> map=new HashMap<String,String>();
		String[] entityArray=entityString.split("=");
		map.put(entityArray[0], entityArray[1]);
		return map;
	}
	
	
	/**
	 * 封装公用的请求URL并返回response对象，并进行异常处理的过程
	 * @param httpClient
	 * @param uri
	 * @return
	 */
	public CloseableHttpResponse getResponse(CloseableHttpClient httpClient, URI uri){
		
		HttpGet httpGet=new HttpGet(uri);
		
		//如果uri前面是www.facebook.com,则设置头部信息来伪装浏览器，以方便网页爬取用户的头像
		if(!uri.getHost().equals("www.facebook.com") && !uri.getHost().equals("graph.facebook.com")){   
			httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.15 Safari/537.36");  
			httpGet.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");  
			httpGet.setHeader("Accept-Language","zh-CN,zh;q=0.8,en;q=0.6,it;q=0.4");    
			httpGet.setHeader("Accept-Encoding","gzip, deflate, sdch"); 
			httpGet.setHeader("Upgrade-Insecure-Requests","1");
		}
		//开始请求API
		log.info("==================请求的URL=====================");
		log.info(uri.toString());
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
		} catch (ClientProtocolException e) {
			log.error("所提交的请求内容不符合http协议要求："+e.getMessage());
			log.error(e.getStackTrace().toString());
		} catch (IOException e) {
			log.error("未知的网络连接错误错误！");
			log.error("可能是代理软件被关闭或者vpn已断开，睡眠2分钟再尝试连接: "+e.getCause()+"，"+e.getMessage());
			log.error("堆栈信息："+e.getStackTrace().toString());
			try {
				Thread.sleep(120*1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			//两分钟后在catch块中重新执行getResponse，相当于重新请求一次
			//一定要写上response= ，否则重新执行该方法即使成功也会产生nullpoint异常，因为得到的结果无法通过return返回，原理同上面callAPI一样
			response=getResponse(httpClient,uri);
		}

		return response;
	}
	
	/**
	 * 封装公用的http实体转字符串，并进行异常处理的过程
	 * @param entity
	 * @return
	 */
	public String getEntityString(HttpEntity entity){
		
		String entityString=null;
		try {
			entityString= EntityUtils.toString(entity, "utf-8");
		} catch (ParseException e) {
			e.printStackTrace();
		} catch(Exception e){
			log.error("可能是代理在SSL传输数据的过程中突然中断，或者对方服务器主动中断SSL链接，睡眠2分钟后重试！");
			log.error("可能是代理软件被关闭或者vpn已断开，睡眠2分钟再尝试连接: "+e.getCause()+"，"+e.getMessage());
			log.error("堆栈信息："+e.getStackTrace().toString());
			try {
				Thread.sleep(120*1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			entityString=getEntityString(entity);
		}
			
		return entityString;
	}
	
	/**
	 * 获取当前时间的时间戳，用于获取insert_time
	 */
	public Timestamp getNowTime(){
		
		return new Timestamp(System.currentTimeMillis());
	}
	
	/**
	 * 字符串转时间戳
	 * @param time
	 * @param format
	 * @return
	 */
	public Timestamp string2Timestamp(String time,String format){
		
		Timestamp ts=null;
		if(format!=null){
			SimpleDateFormat sf = new SimpleDateFormat(format);
			
			Date date=null;
			try {
				date = sf.parse(time);
			} catch (java.text.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			ts = new Timestamp(date.getTime());
		}else{
			
			String[] times=time.split("T");
			String[] hours=times[1].split("\\+");
			time=times[0]+" "+hours[0];
			ts=Timestamp.valueOf(time);
		}
		
		return ts;
		
	}
	
	/**
	 * 从properties中读取accessToken
	 */
	public void loadAccesToken(){
		
		Properties prop = new Properties();
		
		InputStream is = GraphAPI.class.getResourceAsStream("/accessToken.properties");
		try{
			if(is!=null){
				prop.load(is);
			}
			
			if(prop.getProperty("clientId")!=null && prop.getProperty("clientSecret")!=null){
				clientId=prop.getProperty("clientId");
				clientSecret=prop.getProperty("clientSecret");
			}else{
				log.error("accessToken.properties 文件中的字段为空！读取失败");
			}
			
		}catch(IOException e){
			e.printStackTrace();
			log.error("accessToken读取出现错误！"+e.getMessage());
		}finally{
			if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                	
                }
            }
		}
	}
	
	/**
	 * 根据配置文件决定使用哪个clientFactory的实例（VPN或者代理）
	 */
	public void obtainClientFactory() {
		
		if(useProxy!=null){
			if(useProxy.equals("true")){
				clientFactory=proxyClient;
			}else{
				clientFactory=normalClient;
			}
		}else{
			log.error("params.properties 文件中的useProxy字段为空！读取失败");
			System.out.println(useProxy);
		}
		
	}
}
