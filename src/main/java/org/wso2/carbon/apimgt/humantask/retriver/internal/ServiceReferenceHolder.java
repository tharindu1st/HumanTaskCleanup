package org.wso2.carbon.apimgt.humantask.retriver.internal;

import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.utils.ConfigurationContextService;

public class ServiceReferenceHolder {

	private static final ServiceReferenceHolder instance = new ServiceReferenceHolder();

	private static ConfigurationContextService contextService;
	private APIManagerConfigurationService amConfigurationService;
	private RegistryService registryService;

	private ServiceReferenceHolder() {

	}

	public static ServiceReferenceHolder getInstance() {
		return instance;
	}

	public static ConfigurationContextService getContextService() {
		return contextService;
	}

	public static void setContextService(ConfigurationContextService contextService) {
		ServiceReferenceHolder.contextService = contextService;
	}

	public APIManagerConfigurationService getAPIManagerConfigurationService() {
		return amConfigurationService;
	}

	public void setAPIManagerConfigurationService(APIManagerConfigurationService amConfigurationService) {
		this.amConfigurationService = amConfigurationService;
	}

	public void setRegistryService(RegistryService registryService) {
		this.registryService=registryService;
	}

	public RegistryService getRegistryService() {
		return registryService;
	}
}
