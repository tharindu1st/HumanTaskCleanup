package org.wso2.carbon.apimgt.humantask.retriver;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledExecutorService;

public class CleanUpHumanTaskConfigurationService {

	public static CleanUpHumanTaskConfigurationService instance = new CleanUpHumanTaskConfigurationService();
	public Map<String, ScheduledExecutorService> stringScheduledExecutorServiceMap = new ConcurrentSkipListMap<String,
			ScheduledExecutorService>();

	public static synchronized CleanUpHumanTaskConfigurationService getInstance() {
		if (instance == null) {
			instance = new CleanUpHumanTaskConfigurationService();
		}
		return instance;
	}

}
