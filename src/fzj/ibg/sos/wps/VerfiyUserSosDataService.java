package fzj.ibg.sos.wps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;

public class VerfiyUserSosDataService extends
		AbstractAuthenticatedSelfDescribingAlgorithm {
	private static Logger log = Logger
			.getLogger(VerfiyUserSosDataService.class.getName());
	
	
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
		String session=(String)this.getScalarData(Constants.SESSION_ID, inputData).getPayload();
		HashMap<String, IData> result = new HashMap<String, IData>();
		String user=this.checkSessionAndGetPloneUsername(session);
		LiteralStringBinding resultValue = new LiteralStringBinding(user);
		log.info("successfully validating user: "+user);
		result.put(Constants.STRING_RESULT_PARAMETER, resultValue);
		return result;
	}
	
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
