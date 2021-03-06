package api.facebook.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * 根据操作系统来判断如何初始化applicationContext（spring容器）
 * 对classpath方式没有太大意义，对filePath方式有意义
 */
public class AppContext {

	public static  ApplicationContext appCtx;
	// 从classpath初始化spring容器
	public static  void initAppCtx() {
		if(OSUtil.isWindowsOS()){
			appCtx = new ClassPathXmlApplicationContext("applicationContext.xml");
		}else{
			appCtx = new ClassPathXmlApplicationContext("applicationContext.xml");
		}
		
	}

}