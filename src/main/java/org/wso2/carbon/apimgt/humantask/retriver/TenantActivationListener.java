package org.wso2.carbon.apimgt.humantask.retriver;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class TenantActivationListener extends AbstractAxis2ConfigurationContextObserver {
	private static final Log log = LogFactory.getLog(TenantActivationListener.class);

	@Override
	public void createdConfigurationContext(ConfigurationContext configContext) {
		String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
		int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
		initializeHumanTaskCleanupTask(tenantId, tenantDomain);
	}

	public void initializeHumanTaskCleanupTask(int tenantId, String tenantDomain) {
		new CleanupHumanTasks(tenantId, tenantDomain);
	}

	@Override public void terminatedConfigurationContext(ConfigurationContext configCtx) {

		String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

		Map<String, ScheduledExecutorService> executorServiceMap =
				CleanUpHumanTaskConfigurationService.getInstance().stringScheduledExecutorServiceMap;
		executorServiceMap.get(tenantDomain).shutdown();
		executorServiceMap.remove(tenantDomain);
	}
}
