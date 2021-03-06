package api.facebook.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import api.facebook.bean.Posts;

/**
 * 此类对应于API中获取公共主页贴文的方法
 * @author chenkedi
 *
 */
@Component
public class GetPosts extends GraphAPI
{
	
	/**
	 * 通过APIcall函数返回的”顶层“json对象，进行数据的解析与容错处理
	 * @param jsonObject
	 * @param seedsId
	 * @return
	 */
	public List<Posts> dataExtract(JSONObject jsonObject ,int seedsId) {
		
		List<Posts> postList=new ArrayList<Posts>();	
		//如果请求没有成功，则返回错误信息和错误code
		if(json.isErrorJson(jsonObject)){
			Map<String,String> map=null;
			map=json.jsonErrorMessage(jsonObject);
			log.error("错误代码："+map.get("code")+"，错误信息："+map.get("message"));
			Posts post=new Posts();
			post.setStatus("error");
			post.setCodeMessage(map.get("code"));
			postList.add(post);
		}else{
			
			//检测当前的顶层json对象是否还有paging这个键，没有的话说明这个json是空的
			if(jsonObject.has("paging")){

				//获取翻页api链接
				JSONObject pageLinkJson=jsonObject.getJSONObject("paging");
				JSONArray jsonArray=jsonObject.getJSONArray("data");
				for(int i=0;i<jsonArray.length();i++){
					Posts post= new Posts();
					JSONObject postJsonObj=jsonArray.getJSONObject(i);
					
					//先处理post本表内部对应json对象中的一级键值
					if(postJsonObj.has("id")){
						post.setMessageId(postJsonObj.getString("id"));
					}else{
						post.setMessageId(null);
					}
					
					if(postJsonObj.has("message")){
						post.setMessage(postJsonObj.getString("message"));
					}else{
						post.setMessage(null);
					}
					
					if(postJsonObj.has("created_time")){
						post.setCreatedTime(string2Timestamp( postJsonObj.getString("created_time"), null));
					}else{
						post.setCreatedTime(null);
					}
										
					post.setSeedsId(seedsId);	
					
					if(postJsonObj.has("link")){
						post.setLink(postJsonObj.getString("link"));
					}else{
						post.setLink(null);
					}
					
					if(postJsonObj.has("picture")){
						post.setPicture(postJsonObj.getString("picture"));
					}else{
						post.setPicture(null);
					}
					
					if(postJsonObj.has("shares")){
						post.setShares(postJsonObj.getJSONObject("shares").getInt("count"));
					}else{
						post.setShares(null);
					}
					
					/*===============为了防止每个post对象都存储PageLink，仅在循环第一次时写入PageLink进入post对象，取出时只需要取出index索引为0即可=====================*/
					if(i==0){
						if(pageLinkJson.has("previous")){
							post.setPostsPreviousPage(pageLinkJson.getString("previous"));
						}else{
							post.setPostsPreviousPage(null);
						}
						
						if(pageLinkJson.has("next")){
							post.setPostsNextPage(pageLinkJson.getString("next"));
						}else{
							post.setPostsNextPage(null);
						}
					}
					
					
					postList.add(post);

					if(jsonArray.length()-1==i){
						log.info("获得\""+postJsonObj.getJSONObject("from").getString("name")+"\"的贴文信息成功！准备写入数据库！");
					}				
				}
			
			}else{//posts 顶层不含有paging这个key，说明posts为空，爬取走到尽头
				
				Posts post =new Posts();
				post.setStatus("empty");
				postList.add(post);
			}
			
		}
		return postList;

	}

}

//处理评论部分
//if(postJsonObj.has("comments")){
//	JSONArray commentArray=postJsonObj.getJSONObject("comments").getJSONArray("data");
//	List<Comments> comments=new ArrayList<Comments>();
//	for(int j=0;i<commentArray.length();j++){
//		JSONObject commentObj=commentArray.getJSONObject(j);
//		Comments comment=new Comments();
//		comment.setMessageId(commentObj.getString("id"));
//		comment.setMessage(commentObj.getString("message"));
//		comment.setFromUserId(commentObj.getJSONObject("from").getString("id"));
//		comment.setFromUserName(commentObj.getJSONObject("from").getString("name"));
//		comment.setPostId(postJsonObj.getInt(arg0));
//		
//	}
//}
