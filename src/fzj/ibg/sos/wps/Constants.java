package fzj.ibg.sos.wps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import org.slf4j.LoggerFactory;

public class Constants {
	private static Constants _this;

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Constants.class);
	public static final String WPS_PROC_GETFLAGS = "fzj.ibg.sos.wps.GetQualityFlagsSosDataService";
	public static final String WPS_PROC_GETPROCSTATUS = "fzj.ibg.sos.wps.GetProcessingStatiSosDataService";
	public static final String WPS_PROC_ADDFLAG = "fzj.ibg.sos.wps.AddFlagSosDataService";
	public static final String WPS_PROC_UPDATEFLAG = "fzj.ibg.sos.wps.UpdateFlagSosDataService";
	public static final String WPS_PROC_GETSENPROPBYUSR = "fzj.ibg.sos.wps.GetPermittedObservedProperties";
	public static final String WPS_PROC_GETOPERATIONMAINTENANCE = "fzj.ibg.sos.wps.GetOperationMaintenance";
	public static final String USER_NAME = "username";
	public static final String SESSION_ID = "sessionId";
	public static final String FLAG = "flag";
	public static final String STRING_RESULT_PARAMETER = "stringResult";
	public static final String RESPONSIBILITYQC ="quality";
	public static final String SOS_URL_PARAMETER_NAME = "sos_url";
	public static final String NEW_DATA_VALUE_PARAMETER = "datavalue";
	public static final String PHENOMENON_PARAMETER_NAME = "phenomenon";
	public static final String PROCEDURE_PARAMETER_NAME = "procedure";
	public static final String TIMESTAMP_PARAMETER_NAME = "timestamp";
	public static final String TIME_INTERVAL_START_PARAMETER_NAME = "begin";
	public static final String TIME_INTERVAL_END_PARAMETER_NAME = "end";
	public static final String BOOLEAN_RESULT_PARAMETER = "booleanResult";

	public static final String QUALIFY_CONTROL_LEVEL_RAW_DATA = "1";
	public static final String PROCESSING_LEVEL_2A = "2a";
	public static final String PROCESSING_LEVEL_2B = "2b";
	public static final String PROCESSING_LEVEL_2C = "2c";
	public static final String OBSERVATION_SCHEMA ="terenodata";
	
	public static final String DEFAULT_DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ssZ";
	public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat(
			DEFAULT_DATE_FORMAT_PATTERN);

	public static final String QUALIFIER_UNEVALUATED = "unevaluated";
	
	private String userManagementServiceUrl = "";
	private static final File CONFIG_FILE = new File("ibg.wps.data.modify.cfg");
	private Properties props;
	private static final String USER_MANAGEMENT_SERVICE_URL_PN = "USER_MANAGEMENT_SERVICE_URL";

	public static Constants getInstance() throws FileNotFoundException,
			IOException {
		if (_this == null) {
			_this = new Constants();
		}
		return _this;
	}


	public String getUserManagementServiceUrl() {
		return userManagementServiceUrl;
	}
	
	private Constants() throws FileNotFoundException, IOException {
		this.props = new Properties();
		LOG.info("env:\n" + System.getenv().toString());
		LOG.info("loading configuration from file: " + CONFIG_FILE);
		try {
			this.props.load(new FileInputStream(CONFIG_FILE));
			LOG.info("loading complete: " + this.props.toString());
			if (this.props.containsKey(USER_MANAGEMENT_SERVICE_URL_PN)) {
				this.userManagementServiceUrl = this.props
						.getProperty(USER_MANAGEMENT_SERVICE_URL_PN);
				LOG.info("USER_MANAGEMENT_SERVICE_URL: "
						+ this.props
								.getProperty(USER_MANAGEMENT_SERVICE_URL_PN));
			}
		} catch (Exception e) {
			LOG.error("error while loading configuration (now working with default configuration)",e);
		}
	}

}
