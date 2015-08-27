package fzj.ibg.sos.wps;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.fzj.ibg.odm.HibernateUtilForWebApplication;
import org.fzj.ibg.odm.tables.datavalues.DataValue;
import org.fzj.ibg.odm.tables.datavalues.ProcessingStatus;
import org.fzj.ibg.odm.tables.datavalues.QualifierGroup;
import org.fzj.ibg.odm.tables.management.Source;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class UpdateFlagSosDataService extends AddFlagSosDataService {

	private static Logger log = Logger.getLogger(UpdateFlagSosDataService.class);

	/**
	 * TODO: UpdateFlagSosDataService.doUpdate/3 has the same semantic as
	 * AddFlagSosDataService.doUpdate/3 therefore remove this implementation
	 */

	@Override
	protected void doUpdate(List<DataValue> data, HibernateUtilForWebApplication tm, QualifierGroup qualifierGroup, Source validationSource) throws Exception {
		int i = 0;

		// ProcessingStatus processingStatus2d = (ProcessingStatus)
		// tm.loadObject(new ProcessingStatus(), Constants.PROCESSING_LEVEL_2D);
		ProcessingStatus processingStatus2c = (ProcessingStatus) tm.loadObject(new ProcessingStatus(), Constants.PROCESSING_LEVEL_2C);
		for (DataValue dataValue : data) {
			log.debug("updating dataset with key: " + dataValue.getId());

			if (!(dataValue.getProcessingStatus().getAbbreviation().equals(Constants.QUALIFY_CONTROL_LEVEL_RAW_DATA))) {
				if (dataValue.getProcessingStatus().getAbbreviation().equals(Constants.PROCESSING_LEVEL_2A) || dataValue.getProcessingStatus().getAbbreviation().equals(Constants.PROCESSING_LEVEL_2B)) {
					// 2a,2b -> 2c
					dataValue.setQualifierGroup(qualifierGroup);
					dataValue.setProcessingStatus(processingStatus2c);
					tm.update(dataValue);
					i++;
				} else {
					// dont update data level, just quality flags (2c->2c)
					dataValue.setQualifierGroup(qualifierGroup);
					tm.update(dataValue);
					i++;
				}
				dataValue.setModifiedSource(validationSource);
				dataValue.setModified(new Date());
			}
		}
		log.info("update ProcessingStatus done. " + i + " object(s) updated ... (not commited)");

	}

	@Override
	public boolean updateDataByPoint(Date timeStamp, List<String> phenomenons, String procedure, int flag, int sourceId, String dataClass) {
		boolean status = true;
		Session session = HibernateUtil.getInstance().getSessionFactory().getCurrentSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			String ql = "update "+Constants.OBSERVATION_SCHEMA+"." + dataClass + " set qualifierid=:qualifier , processingstatusid =:processing , ";
			ql += "modifiedsourceid=:source , modified =:timevalid ";
			ql += "from cv.variables, observationreferences.sites ";
			ql += "where timestampto =:time and ";
			ql += "variableid=variables.objectid and siteid=sites.objectid and sites.code =:procedure and variables.code in (:phenomenons) ";
			ql += "and processingstatusid <> :levelOne and (processingstatusid =:level2a or processingstatusid =:level2b or processingstatusid =:level2c) ";

			Query query = session.createSQLQuery(ql.toString()).setInteger("qualifier", flag).setInteger("processing", processingStatusLevel2c).setInteger("source", sourceId)
					.setTimestamp("timevalid", new Date()).setTimestamp("time", timeStamp).setString("procedure", procedure).setParameterList("phenomenons", phenomenons)
					.setInteger("levelOne", processingStatusLevel1).setInteger("level2a", processingStatusLevel2a).setInteger("level2b", processingStatusLevel2b)
					.setInteger("level2c",processingStatusLevel2c);
			int result = query.executeUpdate();
			tx.commit();
			log.info("Update ProcessingStatus done. " + result + " object(s) updated.");
		} catch (HibernateException e) {
			status = false;
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
		}
		return status;
	}

	@Override
	public boolean updateDataByRange(Date start, Date end, List<String> phenomenons, String procedure, int flag, int sourceId, String dataClass) {
		Transaction tx = null;
		boolean status = true;
		Session session = HibernateUtil.getInstance().getSessionFactory().getCurrentSession();
		try {
			tx = session.beginTransaction();
			String ql = "update " +Constants.OBSERVATION_SCHEMA+"." + dataClass + " set qualifierid=:qualifier , processingstatusid =:processing , ";
			ql += "modifiedsourceid=:source , modified =:timevalid ";
			ql += "from cv.variables, observationreferences.sites ";
			ql += "where timestampto between :start and :end and ";
			ql += "variableid=variables.objectid and siteid=sites.objectid and sites.code =:procedure and variables.code in (:phenomenons) ";
			ql += "and processingstatusid <> :levelOne and (processingstatusid =:level2a or processingstatusid =:level2b or processingstatusid =:level2c) ";

			Query query = session.createSQLQuery(ql.toString()).setInteger("qualifier", flag).setInteger("processing", processingStatusLevel2c)
					.setInteger("source", sourceId).setTimestamp("timevalid", new Date()).setTimestamp("start", start).setTimestamp("end", end)
					.setString("procedure", procedure).setParameterList("phenomenons", phenomenons)
					.setInteger("levelOne", processingStatusLevel1).setInteger("level2a", processingStatusLevel2a).setInteger("level2b", processingStatusLevel2b)
					.setInteger("level2c",processingStatusLevel2c);
			int result = query.executeUpdate();
			tx.commit();
			log.info("Update ProcessingStatus done. " + result + " object(s) updated.");
		} catch (HibernateException e) {
			status = false;
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
		}
		return status;
	}

}
