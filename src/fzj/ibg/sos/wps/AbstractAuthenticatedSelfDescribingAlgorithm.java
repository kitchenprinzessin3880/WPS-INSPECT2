package fzj.ibg.sos.wps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fzj.ibg.odm.HibernateUtilForWebApplication;
import org.fzj.ibg.odm.tables.datavalues.DataValue;
import org.fzj.ibg.odm.tables.datavalues.QualifierGroup;
import org.fzj.ibg.odm.tables.management.SensorDataStatus;
import org.fzj.ibg.odm.tables.management.Source;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fzj.ibg3.exceptions.IbgException;

public abstract class AbstractAuthenticatedSelfDescribingAlgorithm extends
		AbstractSelfDescribingAlgorithm {
	private static final Logger log = LoggerFactory
			.getLogger(AbstractAuthenticatedSelfDescribingAlgorithm.class);

	protected String checkSessionAndGetPloneUsername(String sessionId) {
		String result;
		try {
			result = this.getUsername(sessionId);
		} catch (IbgException e) {
			throw new RuntimeException(e);
		}
		if (result == null) {
			throw new RuntimeException(new IbgException(
					"error while verifying session: " + sessionId));
		}
		return result;
	}

	protected void checkSession(String sessionId) {
		this.checkSessionAndGetPloneUsername(sessionId);
	}

	// protected Source getSource(String username) {
	// Source source=Source.getSourceFromExternalAccount(username);
	// // List<?> sources = tm.createQuery("from Source where code=:username")
	// // .setString("username", username).list();
	// if (source != null) {
	// return source;
	// }
	// throw new RuntimeException(new IbgException("error: user not found: "
	// + username));
	// }

	/*
	 * comment out ASD 11.11.2014 protected List<UserSiteVariablePermission>
	 * getUserPermissions_Old(Source source) { HibernateUtilForWebApplication tm
	 * = HibernateUtilForWebApplication.getInstance(); List<?> result =
	 * tm.createQuery
	 * ("from UserSiteVariablePermission where source=:source").setParameter
	 * ("source", source).list(); return (List<UserSiteVariablePermission>)
	 * result; }
	 */

	// ASD 11.11.2014
	public Map<String, String> getUserPermissions(Long id) {
		Session session2 = HibernateUtil.getInstance().getSessionFactory()
				.getCurrentSession();
		Map<String, HashSet<String>> allowedSiteProps = new HashMap<String, HashSet<String>>();
		Transaction trx = null;
		try {
			trx = session2.beginTransaction();
			String sql = "select datastatus from SensorDataStatus datastatus,SensorInstance sensorinst, ResponsibilityGroup responsibilitygroup, "
					+ "Responsibility responsibility where "
					+ "responsibility.code = :quality and responsibility.code=responsibilitygroup.responsibility.code "
					+ "and responsibilitygroup.source.id = :i  "
					+ "and responsibilitygroup.sourceGroup.id=sensorinst.sourceGroup.id "
					+ "and sensorinst.site.id = datastatus.site.id and sensorinst.id=datastatus.sensorInstance.id";
			Query q = session2.createQuery(sql);
			q.setParameter("i", id);
			q.setParameter("quality", Constants.RESPONSIBILITYQC);
			List<SensorDataStatus> result = q.list();

			if (result.size() > 0) {
				for (Iterator iterator = result.iterator(); iterator.hasNext();) {
					SensorDataStatus r = (SensorDataStatus) iterator.next();
					String site = r.getSite().getCode();
					String prop = r.getVariable().getCode();
					if (allowedSiteProps.get(site) == null) {
						HashSet<String> list = new HashSet<String>();
						list.add(prop);
						allowedSiteProps.put(site, list);
					} else {
						HashSet<String> exisList = (HashSet<String>) allowedSiteProps
								.get(site);
						exisList.add(prop);
					}
				}
			}
			trx.commit();
		} catch (RuntimeException e) {
			if (trx != null && trx.isActive()) {
				try {
					trx.rollback();
				} catch (HibernateException e1) {
				}
				throw e;
			}
		}
		Map<String, String> allowedSitePropsUnique = new HashMap<String, String>();
		for (Entry<String, HashSet<String>> entry : allowedSiteProps.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().toString();
			allowedSitePropsUnique.put(key, value);

		}
		return allowedSitePropsUnique;
	}

	public Source getSourceByUsername(String usr) {
		HibernateUtilForWebApplication tm = HibernateUtilForWebApplication
				.getInstance();
		List<DataValue> data = null;
		StringBuffer hql = new StringBuffer(
				"from  DataValue as datavalue where datavalue.timestampto= :timeStamp and datavalue.site.code= :procedure ");
		Transaction t = tm.getSession().beginTransaction();
		Source source = null;
		try {
			Query query = tm.getSession().createQuery(
					"from Source where externalaccountname = :ext ");
			query.setParameter("ext", usr);
			List<Source> rs = query.list();
			if (rs.size() > 0) {
				source = (Source) rs.get(0);
			}
			t.commit();
		} catch (RuntimeException e) {
			if (t != null && t.isActive()) {
				try {
					t.rollback();// Second try catch as the rollback could fail
									// as well
				} catch (HibernateException e1) {
					log.debug("Error rolling back transaction: getSourcefromUsername()");
				}
				// throw again the first exception
				throw e;
			}
		}
		return source;
	}

	protected String getUsername(String sessionId) throws IbgException {
		log.info("SessionId: " + sessionId);
		String result = null;
		try {
			String url = Constants.getInstance().getUserManagementServiceUrl();
			if (url != null) {
				result = PloneAuthentication.getInstance(url)
						.getUserBySessionCookie(sessionId);
				log.info("Username: " + result);
			}
			return result;
		} catch (Exception e) {
			throw new IbgException("error while authenticating user:", e);
		}
	}

}
