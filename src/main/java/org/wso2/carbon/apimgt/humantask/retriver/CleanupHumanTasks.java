package org.wso2.carbon.apimgt.humantask.retriver;

import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.*;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.*;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.logging.*;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.humantask.retriver.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.*;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowConstants;
import org.wso2.carbon.humantask.stub.ui.task.client.api.HumanTaskClientAPIAdminStub;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalArgumentFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalStateFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.types.TSimpleQueryInput;
import org.wso2.carbon.humantask.stub.ui.task.client.api.types.TStatus;
import org.wso2.carbon.humantask.stub.ui.task.client.api.types.TTaskSimpleQueryResultRow;
import org.wso2.carbon.humantask.stub.ui.task.client.api.types.TTaskSimpleQueryResultSet;

import javax.xml.stream.XMLStreamException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class CleanupHumanTasks {

	private static final Log log = LogFactory.getLog(CleanupHumanTasks.class);
	private static final String Human_TASK_CLEANUP_FREQUENCY = "human.task.cleanup.frequency";
	private HumanTaskClientAPIAdminStub stub = null;
	private String bpsServiceUrl, user, password;

	public CleanupHumanTasks(int tenantId, String tenantDomain) {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
		                                                                     new ThreadFactory() {

			                                                                     public Thread newThread(
					                                                                     Runnable r) {
				                                                                     Thread t = new Thread(r);
				                                                                     t.setName(
						                                                                     "HumanTask Cleanup " +
						                                                                     "Task");
				                                                                     return t;
			                                                                     }
		                                                                     });
		String humanTaskCleanupFrequency = System.getProperty(Human_TASK_CLEANUP_FREQUENCY);
		if (humanTaskCleanupFrequency == null) {
			humanTaskCleanupFrequency = "3600000";
		}

		if (log.isDebugEnabled()) {
			log.debug("Human Task Cleanup frequency set it to" + humanTaskCleanupFrequency);
		}

		executor.scheduleAtFixedRate(new CleanupTask(tenantDomain, tenantId),
		                             Integer.parseInt(humanTaskCleanupFrequency),
		                             Integer.parseInt(humanTaskCleanupFrequency), TimeUnit.MILLISECONDS);

	CleanUpHumanTaskConfigurationService.getInstance().stringScheduledExecutorServiceMap.put(tenantDomain,executor);
	}

	private class CleanupTask implements Runnable {
		String tenantDomain;
		int tenantId;

		public CleanupTask(String tenantDomain, int tenantId) {
			this.tenantDomain = tenantDomain;
			this.tenantId = tenantId;
		}

		public void run() {
			if (log.isDebugEnabled()) {
				log.debug("Running the cleanup task");
			}
			synchronized (tenantDomain) {
				HumanTaskCleanupDao humanTaskCleanupDao = HumanTaskUtil.getHumanTask(tenantDomain, tenantId);
				if (humanTaskCleanupDao != null && humanTaskCleanupDao.isTaskCleanupEnabled()) {
					user = humanTaskCleanupDao.getBpsUserName();
					password = humanTaskCleanupDao.getBpsPassword();
					bpsServiceUrl = humanTaskCleanupDao.getBpsServiceUrl();

					try {
						stub = new HumanTaskClientAPIAdminStub(
								ServiceReferenceHolder.getContextService().getClientConfigContext(),
								bpsServiceUrl + "HumanTaskClientAPIAdmin");
					} catch (AxisFault axisFault) {
						log.error("Connection Refused to BPS connection", axisFault);
					}
					if (stub != null) {

						ServiceClient client = stub._getServiceClient();
						Options options = client.getOptions();
						options.setManageSession(true);
						HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
						auth.setUsername(user);
						auth.setPassword(password);
						auth.setPreemptiveAuthentication(true);
						List<String> authSchemes = new ArrayList<String>();
						authSchemes.add(HttpTransportProperties.Authenticator.BASIC);
						auth.setAuthSchemes(authSchemes);
						options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE,
						                    auth);
						client.setOptions(options);
					}
					TSimpleQueryInput tSimpleQueryInput = new TSimpleQueryInput();
					TStatus tStatus1 = new TStatus();
					tStatus1.setTStatus("RESERVED");
					TStatus tStatus2 = new TStatus();
					tStatus2.setTStatus("IN_PROGRESS");
					TStatus tStatus3 = new TStatus();
					tStatus3.setTStatus("READY");
					TStatus[] tStatusList = { tStatus1, tStatus2, tStatus3 };
					tSimpleQueryInput.setStatus(tStatusList);
					ApiMgtDAO apiMgtDAO = new ApiMgtDAO();

					try {
						TTaskSimpleQueryResultSet simpleQueryResultSet = stub.simpleQuery(tSimpleQueryInput);
						Set<TTaskSimpleQueryResultRow> simpleQueryResultSetRow =
								new HashSet<TTaskSimpleQueryResultRow>(Arrays.asList(
										simpleQueryResultSet.getRow()));
						List<WorkflowDTO> workflowDTOList = HumanTaskUtil.retrievePendingWorkFlows(tenantId);
						for (WorkflowDTO workflowDTO : workflowDTOList) {

							if (WorkflowConstants.WF_TYPE_AM_APPLICATION_CREATION
									.equals(workflowDTO.getWorkflowType())) {
								removeApplicationCreationWorkflowTasks(workflowDTO, simpleQueryResultSetRow, apiMgtDAO);

							} else if (WorkflowConstants.WF_TYPE_AM_SUBSCRIPTION_CREATION
									.equals(workflowDTO.getWorkflowType())) {
								removeSubscriptionCreationWorkflowTasks(workflowDTO, simpleQueryResultSetRow,
								                                        apiMgtDAO);
							} else if (WorkflowConstants.WF_TYPE_AM_APPLICATION_REGISTRATION_PRODUCTION
									.equals(workflowDTO.getWorkflowType())) {
								removeApplicationRegistrationWorkFlowTasks(workflowDTO, simpleQueryResultSetRow,
								                                           apiMgtDAO);

							} else if (WorkflowConstants.WF_TYPE_AM_APPLICATION_REGISTRATION_SANDBOX
									.equals(workflowDTO.getWorkflowType())) {
								removeApplicationRegistrationWorkFlowTasks(workflowDTO, simpleQueryResultSetRow,
								                                           apiMgtDAO);

							}

						}

					} catch (APIManagementException e) {
						log.error(e.getMessage(),e);
					} catch (RemoteException e) {
						log.error("Couldn't Log into BPS Host", e);
					} catch (IllegalArgumentFault illegalArgumentFault) {
						log.error("Couldn't checked with states",illegalArgumentFault);
					} catch (IllegalStateFault illegalStateFault) {
						log.error("Couldn't checked with states", illegalStateFault);

					}

				}
			}
		}

		/**
		 * Returns a workflow object for a given external workflow reference.
		 */

		private void removeTaskFromBPS(String taskId) throws AxisFault {
			final String Exit_HumanTASK_ACTION =
					"http://docs.oasis-open.org/ns/bpel4people/ws-humantask/protocol/200803/exit";
			ServiceClient client =
					new ServiceClient(ServiceReferenceHolder.getContextService()
					                                        .getClientConfigContext(),
					                  null);
			Options options = client.getOptions();
			options.setAction(Exit_HumanTASK_ACTION);
			options.setTo(new EndpointReference(bpsServiceUrl + "HumanTaskProtocolHandler"));

			HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();

			auth.setUsername(user);
			auth.setPassword(password);
			auth.setPreemptiveAuthentication(true);
			List<String> authSchemes = new ArrayList<String>();
			authSchemes.add(HttpTransportProperties.Authenticator.BASIC);
			auth.setAuthSchemes(authSchemes);
			options.setProperty(Constants.Configuration.MESSAGE_TYPE, HTTPConstants.MEDIA_TYPE_TEXT_XML);
			options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
			options.setManageSession(true);
			client.setOptions(options);

			String payload = "<ns:exit xmlns:ns=\"http://docs.oasis-open" +
			                 ".org/ns/bpel4people/ws-humantask/protocol/200803\">\n" +
			                 "<ns:taskID>$1</ns:taskID>\n" +
			                 "</ns:exit>";
			payload = payload.replace("$1", taskId);
			try {
				client.fireAndForget(AXIOMUtil.stringToOM(payload));
			} catch (XMLStreamException e) {
				log.error("couldn't transform payload",e);

			}
		}

		private void removeApplicationCreationWorkflowTasks(WorkflowDTO workflowDTO,
		                                                    Set<TTaskSimpleQueryResultRow> simpleQueryResultRows,
		                                                    ApiMgtDAO apiMgtDAO)
				throws AxisFault, APIManagementException {
			if (apiMgtDAO.getApplicationById(Integer.parseInt(workflowDTO.getWorkflowReference())) ==
			    null) {
				for (TTaskSimpleQueryResultRow queryResultRow : simpleQueryResultRows) {
					if (queryResultRow.getPresentationSubject().toString()
					                  .contains(workflowDTO.getExternalWorkflowReference())) {
						removeTaskFromBPS(queryResultRow.getId().toString());
						HumanTaskUtil.removeWorkFlowReferenceFromTable(workflowDTO.getExternalWorkflowReference());
						simpleQueryResultRows.remove(queryResultRow);
						break;
					}

				}

			}
		}

		private void removeSubscriptionCreationWorkflowTasks(WorkflowDTO workflowDTO,
		                                                     Set<TTaskSimpleQueryResultRow> simpleQueryResultSetRow,
		                                                     ApiMgtDAO apiMgtDAO) throws APIManagementException,
		                                                                                 AxisFault {
			if (apiMgtDAO.getSubscriptionStatusById(Integer.parseInt(workflowDTO.getWorkflowReference())) ==
			    null) {
				for (TTaskSimpleQueryResultRow queryResultRow : simpleQueryResultSetRow) {
					if (queryResultRow.getPresentationSubject().toString()
					                  .contains(workflowDTO.getExternalWorkflowReference())) {
						removeTaskFromBPS(queryResultRow.getId().toString());
						HumanTaskUtil.removeWorkFlowReferenceFromTable(workflowDTO.getExternalWorkflowReference());
						simpleQueryResultSetRow.remove(queryResultRow);
						break;
					}
				}
			}
		}

		private void removeApplicationRegistrationWorkFlowTasks(WorkflowDTO workflowDTO,
		                                                        Set<TTaskSimpleQueryResultRow> simpleQueryResultSetRow,
		                                                        ApiMgtDAO apiMgtDAO)
				throws APIManagementException, AxisFault {

			ApplicationRegistrationWorkflowDTO applicationRegistrationWorkflowDTO =
					(ApplicationRegistrationWorkflowDTO) workflowDTO;
			Application application = applicationRegistrationWorkflowDTO.getApplication();
			if (application == null || apiMgtDAO.getApplicationById(application.getId()) == null) {
				for (TTaskSimpleQueryResultRow queryResultRow : simpleQueryResultSetRow) {
					if (queryResultRow.getPresentationSubject().toString()
					                  .contains(workflowDTO.getExternalWorkflowReference())) {
						removeTaskFromBPS(queryResultRow.getId().toString());
						HumanTaskUtil.removeWorkFlowReferenceFromTable(workflowDTO.getExternalWorkflowReference());
						simpleQueryResultSetRow.remove(queryResultRow);
						break;
					}
				}
			}
		}
	}
}