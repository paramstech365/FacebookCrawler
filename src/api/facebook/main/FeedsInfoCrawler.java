package api.facebook.main;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;

import api.facebook.bean.Feeds;
import api.facebook.bean.Seeds;
import api.facebook.dao.FeedsDao;
import api.facebook.dao.SeedsDao;
import api.facebook.method.GetFeeds;
import api.facebook.util.AppContext;
import api.facebook.util.Params;

@Controller
public class FeedsInfoCrawler
{
	@Resource
	private FeedsDao feedsDao;
	@Resource
	private SeedsDao seedsDao;
	@Resource
	private Params params;
	
	//初始化API的getFeeds方法
	private GetFeeds getFeeds=new GetFeeds();
	private int cycle=1;//记录爬虫启动以来轮询的次数
	private static final Logger log = Logger.getLogger(FeedsInfoCrawler.class);
	
	
	public static void main(String[] args){
		log.info("正在创建数据库连接和缓冲池...");
	    AppContext.initAppCtx();
	    log.info("数据库连接已连接！缓冲池已建立");
	    
	    FeedsInfoCrawler crawler= (FeedsInfoCrawler) AppContext.appCtx.getBean(FeedsInfoCrawler.class);
	    crawler.run();
	}
	
	/**
	 * 爬行器主方法
	 */
	public void run(){
		while(true){
		
				//查询Crawed_Feeds为0的seed，表示还未经过第一次遍历，需要获取PageLink，才能进行后续的历史数据和未来数据爬行
				List<Seeds> seeds=seedsDao.readSeedsForFeeds(params.getFeedsInfoLength(),0);//获得需要爬取的种子队列
				
				if(seeds.size()!=0){
					for(Seeds temp : seeds){

		    			//调用api得到Feeds的json数据
		    			JSONObject jsonObject=getFeeds.callAPI((temp.getFacebookId()!=null)?temp.getFacebookId():temp.getUserName(),"feed");
		    			//数据抽取，将json转换为bean的格式
		    			//上一句返回的json对象有可能没有feed键，所以用三元运算符判断，然后交给dataExtract处理(是错误或者空json)
		    			List<Feeds> feedsList=getFeeds.dataExtract(jsonObject.has("feed")?jsonObject.getJSONObject("feed"):jsonObject,temp.getSeedsId());
		    			if(!feedsList.get(0).getStatus().equals("error")){
		    				feedsDao.batchInsert(feedsList);
		    				log.info(temp.getName()+"的feed批量插入成功!");
		    				//将翻页的链接写入seeds,此处第一次遍历，previous和next都要跟新
		    				seedsDao.updatePreviousPage(feedsList.get(0),temp.getSeedsId());
		    				seedsDao.updateNextPage(feedsList.get(0),temp.getSeedsId());
		    				log.info(temp.getName()+"的翻页链接更新成功!");
		    				//将已经爬取过的种子标记值更新为1
		    				String sql="UPDATE seeds set crawed_feeds=? WHERE seeds_id=?";
		    				seedsDao.updateCrawed(sql,temp.getSeedsId(),1);
		    				log.info("种子的Crawed_Feeds爬取状态更新成功!\n\n");
		    			}
		    			else{
		    				log.error(temp.getName()+"的feed获取失败，继续采集下一个种子！");
		    				log.info("为避免系统问题，睡眠10秒钟\n\n");
		    				try {
								Thread.sleep(10*1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
		    			}
					}
				}else{
					//当种子都经过第一次爬取后，就要切换到翻页链接的爬取，使用深度优先。
					//使用facebook提供的分页链接爬取，返回的json对象外面没有包裹说明变量（如Feeds等）
					//使用这个翻页链接可以避免数据重复
					
					//查询CrawedFeeds为1的值，表示还未经过第二轮，即爬取历史数据
					seeds=seedsDao.readSeedsForFeeds(params.getFeedsInfoLength(),1);//获得需要爬取的Crawed_Feeds为1的种子队列
					if(seeds.size()!=0){
						for(Seeds temp : seeds){						

			    			//调用api得到Feeds的json数据
			    			JSONObject jsonObject=getFeeds.callAPI(((temp.getUserName()!=null)?temp.getUserName():temp.getFacebookId()),"feed",temp.getFeedsNextPage());
			    			//数据抽取，将json转换为bean的格式
			    			//上一句返回的json对象有可能是含有错误信息的json对象，但由于正确和错误的类型的json对象都是顶层对象，不需要做判断
			    			List<Feeds> feedsList=getFeeds.dataExtract(jsonObject,temp.getSeedsId());
			    			
			    			//开始深度优先爬取历史feed
			    			if(!feedsList.get(0).getStatus().equals("error")){
			    				if(!feedsList.get(0).getStatus().equals("empty")){
			    					feedsDao.batchInsert(feedsList);
				    				log.info(temp.getName()+"的feed批量插入成功!");
				    				//将最新的翻页的链接写入seeds，注意，此处只应该更新nextpage链接
				    				seedsDao.updateNextPage(feedsList.get(0),temp.getSeedsId());
				    				log.info(temp.getName()+"的Next翻页链接更新成功!\n\n");
				    				
			    				}else{
			    					log.info(temp.getName()+"的历史feed为空，已采集完毕，将Crawed_feed状态更新为-1（历史feed枯竭）继续采集下一个种子！");
			    					//将已经爬取过的种子标记值更新为-1,表示历史数据枯竭
				    				String sql="UPDATE seeds set crawed_Feeds=? WHERE seeds_id=?";
				    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-1);
				    				log.info("种子的Crawed_Feeds爬取状态更新为-1成功!\n\n");
			    				}		
			    			}else{
			    				log.error(temp.getName()+"的feed获取失败，准备继续采集下一个种子！");
			    				//此处可能会出现”(#12) location field is deprecated for versions v2.3 and higher“错误
			    				//此处有可能会出现(#1) An unknown error has occurred.
			    				//还有可能(#100) "Unsupported get request.
			    				//如果出现这两个错误，说明这个种子不可能遍历到历史数据为空。由于这是facebook给出的翻页链接本身有问题，所以跳过并标记此种子，继续下一个
			    				if(feedsList.get(0).getCodeMessage().equals("12") || feedsList.get(0).getCodeMessage().equals("1")|| feedsList.get(0).getCodeMessage().equals("100")){
			    					log.info(temp.getName()+"的历史feed出现Facebook自身链接的错误，Code： "+feedsList.get(0).getCodeMessage()+",该种子绝大部分数据已采集完毕，将Crawed_feed状态更新为-1，继续采集下一个种子！");
				    				String sql="UPDATE seeds set crawed_feeds=? WHERE seeds_id=?";
				    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-1);
				    				log.info("种子的Crawed_Feeds爬取状态更新为-1成功!\n\n");
			    				}
			    				log.info("为避免系统问题，睡眠10秒钟\n\n");
			    				try {
									Thread.sleep(10*1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
			    				
			    			}
						}
						
					}else{//为1的种子全部爬完，历史数据采集完毕，crawed_Feeds现在应该被全部置为-1可以开始向未来数据在线爬取了
						

						seeds=seedsDao.readSeedsForFeeds(params.getFeedsInfoLength(),-1);//获得需要爬取的Crawed_Feeds为1的种子队列
						if(seeds.size()!=0){
							for(Seeds temp : seeds){						

				    			//调用api得到Feeds的json数据
				    			JSONObject jsonObject=getFeeds.callAPI((temp.getUserName()!=null)?temp.getUserName():temp.getFacebookId(),"feed",temp.getFeedsPreviousPage());
				    			//数据抽取，将json转换为bean的格式
				    			//上一句返回的json对象有可能是含有错误信息的json对象，但由于正确和错误的类型的json对象都是顶层对象，不需要做判断
				    			List<Feeds> feedsList=getFeeds.dataExtract(jsonObject,temp.getSeedsId());
				    			
				    			//开始在线爬取首次遍历以后的feed
				    			if(!feedsList.get(0).getStatus().equals("error")){
				    				if(!feedsList.get(0).getStatus().equals("empty")){
				    					feedsDao.batchInsert(feedsList);
					    				log.info(temp.getName()+"的feed批量插入成功!");
					    				//将最新的翻页的链接写入seeds，注意，此处只应该更新nextpage链接
					    				seedsDao.updatePreviousPage(feedsList.get(0),temp.getSeedsId());
					    				log.info(temp.getName()+"的previous翻页链接更新成功!");
					    				String sql="UPDATE seeds set crawed_feeds=? WHERE seeds_id=?";
					    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-2);
					    				log.info("种子的Crawed_Feeds爬取状态更新为-2（已经经过至少一轮未来数据采集）成功!\n\n");
				    				}else{
				    					log.info(temp.getName()+"的涂鸦墙暂时没有新贴，将crawed_Feeds置为-2，继续采集下一个种子！");
				    					//将已经爬取过的种子标记值更新为-2,即使它没有采到数据，防止其很久没有发帖，影响其他活跃种子的采集，表示经过了一次未来数据的遍历
					    				String sql="UPDATE seeds set crawed_feeds=? WHERE seeds_id=?";
					    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-2);
					    				log.info("种子的Crawed_Feeds爬取状态更新为-2（已经经过至少一轮未来数据采集）成功!睡眠15秒\n\n");
					    				try {
											Thread.sleep(15*1000);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
				    				}		
				    			}else{
				    				log.error(temp.getName()+"的feed获取失败，继续采集下一个种子！");
				    				//此处可能会出现”(#12) location field is deprecated for versions v2.3 and higher“错误
				    				//此处有可能会出现(#1) An unknown error has occurred.
				    				//还有可能(#100) "Unsupported get request.
				    				//由于这个是在爬取未来的信息，如果出现这两个错误，说明这个种子不能进行未来数据的采集。由于这是facebook给出的翻页链接本身有问题，所以跳过并标记此种子为-3，继续下一个
				    				if(feedsList.get(0).getCodeMessage().equals("12") || feedsList.get(0).getCodeMessage().equals("1")|| feedsList.get(0).getCodeMessage().equals("100")){
				    					log.info(temp.getName()+"的历史feed出现Facebook自身链接的错误，Code： "+feedsList.get(0).getCodeMessage()+",该种子无法进行未来数据遍历，将Crawed_feed状态更新为-3，继续采集下一个种子！");
					    				String sql="UPDATE seeds set crawed_feeds=? WHERE seeds_id=?";
					    				//更新为-3表示放弃对这个种子进行未来事件的爬取(也无法进行爬取，因为未来事件的链接无法更新)
					    				seedsDao.updateCrawed(sql,temp.getSeedsId(),-3);
					    				log.info("种子的Crawed_Feeds爬取状态更新为-3成功!\n\n");
				    				}
				    				log.info("为避免系统问题，睡眠15秒钟\n\n");
				    				try {
										Thread.sleep(15*1000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
				    				
				    			}
						}
					}else{//状态为-1的种子已经全部遍历一遍，现在为状态为-2，可以开始重置种子为-1(不包括状态为-3的种子)，继续进行未来爬行
						String sql="UPDATE seeds set crawed_feeds=? WHERE crawed_feeds=-2";
						seedsDao.resetCrawed(sql,-1);
	    				log.info("自爬虫启动以来，第” "+cycle+" “次的轮询已完成，Crawed_Feeds重置为-1成功!\n\n");
	    				log.info("在开始下一轮在线轮询时，考虑到用户涂鸦的频繁度，睡眠15分钟，也可以防止请求过于频繁");
	    				try {
							Thread.sleep(900*1000);
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
