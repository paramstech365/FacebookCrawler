package api.facebook.main;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;

import api.facebook.bean.Posts;
import api.facebook.bean.Seeds;
import api.facebook.dao.PostsDao;
import api.facebook.dao.SeedsDao;
import api.facebook.method.GetPosts;
import api.facebook.util.AppContext;
import api.facebook.util.Params;

/**
 * 爬取公众人物发表的帖文
 * @author chenkedi
 *
 */
@Controller
public class PostsInfoCrawler
{
	@Resource
	private PostsDao postsDao;
	@Resource
	private SeedsDao seedsDao;
	@Resource
	private Params params;
	@Resource
	private GetPosts getPosts;
	
	//如果需要在虚类中使用spring的注解注入类成员变量，则只要其子类上使用@Component等Steretype类型的注解
	//更重要的是，在需要引用虚类子类的实例时，需要交给spring的bean上下文初始化，即在主类中使用spring的xml或者注解形式进行注入，不能像下发一样自己实例化对象
//	private GetPosts getPosts=new GetPosts();
	private int cycle=1;//记录爬虫启动以来轮询的次数
	private static final Logger log = Logger.getLogger(PostsInfoCrawler.class);
	
	
	public static void main(String[] args){
		log.info("正在创建数据库连接和缓冲池...");
	    AppContext.initAppCtx();
	    log.info("数据库连接已连接！缓冲池已建立");
	    
	    PostsInfoCrawler crawler= (PostsInfoCrawler) AppContext.appCtx.getBean(PostsInfoCrawler.class);
	    crawler.run();
	}
	
