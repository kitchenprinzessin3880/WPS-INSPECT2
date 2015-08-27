package fzj.ibg.sos.wps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.fzj.ibg.odm.HibernateUtilForWebApplication;
import org.fzj.ibg.odm.tables.management.Source;
import org.hibernate.Transaction;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;

import fzj.ibg3.exceptions.IbgException;

public class GetPermittedObservedProperties extends
		AbstractAuthenticatedSelfDescribingAlgorithm {

	private static Logger log = Logger.getLogger(VerfiyUserSosDataService.class
			.getName());

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData) {
		if (inputData == null) {
			throw new RuntimeException(
					"Error while allocating input parameters: no parameters were submitted");
		}
		if (!inputData.containsKey(Constants.SESSION_ID)) {
			throw new RuntimeException(
					"Error while allocating input parameters: missing Parameter "
							+ Constants.SESSION_ID);
		}
		String session = (String) this.getScalarData(Constants.SESSION_ID,inputData).getPayload();

		HashMap<String, IData> result = new HashMap<String, IData>();
		String user = this.checkSessionAndGetPloneUsername(session);
		log.info("successfully validating user: " + user);
		HibernateUtilForWebApplication tb = HibernateUtilForWebApplication
				.getInstance();
		Hashtable<String, String> siteAndPhenomenons = new Hashtable<String, String>();
		Transaction t = tb.getSession().beginTransaction();
		    
		try {
			// Source source = this.getSource(user);
			//Source source = Source.getSourceFromExternalAccountWithoutTransaction(user);
			Source source =  getSourceByUsername(user);
			if(source==null){
				throw new IbgException("found no source object for the given user name: "+ user);
			}
			
			/* commet out ASD 11.11.2014
			for (UserSiteVariablePermission permissions : this.getUserPermissions(source)) {
				String siteCode = permissions.getSite().getCode();
				String observedProperties = "";
				if (siteAndPhenomenons.containsKey(siteCode)) {
					observedProperties = new StringBuffer(
							siteAndPhenomenons.get(siteCode)).append(",")
							.append(permissions.getVariable().getCode())
							.toString();
				} else {
					observedProperties = (permissions.getVariable().getCode());
				}
				siteAndPhenomenons.put(siteCode, observedProperties);
			} */
			
			Map<String, String> siteAndPhenomenonsTemp = getUserPermissions(source.getId());
		} catch (Exception e) {
			t.rollback();
			throw new RuntimeException(e);
		}
		t.commit();

		LiteralStringBinding resultValue = new LiteralStringBinding(this.mapToString(siteAndPhenomenons));
		result.put(Constants.STRING_RESULT_PARAMETER, resultValue);
		return result;
	}

	private String mapToString(Map<String, String> data) {
		StringBuilder sb = new StringBuilder();
		for (String station : data.keySet()) {
			String param = data.get(station);
			sb.append(station).append(":").append(param).append(";");
		}
		return sb.toString();
	}

	// private

	private IData getScalarData(String parameterName,
			Map<String, List<IData>> inputData) {
		List<IData> data = inputData.get(parameterName);
		if (data == null || data.size() != 1) {
			throw new RuntimeException(
					"Error while allocating input parameters: no data for parameter "
							+ parameterName + " was submitted");
		}
		return data.get(0);
	}

	@Override
	public Class<?> getInputDataType(String id) {
		if (id.equals(Constants.SESSION_ID)) {
			return LiteralStringBinding.class;
		}
		// if (id.equals(Constants.PROCEDURE_PARAMETER_NAME)) {
		// return LiteralStringBinding.class;
		// }
		throw new RuntimeException("input parameter " + id
				+ " is not supported");
	}

	@Override
	public Class<?> getOutputDataType(String id) {
		if (id.equals(Constants.STRING_RESULT_PARAMETER)) {
			return LiteralStringBinding.class;
		}
		throw new RuntimeException("output parameter " + id
				+ " is not supported");
	}

	@Override
	public List<String> getInputIdentifiers() {
		List<String> inputParameters = new ArrayList<String>();
		inputParameters.add(Constants.SESSION_ID);
		// inputParameters.add(Constants.PROCEDURE_PARAMETER_NAME);
		return inputParameters;
	}

	@Override
	public List<String> getOutputIdentifiers() {
		List<String> outputParameters = new ArrayList<String>();
		outputParameters.add(Constants.STRING_RESULT_PARAMETER);
		return outputParameters;
	}

}
