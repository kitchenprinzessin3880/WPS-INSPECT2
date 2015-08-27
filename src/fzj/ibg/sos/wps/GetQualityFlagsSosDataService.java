package fzj.ibg.sos.wps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.fzj.ibg.odm.HibernateUtilForWebApplication;
import org.fzj.ibg.odm.tables.datavalues.QualifierGroup;
import org.hibernate.Transaction;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;

import fzj.ibg3.exceptions.IbgException;

public class GetQualityFlagsSosDataService extends
		AbstractAuthenticatedSelfDescribingAlgorithm {
	private static Logger log = Logger
			.getLogger(GetQualityFlagsSosDataService.class.getName());

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
		String session = (String) this.getScalarData(Constants.SESSION_ID,
				inputData).getPayload();
		String user = this.checkSessionAndGetPloneUsername(session);

		HibernateUtilForWebApplication hibernate = HibernateUtilForWebApplication
				.getInstance();
		HashMap<String, IData> result = new HashMap<String, IData>();
//		LiteralStringBinding resultValue = new LiteralStringBinding(
//				"13,baddata,autosampler;15,baddata,irregular;18,baddata,isolatedspike;6,baddata,max;5,baddata,min;12,doubtful,autosampler;17,doubtful,max;16,doubtful,min;14,doubtful,missing;4,gapfilled,extrapolated;3,gapfilled,interpolated;11,ok,autosampler;2,ok,ok;10,unevaluated,autosampler;1,unevaluated,unevaluated");
		LiteralStringBinding resultValue = new LiteralStringBinding(this.getQualifiers());
		result.put(Constants.STRING_RESULT_PARAMETER, resultValue);
		return result;
	}

	private String getQualifiers() {
		HibernateUtilForWebApplication hibernate = HibernateUtilForWebApplication
				.getInstance();
		Transaction t = hibernate.getSession().beginTransaction();
		List<QualifierGroup> qualifierGroups = hibernate.createQuery(
				"from QualifierGroup").list();
		StringBuffer sb = new StringBuffer();
		for (QualifierGroup qg : qualifierGroups) {
			sb.append(qg.getId()).append(",").append(qg.getGroup().getCode())
					.append(",").append(qg.getQualifier().getCode())
					.append(";");
		}
		int len=sb.length();
		if(len>0){
			sb.setLength(len-1);
		}
		return sb.toString();
	}

	private IData getScalarData(String parameterName,
			Map<String, List<IData>> inputData) {
		List<IData> data = inputData.get(parameterName);
		if (data == null || data.size() != 1) {
			throw new RuntimeException(
					"Error while allocating input parameters: no data for parameter "
							+ Constants.PHENOMENON_PARAMETER_NAME
							+ " was submitted");
		}
		return data.get(0);
	}

	@Override
	public Class<?> getInputDataType(String id) {
		if (id.equals(Constants.SESSION_ID)) {
			return LiteralStringBinding.class;
		}
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
		return inputParameters;
	}

	@Override
	public List<String> getOutputIdentifiers() {
		List<String> outputParameters = new ArrayList<String>();
		outputParameters.add(Constants.STRING_RESULT_PARAMETER);
		return outputParameters;
	}

}
