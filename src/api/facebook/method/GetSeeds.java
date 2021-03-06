package api.facebook.method;

import java.util.Map;

import org.json.JSONObject;

import api.facebook.bean.Seeds;

/**
 * 此类对应于API中获取公众人物个人信息的方法
 * @author chenkedi
 *
 */
public class GetSeeds extends GraphAPI
{

	public Seeds dataExtract(JSONObject jsonObject) {
		
		
		//如果请求没有成功，则返回错误信息和错误code
		if(json.isErrorJson(jsonObject)){
			Map<String,String> map=null;
			map=json.jsonErrorMessage(jsonObject);
			log.error("错误代码："+map.get("code")+"，错误信息："+map.get("message"));
			return null;
		}else{
			Seeds seed=new Seeds();
			
			seed.setFacebookId(jsonObject.getString("id"));
			
			if(jsonObject.has("username")){
				seed.setUserName(jsonObject.getString("username"));
			}else{
				seed.setUserName(null);
			}
			
			if(jsonObject.has("about")){
				seed.setAbout(jsonObject.getString("about"));
			}else{
				seed.setAbout(null);
			}
			
			if(jsonObject.has("bio")){
				seed.setBio(jsonObject.getString("bio"));
			}else{
				seed.setBio(null);
			}
			
			if(jsonObject.has("birthday")){
				seed.setBirthday(string2Timestamp(jsonObject.getString("birthday"), "MM/dd/yyyy"));
			}else{
				seed.setBirthday(null);
			}
			
			if(jsonObject.has("category")){
				seed.setCategory(jsonObject.getString("category"));
			}else{
				seed.setCategory(null);
			}
			
			if(jsonObject.has("hometown")){
				seed.setHometown(jsonObject.getString("hometown"));
			}else{
				seed.setHometown(null);
			}
			
			if(jsonObject.has("likes")){
				seed.setLikes(jsonObject.getInt("likes"));
			}else{
				seed.setLikes(null);
			}
			
			if(jsonObject.has("link")){
				seed.setLink(jsonObject.getString("link"));
			}else{
				seed.setLink(null);
			}
			
			if(jsonObject.has("name")){
				seed.setPageName(jsonObject.getString("name"));//主页上显示的名字，对应数据库中的page_name
			}else{
				seed.setPageName(null);
			}
			
			if(jsonObject.has("website")){
				seed.setWebsite(jsonObject.getString("website"));//主页上显示的名字，对应数据库中的page_name
			}else{
				seed.setWebsite(null);
			}
			
			
			log.info("获得\""+seed.getPageName()+"\"的个人信息成功！准备写入数据库！");
			return seed;
		}
	}

}
