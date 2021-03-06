package api.facebook.daoImpl;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import api.facebook.bean.Feeds;
import api.facebook.dao.FeedsDao;

@Repository
public class FeedsDaoImpl implements FeedsDao
{
	private JdbcTemplate jdbcTemplate;
	private static final Logger log=Logger.getLogger(PostDaoImpl.class);
	
	@Autowired
	public void setDataSource(DataSource dataSource){
		jdbcTemplate=new JdbcTemplate(dataSource);
	}

	@Override
	public int[] batchInsert(List<Feeds> feeds) {
		String SQL_INSERT_FEED=
				"INSERT INTO feeds (message_id,message,from_user_id,from_user_name,seeds_id,description,name,link,picture,type,created_time) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		List<Object[]> batch = new ArrayList<Object[]>();
		for (Feeds feed : feeds) {
            Object[] values = new Object[] {
            		feed.getMessageId(),
    				feed.getMessage(),
    				feed.getFromUserId(),
    				feed.getFromUserName(),
    				feed.getSeedsId(),
    				feed.getDescription(),
    				feed.getName(),			    				
    				feed.getLink(),
    				feed.getPicture(),
    				feed.getType(),
    				feed.getCreatedTime()
    		};
            batch.add(values);
        }
		
		try{
			int[] updateCounts = jdbcTemplate.batchUpdate(
					SQL_INSERT_FEED,
	                batch);
	        return updateCounts;
		}catch(Exception e){
				log.error("数据库批量插入种子人物“"+feeds.get(0).getSeedsId()+"号”的Feed数据出错，错误信息："+e.getMessage());
				return new int[] {1};
		}
	}
	
	
}
