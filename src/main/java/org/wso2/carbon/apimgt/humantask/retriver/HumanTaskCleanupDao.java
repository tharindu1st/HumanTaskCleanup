package org.wso2.carbon.apimgt.humantask.retriver;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.humantask.retriver.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;

public class HumanTaskCleanupDao implements Serializable {
	private static final Log log = LogFactory.getLog(HumanTaskCleanupDao.class);
	public static final String Human_Task_Cleanup_LOCATION = "/apimgt/applicationdata/task-cleanup.xml";

	private static final QName PROP_Q = new QName("Property");

	private static final QName ATT_NAME = new QName("name");
	private static final String HUMAN_TASK_CLEANUP = "HumanTaskCleanup";

	private boolean taskCleanupEnabled;

	private String bpsServiceUrl;

	private String bpsUserName;

	private String bpsPassword;

	public HumanTaskCleanupDao(int tenantId,String tenantDomain) {
		boolean isTenantFlowStarted = false;

		try {
			if (tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
				isTenantFlowStarted = true;
				PrivilegedCarbonContext.startTenantFlow();
				PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
			}
		InputStream in;

		try {

			Registry registry = ServiceReferenceHolder.getInstance().
					getRegistryService().getGovernanceSystemRegistry(tenantId);
			Resource resource = registry.get(Human_Task_Cleanup_LOCATION);
			if (resource != null) {
				in = resource.getContentStream();

				StAXOMBuilder builder = new StAXOMBuilder(in);

				SecretResolver secretResolver = SecretResolverFactory.create(builder.getDocumentElement(), true);
				OMElement humanTaskCleanupElements = builder.getDocument().getFirstChildWithName(
						new QName(HUMAN_TASK_CLEANUP));

				String securevaultKey = WorkflowConstants.API_MANAGER + "." + HUMAN_TASK_CLEANUP + "." +
				                        humanTaskCleanupElements.getLocalName() + "." + WorkflowConstants.PASSWORD;

				for (Iterator it = humanTaskCleanupElements.getChildrenWithName(PROP_Q); it.hasNext(); ) {
					OMElement propertyElem = (OMElement) it.next();
					String propName = propertyElem.getAttribute(ATT_NAME).getAttributeValue();

					if (propName != null) {
						if ("taskCleanupEnable".equalsIgnoreCase(propName)) {
							taskCleanupEnabled = (Boolean.parseBoolean(propertyElem.getText()));
						} else if ("serviceUrl".equalsIgnoreCase(propName)) {
							bpsServiceUrl = propertyElem.getText().trim();
						} else if ("username".equalsIgnoreCase(propName)) {
							bpsUserName = propertyElem.getText().trim();
						} else if (WorkflowConstants.PASSWORD_.equals(propName)) {
							if (secretResolver.isInitialized() && secretResolver.isTokenProtected(securevaultKey)) {
								bpsPassword = secretResolver.resolve(securevaultKey);
							} else {
								bpsPassword = propertyElem.getText();
							}
						}
					} else {
						String msg = "human task Cleanup configuration is not done successful";
						log.error(msg);
						throw new Exception(msg);
					}
				}
			}
		} catch (RegistryException e) {
			log.error("task-cleanup.xml couldn't find in" + Human_Task_Cleanup_LOCATION);
		} catch (XMLStreamException e) {
			log.error("task-cleanup.xml couldn't build", e);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		} finally {
			if (isTenantFlowStarted) {
				PrivilegedCarbonContext.endTenantFlow();
			}
		}
	}

	public boolean isTaskCleanupEnabled() {
		return taskCleanupEnabled;
	}

	public void setTaskCleanupEnabled(boolean taskCleanupEnabled) {
		this.taskCleanupEnabled = taskCleanupEnabled;
	}

	public String getBpsServiceUrl() {
		return bpsServiceUrl;
	}

	public void setBpsServiceUrl(String bpsServiceUrl) {
		this.bpsServiceUrl = bpsServiceUrl;
	}

	public String getBpsUserName() {
		return bpsUserName;
	}

	public void setBpsUserName(String bpsUserName) {
		this.bpsUserName = bpsUserName;
	}

	public String getBpsPassword() {
		return bpsPassword;
	}

	public void setBpsPassword(String bpsPassword) {
		this.bpsPassword = bpsPassword;
	}

}