	public void run(){
		while(true){
			
				//当还有至少一个种子的历史贴文没有爬取完毕时，next方向的爬行继续

				//查询CrawedPosts为0的seed，表示还未经过第一次遍历
				List<Seeds> seeds=seedsDao.readSeedsForPosts(params.getPostsInfoLength(),0,null);//获得需要爬取的种子队列
				
				if(seeds.size()!=0){
					for(Seeds temp : seeds){

		    			//调用api得到posts的json数据
		    			JSONObject jsonObject=getPosts.callAPI((temp.getFacebookId()!=null)?temp.getFacebookId():temp.getUserName(),"posts");
		    			//数据抽取，将json转换为bean的格式
		    			//上一句返回的json对象有可能不含有post数据，所以用三元运算符判断，然后交给dataExtract处理
		    			List<Posts> postsList=getPosts.dataExtract(jsonObject.has("posts")?jsonObject.getJSONObject("posts"):jsonObject,temp.getSeedsId());//如果出现请求错误，seed可能为空，需要做处理
		    			if(!postsList.get(0).getStatus().equals("error")){
		    				postsDao.batchInsert(postsList);
		    				log.info(temp.getName()+"的贴文批量插入成功!");
		    				//将翻页的链接写入seeds,此处第一次遍历，previous和next都要跟新
		    				seedsDao.updatePreviousPage(postsList.get(0),temp.getSeedsId());
		    				seedsDao.updateNextPage(postsList.get(0),temp.getSeedsId());
		    				log.info(temp.getName()+"的翻页链接更新成功!");
		    				//将已经爬取过的种子标记值更新为1
		    				String sql="UPDATE seeds set crawed_posts=? WHERE seeds_id=?";
		    				seedsDao.updateCrawed(sql,temp.getSeedsId(),1);
		    				log.info("种子的Crawed_posts爬取状态更新成功!\n\n");
		    			}
		    			else{
		    				log.error(temp.getName()+"的贴文获取失败，继续采集下一个种子！");
		    				log.error("错误代码："+postsList.get(0).getCodeMessage());
		    			}
					}
				}else{
					//当种子都经过第一次爬取后，就要切换到翻页链接的爬取，使用深度优先。
					//使用facebook提供的分页链接爬取，返回的json对象外面没有包裹说明变量（如posts等）
					//使用这个翻页链接可以避免数据重复
					
					//查询CrawedPosts为1的值，表示还未经过第二轮，即爬取历史数据
					seeds=seedsDao.readSeedsForPosts(params.getPostsInfoLength(),1,"posts_next_page");//获得需要爬取的Crawed_posts为1的种子队列
					if(seeds.size()!=0){
						for(Seeds temp : seeds){						

			    			//调用api得到posts的json数据
			    			//这里要防止翻页的PageLink为空！！！！！,即在取种子时就做好筛选，不选择那些没有贴文翻页链接的人
			    			JSONObject jsonObject=getPosts.callAPI((temp.getUserName()!=null)?temp.getUserName():temp.getFacebookId(),"posts",temp.getPostsNextPage());
			    			//数据抽取，将json转换为bean的格式
			    			List<Posts> postsList=getPosts.dataExtract(jsonObject,temp.getSeedsId());//如果出现请求错误，seed可能为空，需要做处理
			    			
			    			//开始深度优先爬取历史贴文
			    			if(!postsList.get(0).getStatus().equals("error")){
			    				if(!postsList.get(0).getStatus().equals("empty")){
			    					postsDao.batchInsert(postsList);
				    				log.info(temp.getName()+"的贴文批量插入成功!");
				    				//将最新的翻页的链接写入seeds，注意，此处只应该更新nextpage链接
				    				seedsDao.updateNextPage(postsList.get(0),temp.getSeedsId());
				    				log.info(temp.getName()+"的Next翻页链接更新成功!\n\n");
				    				
			    				}else{
			    					log.info(temp.getName()+"的历史贴文为空，已采集完毕，将Crawed_post状态更新为-1（历史贴文枯竭）继续采集下一个种子！\n\n");
			    					//将已经爬取过的种子标记值更新为-1,表示历史数据枯竭
				    				String sql="UPDATE seeds set crawed_posts=? WHERE seeds_id=?";
				    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-1);
				    				log.info("种子的Crawed_posts爬取状态更新成功!\n\n");
			    				}		
			    			}else{
			    				//此处可能会出现”(#12) location field is deprecated for versions v2.3 and higher“错误
			    				//此处可能会出现”(#1) An unknown error has ocuured“错误
			    				//如果出现此错误，说明这个种子不可能遍历到历史数据为空。由于这是facebook给出的翻页链接本身有问题，所以跳过并标记此种子，继续下一个
			    				if(postsList.get(0).getCodeMessage().equals("12") || postsList.get(0).getCodeMessage().equals("1") || postsList.get(0).getCodeMessage().equals("100")){
			    					log.info(temp.getName()+"的历史post出现Facebook自身链接的错误(code："+postsList.get(0).getCodeMessage()+"），绝大部分数据已采集完毕，将Crawed_post状态更新为-1，继续采集下一个种子！");
				    				String sql="UPDATE seeds set crawed_posts=? WHERE seeds_id=?";
				    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-1);
				    				log.info("种子的Crawed_posts爬取状态更新为-1成功!\n\n");
			    				}else{
			    					log.error(temp.getName()+"的贴文获取失败，继续采集下一个种子！");
			    					log.error("错误代码："+postsList.get(0).getCodeMessage());
			    				}
			    			}
						}
						
					}else{//为1的种子全部爬完，历史数据采集完毕，crawed_posts现在应该被全部置为-1可以开始向未来数据在线爬取了
						
						seeds=seedsDao.readSeedsForPosts(params.getPostsInfoLength(),-1,"posts_next_page");//获得需要爬取的Crawed_posts为1的种子队列
						if(seeds.size()!=0){
							for(Seeds temp : seeds){						

				    			//调用api得到posts的json数据
				    			JSONObject jsonObject=getPosts.callAPI((temp.getUserName()!=null)?temp.getUserName():temp.getFacebookId(),"posts",temp.getPostsPreviousPage());
				    			//数据抽取，将json转换为bean的格式
				    			List<Posts> postsList=getPosts.dataExtract(jsonObject,temp.getSeedsId());//如果出现请求错误，seed可能为空，需要做处理
				    			
				    			//开始在线爬取首次遍历以后的贴文
				    			if(!postsList.get(0).getStatus().equals("error")){
				    				if(!postsList.get(0).getStatus().equals("empty")){
				    					postsDao.batchInsert(postsList);
					    				log.info(temp.getName()+"的贴文批量插入成功!");
					    				//将最新的翻页的链接写入seeds，注意，此处只应该更新nextpage链接
					    				seedsDao.updatePreviousPage(postsList.get(0),temp.getSeedsId());
					    				log.info(temp.getName()+"的previous翻页链接更新成功!");
					    				String sql="UPDATE seeds set crawed_posts=? WHERE seeds_id=?";
					    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-2);
					    				log.info("种子的Crawed_posts爬取状态更新为-2（已经经过至少一轮未来数据采集）成功!\n\n");
				    				}else{
				    					log.info(temp.getName()+"暂时没有发新贴，将crawed_posts置为-2，继续采集下一个种子！");
				    					//将已经爬取过的种子标记值更新为-2,即使它没有采到数据，防止其很久没有发帖，影响其他活跃种子的采集，表示经过了一次未来数据的遍历
					    				String sql="UPDATE seeds set crawed_posts=? WHERE seeds_id=?";
					    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-2);
					    				log.info("种子的Crawed_posts爬取状态更新为-2（已经经过至少一轮未来数据采集）成功!\n\n");
				    				}		
				    			}else{
				    				if(postsList.get(0).getCodeMessage().equals("100")){
				    					log.info(temp.getName()+"的未来post出现Facebook自身链接的错误(code："+postsList.get(0).getCodeMessage()+"），绝大部分数据已采集完毕，将Crawed_post状态更新为-1，继续采集下一个种子！");
					    				String sql="UPDATE seeds set crawed_posts=? WHERE seeds_id=?";
					    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-2);
					    				log.info("种子的Crawed_posts爬取状态更新为-2成功!\n\n");
				    				}else{
				    					log.error(temp.getName()+"的贴文获取失败，继续采集下一个种子！");
				    					log.error("错误代码："+postsList.get(0).getCodeMessage());
				    				}
				    				
				    			}
						}
					}else{//状态为-1的种子已经全部遍历一遍，现在为状态为-2，可以开始重置种子为-1，继续进行未来爬行
						String sql="UPDATE seeds set crawed_posts=?";
						seedsDao.resetCrawed(sql,-1);
	    				log.info("自爬虫启动以来，第” "+cycle+" “次的轮询已完成，Crawed_posts重置为-1成功!\n\n");
	    				log.info("在开始下一轮在线轮询时，考虑到用户发帖的频繁度，睡眠30分钟，也可以防止请求过于频繁");
	    				try {
							Thread.sleep(1800*1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	    				cycle++;
	    				
					}
					
				}
			}		
		}	
	}
}
