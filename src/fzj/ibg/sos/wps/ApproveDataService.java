package fzj.ibg.sos.wps;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;

public class ApproveDataService extends AbstractAuthenticatedSelfDescribingAlgorithm {

	private static DatabaseOperation dbInstance = null;
	private static Logger log = Logger.getLogger(ApproveDataService.class.getName());
	protected static int processingStatusLevel1;
	protected static int processingStatusLevel2a;
	protected static int processingStatusLevel2b;
	protected static int processingStatusLevel2c;
	protected static int unevaluatedId;

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData) {
		dbInstance = DatabaseOperation.getInstance();
		processingStatusLevel1 = DatabaseOperation.getProcessingStatusLevel1();
		processingStatusLevel2a = DatabaseOperation.getProcessingStatusLevel2a();
		processingStatusLevel2b = DatabaseOperation.getProcessingStatusLevel2b();
		processingStatusLevel2c = DatabaseOperation.getProcessingStatusLevel2c();
		unevaluatedId = DatabaseOperation.getUnevaluatedId();

		if (inputData == null) {
			throw new RuntimeException("Error while allocating input parameters: no parameters were submitted");
		}
		if (!inputData.containsKey(Constants.PROCEDURE_PARAMETER_NAME)) {
			throw new RuntimeException("Error while allocating input parameters: missing Parameter " + Constants.PROCEDURE_PARAMETER_NAME);
		}
		if (!inputData.containsKey(Constants.SESSION_ID)) {
			throw new RuntimeException("Error while allocating input parameters: missing Parameter " + Constants.SESSION_ID);
		}

		String user = this.checkSessionAndGetPloneUsername((String) this.getScalarData(Constants.SESSION_ID, inputData).getPayload());
		int sourceId = dbInstance.getSourcefromUsername(user);

		Date timeStamp = null;
		Date beginTimeInterval = null;
		Date endTimeInterval = null;
		if (inputData.containsKey(Constants.TIMESTAMP_PARAMETER_NAME)) {
			if (inputData.containsKey(Constants.TIME_INTERVAL_START_PARAMETER_NAME) || inputData.containsKey(Constants.TIME_INTERVAL_END_PARAMETER_NAME)) {
				throw new RuntimeException("Error while allocating input parameters: together using of timestamp and timeinterval is not permitted");
			} else {
				// time stamp
				timeStamp = this.parseTimeStamp((String) this.getScalarData(Constants.TIMESTAMP_PARAMETER_NAME, inputData).getPayload());
				log.info("TimeStamp:" + timeStamp.toLocaleString());
			}
		} else if (inputData.containsKey(Constants.TIME_INTERVAL_START_PARAMETER_NAME) || inputData.containsKey(Constants.TIME_INTERVAL_END_PARAMETER_NAME)) {
			// time interval
			beginTimeInterval = this.parseTimeStamp((String) this.getScalarData(Constants.TIME_INTERVAL_START_PARAMETER_NAME, inputData).getPayload());
			endTimeInterval = this.parseTimeStamp((String) this.getScalarData(Constants.TIME_INTERVAL_END_PARAMETER_NAME, inputData).getPayload());
			log.info("Begin:" + beginTimeInterval.toLocaleString());
			log.info("End:" + endTimeInterval.toLocaleString());
		} else {
			throw new RuntimeException("Error while allocating input parameters: at least one of timestamp or time interval has to be present");
		}

		List<String> phenomenons = this.getListData(Constants.PHENOMENON_PARAMETER_NAME, inputData);
		String procedure = (String) this.getScalarData(Constants.PROCEDURE_PARAMETER_NAME, inputData).getPayload();
		// to-do: we assume that one wps request involves one station with > 1
		// properties. All properties are belong to the same data value table.
		String dataClass = DatabaseOperation.getDataDirectoryMap().get(procedure).get(phenomenons.get(0));
		log.info("Phenomenon : " + phenomenons.toString());
		log.info("Procedure :" + procedure);
		log.info("DataClass :" + dataClass);
		boolean status;
		if (timeStamp != null) {
			status = updateDataByPoint(timeStamp, phenomenons, procedure, sourceId, dataClass);
		} else {
			// time interval
			status = updateDataByRange(beginTimeInterval, endTimeInterval, phenomenons, procedure, sourceId, dataClass);
		}

		HashMap<String, IData> result = new HashMap<String, IData>();
		LiteralBooleanBinding resultValue = new LiteralBooleanBinding(status);
		result.put(Constants.BOOLEAN_RESULT_PARAMETER, resultValue);
		return result;
	}

	// modify -> timePoint
	public boolean updateDataByPoint(Date timeStamp, List<String> phenomenons, String procedure, int sourceId, String dataClass) {
		boolean status = true;
		HibernateUtil.getInstance();
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			String ql = "update "+Constants.OBSERVATION_SCHEMA+"." + dataClass + " set processingstatusid =:processing , ";
			ql += "modifiedsourceid=:source , modified =:timevalid ";
			ql += "from cv.variables, observationreferences.sites ";
			ql += "where timestampto =:time and ";
			ql += "variableid=variables.objectid and siteid=sites.objectid and sites.code =:procedure and variables.code in (:phenomenons) ";
			ql += "and processingstatusid <> :levelOne and qualifierid= :unevaluated and (processingstatusid =:level2a or processingstatusid =:level2b) ";

			Query query = session.createSQLQuery(ql.toString()).setInteger("processing", processingStatusLevel2c).setInteger("source", sourceId).setTimestamp("timevalid", new Date()).setTimestamp("time", timeStamp).setString("procedure", procedure).setParameterList("phenomenons", phenomenons)
					.setInteger("levelOne", processingStatusLevel1).setInteger("level2a", processingStatusLevel2a).setInteger("level2b", processingStatusLevel2b).setInteger("unevaluated", unevaluatedId);
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

	public boolean updateDataByRange(Date start, Date end, List<String> phenomenons, String procedure,  int sourceId, String dataClass) {
		Transaction tx = null;
		boolean status = true;
		HibernateUtil.getInstance();
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		try {
			tx = session.beginTransaction();

			String ql = "update "+Constants.OBSERVATION_SCHEMA+"." + dataClass + " set processingstatusid =:processing , ";
			ql += "modifiedsourceid=:source , modified =:timevalid ";
			ql += "from cv.variables, observationreferences.sites ";
			ql += "where timestampto between :start and :end and ";
			ql += "variableid=variables.objectid and siteid=sites.objectid and sites.code =:procedure and variables.code in (:phenomenons) ";
			ql += "and processingstatusid <> :levelOne and qualifierid= :unevaluated and (processingstatusid =:level2a or processingstatusid =:level2b) ";

			Query query = session.createSQLQuery(ql.toString()).setInteger("processing", processingStatusLevel2c).setInteger("source", sourceId).setTimestamp("timevalid", new Date()).setTimestamp("start", start).setTimestamp("end", end).setString("procedure", procedure).setParameterList("phenomenons", phenomenons)
					.setInteger("levelOne", processingStatusLevel1).setInteger("level2a", processingStatusLevel2a).setInteger("level2b", processingStatusLevel2b).setInteger("unevaluated", unevaluatedId);
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

	private Date parseTimeStamp(String timeStamp) {
		try {
			return Constants.DEFAULT_DATE_FORMAT.parse(timeStamp);
		} catch (ParseException e) {
			throw new RuntimeException("Error while allocating input parameters: timestamp parameter does not match pattern " + Constants.DEFAULT_DATE_FORMAT_PATTERN);
		}
	}

	private IData getScalarData(String parameterName, Map<String, List<IData>> inputData) {
		List<IData> data = inputData.get(parameterName);
		if (data == null || data.size() != 1) {
			throw new RuntimeException("Error while allocating input parameters: no data for parameter " + Constants.PHENOMENON_PARAMETER_NAME + " was submitted");
		}
		return data.get(0);
	}
	
	private List<String> getListData(String parameterName, Map<String, List<IData>> inputData) {
		if (inputData.containsKey(parameterName)) {
			List<String> result = new ArrayList<String>();
			for (IData data : inputData.get(parameterName)) {
				result.add((String) data.getPayload());
			}
			return result;
		}
		return null;

	}
	
	@Override
	public Class<?> getInputDataType(String id) {
		if (id.equals(Constants.PHENOMENON_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		} else if (id.equals(Constants.PROCEDURE_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		} else if (id.equals(Constants.TIMESTAMP_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		} else if (id.equals(Constants.TIME_INTERVAL_START_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		} else if (id.equals(Constants.TIME_INTERVAL_END_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		} else if (id.equals(Constants.SESSION_ID)) {
			return LiteralStringBinding.class;
		}
		throw new RuntimeException("input parameter " + id + " is not supported");
	}

	@Override
	public Class<?> getOutputDataType(String id) {
		if (id.equals(Constants.BOOLEAN_RESULT_PARAMETER)) {
			return LiteralBooleanBinding.class;
		}
		throw new RuntimeException("output parameter " + id + " is not supported");
	}

	@Override
	public List<String> getInputIdentifiers() {
		List<String> inputParameters = new ArrayList<String>();
		inputParameters.add(Constants.PHENOMENON_PARAMETER_NAME);
		inputParameters.add(Constants.PROCEDURE_PARAMETER_NAME);
		inputParameters.add(Constants.TIMESTAMP_PARAMETER_NAME);
		inputParameters.add(Constants.TIME_INTERVAL_START_PARAMETER_NAME);
		inputParameters.add(Constants.TIME_INTERVAL_END_PARAMETER_NAME);
		inputParameters.add(Constants.SESSION_ID);
		return inputParameters;
	}

	@Override
	public List<String> getOutputIdentifiers() {
		List<String> outputParameters = new ArrayList<String>();
		outputParameters.add(Constants.BOOLEAN_RESULT_PARAMETER);
		return outputParameters;
	}
}
