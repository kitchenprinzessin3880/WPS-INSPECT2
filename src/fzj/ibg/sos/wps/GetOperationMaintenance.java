package fzj.ibg.sos.wps;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fzj.ibg.odm.tables.sites.SitesLog;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetOperationMaintenance extends AbstractAuthenticatedSelfDescribingAlgorithm {
	private static final Logger LOGGER = LoggerFactory.getLogger(GetOperationMaintenance.class);
	private static DatabaseOperation dbInstance = null;
	SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	public Map<String, IData> run(Map<String, List<IData>> inputData) {
		dbInstance = DatabaseOperation.getInstance();
		String site = null;
		if (inputData == null) {
			throw new RuntimeException("Error while allocating input parameters: no parameters were submitted");
		}

		if (inputData.containsKey(Constants.PROCEDURE_PARAMETER_NAME)) {
			site = (String) this.getScalarData(Constants.PROCEDURE_PARAMETER_NAME, inputData).getPayload();
			LOGGER.info("Station: " + site);
		} else {
			throw new RuntimeException("Error while allocating input parameters: missing Parameter " + Constants.PROCEDURE_PARAMETER_NAME);
		}

		Map<String, IData> result = null;
		Date timeStamp = null;
		Date beginTimeInterval = null;
		Date endTimeInterval = null;
		if (inputData.containsKey(Constants.TIMESTAMP_PARAMETER_NAME)) {
			if (inputData.containsKey(Constants.TIME_INTERVAL_START_PARAMETER_NAME) || inputData.containsKey(Constants.TIME_INTERVAL_END_PARAMETER_NAME)) {
				throw new RuntimeException("Error while allocating input parameters: together using of timestamp and timeinterval is not permitted");
			} else {
				// time stamp
				timeStamp = this.parseTimeStamp((String) this.getScalarData(Constants.TIMESTAMP_PARAMETER_NAME, inputData).getPayload());
				LOGGER.info("TimeStamp : " + timeStamp.toGMTString());
				//start = end
				result = getOMByTimeRange(site, timeStamp,timeStamp);
			}
		} else if (inputData.containsKey(Constants.TIME_INTERVAL_START_PARAMETER_NAME) || inputData.containsKey(Constants.TIME_INTERVAL_END_PARAMETER_NAME)) {
			// time interval
			beginTimeInterval = this.parseTimeStamp((String) this.getScalarData(Constants.TIME_INTERVAL_START_PARAMETER_NAME, inputData).getPayload());
			endTimeInterval = this.parseTimeStamp((String) this.getScalarData(Constants.TIME_INTERVAL_END_PARAMETER_NAME, inputData).getPayload());
			LOGGER.info("Begin : " + beginTimeInterval.toGMTString());
			LOGGER.info("End : " + endTimeInterval.toGMTString());
			result = getOMByTimeRange(site, beginTimeInterval, endTimeInterval);
		} else {
			throw new RuntimeException("Error while allocating input parameters: at least one of timestamp or time interval has to be present");
		}
		return result;
	}

	private Map<String, IData> getOMByTimeRange(String sensor, Date start, Date end) {
		Session session = HibernateUtil.getInstance().getSessionFactory().getCurrentSession();
		Transaction tx = null;
		List<SitesLog> results = null;
		HashMap<String, IData> result = new HashMap<String, IData>();
		LiteralBooleanBinding resultBoolean = new LiteralBooleanBinding(true);
		try {
			tx = session.beginTransaction();
			String hql = "from SitesLog as log where log.site.code= :procedure ";
			hql += "and log.timestampto <=:end and (log.timestampto is null OR log.timestampto >= :start) order by log.timestampfrom";
			results = session.createQuery(hql.toString()).setString("procedure", sensor).setTimestamp("start", start).setTimestamp("end", end).list();
			LOGGER.info("Total results :" + results.size());
			String resultValues = "";
			
			if (results != null && results.size() > 0) {
				for (SitesLog sl : (List<SitesLog>) results) {
					String text = sl.getDiarytext();
					String source = sl.getSource().getSurname();
					String flag = sl.getQualifier().getCode();
					String site = sl.getSite().getCode();
					Date fromDate = sl.getTimestampfrom();
					Date toDate = sl.getTimestampto();
					String fromDateStr = "";
					String toDateStr = "";
					if (fromDate != null) {
						fromDateStr = Constants.DEFAULT_DATE_FORMAT.format(fromDate);
					}
					if (toDate != null) {
						toDateStr = Constants.DEFAULT_DATE_FORMAT.format(toDate);
					}
					resultValues += fromDateStr + ","+ toDateStr +"," +site +","+ text + "," + flag + "," + source +";";
				}
				
				if (resultValues.length() > 0 && resultValues.charAt(resultValues.length() - 1) == ';') {
					resultValues = resultValues.substring(0, resultValues.length() - 1).trim();
				}
			}
			else {
				resultValues="noData";
				resultBoolean = new LiteralBooleanBinding(false);
			}

			//LOGGER.info("Data values.................. : " + resultValues);
			//LiteralStringBinding sensorValue = new LiteralStringBinding(sensor);
			LiteralStringBinding valueAll = new LiteralStringBinding(resultValues);
			result.put(Constants.BOOLEAN_RESULT_PARAMETER, resultBoolean);
			//result.put(Constants.PROCEDURE_PARAMETER_NAME, sensorValue);
			result.put(Constants.STRING_RESULT_PARAMETER, valueAll);

		} catch (HibernateException e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
		}

		return result;

	}

	private Date parseTimeStamp(String timeStamp) {
		try {
			return DATE_FORMAT.parse(timeStamp);
		} catch (ParseException e) {
			throw new RuntimeException("Error while allocating input parameters: timestamp parameter does not match pattern yyyy-MM-dd'T'HH:mm:ssZ");
		}
	}

	private IData getScalarData(String parameterName, Map<String, List<IData>> inputData) {
		List<IData> data = inputData.get(parameterName);
		if (data == null || data.size() != 1) {
			throw new RuntimeException("Error while allocating input parameters: no data for parameter " + Constants.PHENOMENON_PARAMETER_NAME + " was submitted");
		}
		return data.get(0);
	}

	@Override
	public Class<?> getInputDataType(String id) {
		if (id.equals(Constants.PROCEDURE_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		} else if (id.equals(Constants.TIMESTAMP_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		} else if (id.equals(Constants.TIME_INTERVAL_START_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		} else if (id.equals(Constants.TIME_INTERVAL_END_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		}
		throw new RuntimeException("Input parameter " + id + " is not supported");
	}

	@Override
	public Class<?> getOutputDataType(String id) {
		if (id.equals(Constants.BOOLEAN_RESULT_PARAMETER)) {
			return LiteralBooleanBinding.class;
		}
		/*
		else if (id.equals(Constants.PROCEDURE_PARAMETER_NAME)) {
			return LiteralStringBinding.class;
		} */
		else if (id.equals(Constants.STRING_RESULT_PARAMETER)) {
			return LiteralStringBinding.class;
		}
		throw new RuntimeException("Output parameter " + id + " is not supported");
	}

	@Override
	public List<String> getInputIdentifiers() {
		List<String> inputParameters = new ArrayList<String>();
		inputParameters.add(Constants.PROCEDURE_PARAMETER_NAME);
		inputParameters.add(Constants.TIMESTAMP_PARAMETER_NAME);
		inputParameters.add(Constants.TIME_INTERVAL_START_PARAMETER_NAME);
		inputParameters.add(Constants.TIME_INTERVAL_END_PARAMETER_NAME);
		return inputParameters;
	}

	@Override
	public List<String> getOutputIdentifiers() {
		List<String> outputParameters = new ArrayList<String>();
		outputParameters.add(Constants.BOOLEAN_RESULT_PARAMETER);
		//outputParameters.add(Constants.PROCEDURE_PARAMETER_NAME);
		outputParameters.add(Constants.STRING_RESULT_PARAMETER);
		return outputParameters;
	}
}
