package org.wso2.carbon.apimgt.humantask.retriver.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.apimgt.humantask.retriver.HumanTaskCleanupDao;
import org.wso2.carbon.apimgt.humantask.retriver.HumanTaskUtil;
import org.wso2.carbon.apimgt.humantask.retriver.TenantListener;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="org.wso2.carbon.apimgt.remote.gateway.activator" immediate="true"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"  bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 * @scr.reference name="apim.configuration"
 * interface="org.wso2.carbon.apimgt.impl.APIManagerConfigurationService"
 * cardinality="1..1" policy="dynamic" bind="setAPIManagerConfigurationService"
 * unbind="unsetAPIManagerConfigurationService"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 */
public class HumanTaskRetriverServiceComponent {
	private static final Log log = LogFactory.getLog(HumanTaskRetriverServiceComponent.class);

	private static APIManagerConfiguration configuration = null;

	protected void activate(ComponentContext componentContext) {

		log.info("HumanTask Retriever Admin Service Started");
		TenantListener tenantListener = new TenantListener();

					tenantListener.initializeHumanTaskCleanupTask(MultitenantConstants.SUPER_TENANT_ID,
		                                              org.wso2.carbon.utils.multitenancy.MultitenantConstants
				                                              .SUPER_TENANT_DOMAIN_NAME);
		componentContext.getBundleContext()
		                .registerService(Axis2ConfigurationContextObserver.class.getName(), tenantListener, null);

	}

	protected void setAPIManagerConfigurationService(APIManagerConfigurationService amcService) {
		if (log.isDebugEnabled()) {
			log.debug("API manager configuration service bound to the CustomWorkflowCallBackService");
		}
		configuration = amcService.getAPIManagerConfiguration();
		ServiceReferenceHolder.getInstance().setAPIManagerConfigurationService(amcService);
	}

	protected void unsetAPIManagerConfigurationService(APIManagerConfigurationService amcService) {
		if (log.isDebugEnabled()) {
			log.debug("API manager configuration service unbound from the CustomWorkflowCallBackService");
		}
		configuration = null;
	}

	public static APIManagerConfiguration getAPIManagerConfiguration() {
		return configuration;
	}

	protected void setConfigurationContextService(ConfigurationContextService contextService) {
		ServiceReferenceHolder.setContextService(contextService);
	}

	protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
		ServiceReferenceHolder.setContextService(null);
	}

	protected void setRegistryService(RegistryService registryService) {
		if (registryService != null && log.isDebugEnabled()) {
			log.debug("Registry service initialized");
		}
		ServiceReferenceHolder.getInstance().setRegistryService(registryService);
	}

	protected void unsetRegistryService(RegistryService registryService) {
		ServiceReferenceHolder.getInstance().setRegistryService(null);
	}

}
