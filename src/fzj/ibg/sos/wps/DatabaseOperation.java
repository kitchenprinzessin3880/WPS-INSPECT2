package fzj.ibg.sos.wps;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fzj.ibg.odm.tables.datavalues.DataValue;
import org.fzj.ibg.odm.tables.datavalues.ProcessingStatus;
import org.fzj.ibg.odm.tables.datavalues.QualifierGroup;
import org.fzj.ibg.odm.tables.management.DataDirectory;
import org.fzj.ibg.odm.tables.management.Source;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseOperation {
	private static final Logger LOG = LoggerFactory.getLogger(DatabaseOperation.class);
	private static DatabaseOperation instance;
	private static Map<Integer, String> processingLevels = null;
	private static Map<String, Integer> qualifiers = null;
	private static Map<String, HashMap<String, String>> dataDirectoryMap = null;
	protected static int processingStatusLevel1;
	protected static int processingStatusLevel2a;
	protected static int processingStatusLevel2b;
	protected static int processingStatusLevel2c;
	protected static int unevaluatedId;

	public static DatabaseOperation getInstance() {
		if (instance == null) {
			instance = new DatabaseOperation();
		}
		return instance;
	}

	private DatabaseOperation() {
		getDataClassFromDb();
		getProcessingStatiFromDb();
		getQualifiersFromDb();
	}

	public int getSourcefromUsername(String aidaUserName) {
		Long source = null;
		String sourceCode = null;
		Session session = HibernateUtil.getInstance().getSessionFactory().getCurrentSession();
		Transaction tx = null;

		try {
			tx = session.beginTransaction();
			Query query = session.createQuery("from Source where externalaccountname = :ext ");
			query.setParameter("ext", aidaUserName);
			List<Source> rs = query.list();
			if (rs.size() > 0) {
				Source s = (Source) rs.get(0);
				source = s.getId();
			}
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (HibernateException e1) {
					LOG.debug("Error rolling back transaction: getSourcefromUsername()");
				}
				throw e;
			}
		}
		return Integer.valueOf(Long.toString(source));
	}

	public void getProcessingStatiFromDb() {
		processingLevels = new HashMap<Integer, String>();
		Session session = HibernateUtil.getInstance().getSessionFactory().getCurrentSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			Criteria criteria = session.createCriteria(ProcessingStatus.class);
			List<ProcessingStatus> result = criteria.list();
			for (Iterator iterator = result.iterator(); iterator.hasNext();) {
				ProcessingStatus ps = (ProcessingStatus) iterator.next();
				String levelName = ps.getAbbreviation();
				// {1=1, 2=2a, 3=2b, 4=2c, 5=3, 6=2d}
				if (levelName.equalsIgnoreCase(Constants.QUALIFY_CONTROL_LEVEL_RAW_DATA)) {
					processingStatusLevel1 = Integer.valueOf(Long.toString(ps.getId()));
				}
				if (levelName.equalsIgnoreCase(Constants.PROCESSING_LEVEL_2A)) {
					processingStatusLevel2a = Integer.valueOf(Long.toString(ps.getId()));
				}
				if (levelName.equalsIgnoreCase(Constants.PROCESSING_LEVEL_2B)) {
					processingStatusLevel2b = Integer.valueOf(Long.toString(ps.getId()));
				}
				if (levelName.equalsIgnoreCase(Constants.PROCESSING_LEVEL_2C)) {
					processingStatusLevel2c = Integer.valueOf(Long.toString(ps.getId()));
				}
				processingLevels.put(Integer.valueOf(Long.toString(ps.getId())), ps.getAbbreviation());
			}
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (HibernateException e1) {
					LOG.debug("Error rolling back transaction: requestProcessingStatiFromDb()");
				}
				throw e;
			}
		}
	}

	public void getQualifiersFromDb() {
		qualifiers = new HashMap<String, Integer>();
		Session session = HibernateUtil.getInstance().getSessionFactory().getCurrentSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			Criteria criteria = session.createCriteria(QualifierGroup.class);
			List<QualifierGroup> result = criteria.list();
			StringBuffer sb = new StringBuffer();
			for (QualifierGroup qg : result) {
				String genericFlag = qg.getGroup().getCode();
				String specificFlag = qg.getQualifier().getCode();
				qualifiers.put(genericFlag + "_" + specificFlag, Integer.valueOf(Long.toString(qg.getId())));
			}
			tx.commit();
			String unevaluated = Constants.QUALIFIER_UNEVALUATED + "_" + Constants.QUALIFIER_UNEVALUATED;
			unevaluatedId = qualifiers.get(unevaluated);
		} catch (RuntimeException e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (HibernateException e1) {
					LOG.debug("Error rolling back transaction: requestQualifiersFromDb()");
				}
				throw e;
			}
		}
	}

	private static void getDataClassFromDb() {
		dataDirectoryMap = new HashMap<String, HashMap<String, String>>();
		Session session = HibernateUtil.getInstance().getSessionFactory().getCurrentSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			Criteria criteria = session.createCriteria(DataDirectory.class);
			List<DataDirectory> result = criteria.list();
			for (Iterator iterator = result.iterator(); iterator.hasNext();) {
				DataDirectory d = (DataDirectory) iterator.next();
				String site = d.getSite().getCode();
				String prop = d.getVariable().getCode();
				//ASD 13.11.2014
				//String dclass = d.getDataTableClass().getCode();
				String dclass = d.getDatatableclassname();
				Long siteId = d.getSite().getId();
				Long propId = d.getVariable().getId();
				if (dataDirectoryMap.containsKey(site)) {
					dataDirectoryMap.get(site).put(prop, dclass);
				} else {
					HashMap<String, String> map = new HashMap<String, String>();
					map.put(prop, dclass);
					dataDirectoryMap.put(site, map);
				}
			}
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (HibernateException e1) {
					LOG.debug("Error rolling back transaction: getDataClassFromDb()");
				}
				throw e;
			}
		}
	}

	public String verifySessionAndGetPloneUsername(String sessionId) {
		LOG.info("[DBOperation] sessionId: " + sessionId);
		String result = null;
		try {
			String url = Constants.getInstance().getUserManagementServiceUrl();
			if (url != null) {
				result = PloneAuthentication.getInstance(url).getUserBySessionCookie(sessionId);
				LOG.info("user: " + result);
			}
		} catch (Exception e) {
			try {
				throw new Exception("Error while authenticating user:", e);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		if (result == null) {
			throw new RuntimeException(new Exception("Error while verifying session: " + sessionId));
		}
		return result;
	}

	private Date parseTimeStamp(String timeStamp) {
		try {
			return Constants.DEFAULT_DATE_FORMAT.parse(timeStamp);
		} catch (ParseException e) {
			throw new RuntimeException("Error while allocating input parameters: timestamp parameter does not match pattern " + Constants.DEFAULT_DATE_FORMAT_PATTERN);
		}
	}

	public int getProcessingLevelIdByName(String name) {
		int pId = 0;
		for (Entry<Integer, String> entry : processingLevels.entrySet()) {
			int key = entry.getKey();
			String value = entry.getValue();
			if (value.equals(name)) {
				pId = key;
				break;
			}

		}
		return pId;
	}

	public static Map<Integer, String> getProcessingLevels() {
		return processingLevels;
	}

	public static void setProcessingLevels(Map<Integer, String> processingLevels) {
		DatabaseOperation.processingLevels = processingLevels;
	}

	public static Map<String, Integer> getQualifiers() {
		return qualifiers;
	}

	public static void setQualifiers(Map<String, Integer> qualifiers) {
		DatabaseOperation.qualifiers = qualifiers;
	}

	public static int getProcessingStatusLevel1() {
		return processingStatusLevel1;
	}

	public static void setProcessingStatusLevel1(int processingStatusLevel1) {
		DatabaseOperation.processingStatusLevel1 = processingStatusLevel1;
	}

	public static int getProcessingStatusLevel2a() {
		return processingStatusLevel2a;
	}

	public static void setProcessingStatusLevel2a(int processingStatusLevel2a) {
		DatabaseOperation.processingStatusLevel2a = processingStatusLevel2a;
	}

	public static int getProcessingStatusLevel2b() {
		return processingStatusLevel2b;
	}

	public static void setProcessingStatusLevel2b(int processingStatusLevel2b) {
		DatabaseOperation.processingStatusLevel2b = processingStatusLevel2b;
	}

	public static int getProcessingStatusLevel2c() {
		return processingStatusLevel2c;
	}

	public static void setProcessingStatusLevel2c(int processingStatusLevel2c) {
		DatabaseOperation.processingStatusLevel2c = processingStatusLevel2c;
	}

	public static int getUnevaluatedId() {
		return unevaluatedId;
	}

	public static void setUnevaluatedId(int unevaluatedId) {
		DatabaseOperation.unevaluatedId = unevaluatedId;
	}

	public static Map<String, HashMap<String, String>> getDataDirectoryMap() {
		return dataDirectoryMap;
	}

	public static void setDataDirectoryMap(Map<String, HashMap<String, String>> dataDirectoryMap) {
		DatabaseOperation.dataDirectoryMap = dataDirectoryMap;
	}

}
