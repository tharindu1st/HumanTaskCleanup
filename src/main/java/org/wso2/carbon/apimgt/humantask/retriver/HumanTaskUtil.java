package org.wso2.carbon.apimgt.humantask.retriver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dto.WorkflowDTO;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowExecutorFactory;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowStatus;

import javax.cache.Cache;
import javax.cache.Caching;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HumanTaskUtil {
	public static final String HUMAN_TASK_CLEANUP_CACHE = "humanTaskCleanupCache";
	private static final Log log = LogFactory.getLog(HumanTaskUtil.class);

	public static HumanTaskCleanupDao getHumanTask(String tenantDomain, int tenantId) {
		HumanTaskCleanupDao humanTaskCleanupDao;

		log.info("inget");
			String cacheName = tenantDomain + "_" + HUMAN_TASK_CLEANUP_CACHE;
			log.info("aftercachename");

			Cache workflowCache =
					Caching.getCacheManager(APIConstants.API_MANAGER_CACHE_MANAGER).getCache(HUMAN_TASK_CLEANUP_CACHE);
			log.info("get cache");
			humanTaskCleanupDao = (HumanTaskCleanupDao) workflowCache.get(cacheName);
			log.info("humantaskcleanupcache1");
			if (humanTaskCleanupDao == null) {
		log.info("humantaskcleanupcache2");
		humanTaskCleanupDao = new HumanTaskCleanupDao(tenantId, tenantDomain);
		log.info("humantaskcleanupcache3");
		workflowCache.put(cacheName, humanTaskCleanupDao);

		}
		return humanTaskCleanupDao;
	}

	public static List<WorkflowDTO> retrievePendingWorkFlows(int tenantId) throws APIManagementException {
		Connection connection = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		WorkflowDTO workflowDTO = null;
		List<WorkflowDTO> workflowDTOList = new ArrayList<WorkflowDTO>();
		final String query = "SELECT * FROM AM_WORKFLOWS WHERE WF_STATUS ='CREATED' AND TENANT_ID =?";
		try {

			connection = APIMgtDBUtil.getConnection();
			prepStmt = connection.prepareStatement(query);
			prepStmt.setInt(1, tenantId);

			rs = prepStmt.executeQuery();

			while (rs.next()) {

				workflowDTO = WorkflowExecutorFactory.getInstance().createWorkflowDTO(rs.getString("WF_TYPE"));
				workflowDTO.setStatus(WorkflowStatus.valueOf(rs.getString("WF_STATUS")));
				workflowDTO.setExternalWorkflowReference(rs.getString("WF_EXTERNAL_REFERENCE"));
				workflowDTO.setCreatedTime(rs.getTimestamp("WF_CREATED_TIME").getTime());
				workflowDTO.setWorkflowReference(rs.getString("WF_REFERENCE"));
				workflowDTO.setTenantDomain(rs.getString("TENANT_DOMAIN"));
				workflowDTO.setTenantId(rs.getInt("TENANT_ID"));
				workflowDTO.setWorkflowDescription(rs.getString("WF_STATUS_DESC"));
				workflowDTOList.add(workflowDTO);
			}

		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
		}

		return workflowDTOList;
	}

	public static void removeWorkFlowReferenceFromTable(String workFlowId) throws APIManagementException {
		Connection connection = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		String query = "DELETE FROM AM_WORKFLOWS WHERE WF_EXTERNAL_REFERENCE = ?";
		try {

			connection = APIMgtDBUtil.getConnection();
			prepStmt = connection.prepareStatement(query);
			prepStmt.setString(1, workFlowId);
			prepStmt.executeUpdate();
			connection.commit();
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
		}
	}
}
