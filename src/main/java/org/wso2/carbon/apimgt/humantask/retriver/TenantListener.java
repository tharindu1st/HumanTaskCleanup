package org.wso2.carbon.apimgt.humantask.retriver;

import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

public class TenantListener extends AbstractAxis2ConfigurationContextObserver {

	@Override
	public void createdConfigurationContext(ConfigurationContext configContext) {
		String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
		int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
		initializeHumanTaskCleanupTask(tenantId, tenantDomain);
	}

	public void initializeHumanTaskCleanupTask(int tenantId, String tenantDomain) {

			new CleanupHumanTasks(tenantId,tenantDomain);


	}
}
