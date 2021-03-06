package api.facebook.main;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;

import api.facebook.bean.To;
import api.facebook.dao.SeedsDao;
import api.facebook.dao.ToDao;
import api.facebook.util.AppContext;

/**
 * 用于手动通过to_other表中的公众人物，扩展种子表
 * 需要选择合适的人物加入种子
 * 并将has_add_to_seeds 更新为1
 * @author chenkedi
 *
 */
@Controller
public class ExpandSeeds
{
	@Resource
	private ToDao toDao;
	@Resource 
	private SeedsDao seedsDao;
	private static final Logger log = Logger.getLogger(ExpandSeeds.class);
	
	public static void main(String[] args){
		
		log.info("正在创建数据库连接和缓冲池...");
	    AppContext.initAppCtx();
	    log.info("数据库连接已连接！缓冲池已建立");
	    
	    ExpandSeeds crawler= (ExpandSeeds) AppContext.appCtx.getBean(ExpandSeeds.class);
	    crawler.run();
	}

	public void run(){
		
		//首先读取to_other中合适的人物
		List<To> tos=toDao.readCandidateSeeds();
		
		//将其插入到seeds表中，注意seeds表中的facebook具有unique索引，所以相同的人物是无法插入的
		if(tos!=null && tos.size()!=0){
			int[] tag=seedsDao.batchInsert(tos);
			if(tag[0]>=0){
				int affectedRows=0;
				log.info("批量插入候选种子人物成功！共插入"+tag[0]+"条");
				
				for(To to : tos){
					affectedRows++;
					toDao.updateHasAddToSeeds(to);
					log.info("更新"+to.getPageName()+"的has_add_to_seeds成功！");
				}
				log.info("批量插入候选种子人物成功！共插入"+affectedRows+"条");
			}
			
			
		}
		
	}
}
