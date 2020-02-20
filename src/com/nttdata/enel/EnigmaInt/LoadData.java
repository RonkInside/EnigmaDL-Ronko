package com.nttdata.enel.EnigmaInt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONException;

public class LoadData {
	protected static Logger logger = Logger.getLogger("com/nttdata/enel/EnigmaInt");

	static Properties connProps = new Properties();

	static Properties appProps = new Properties();

	static Properties fldConfProps = new Properties();

	static String connectionProps = System.getenv("CONNECTIONPROPS");

	static String applicationProps = System.getenv("APPLICATIONPROPS");

	static XSSFCell cellkhead;

	static String basicAuth = "";

	static String xlsInput = null;

	static String xlsOutput = null;

	static String xlsDiscarded = null;

	static String getSheetNum = null;

	static String firstRowDatax = null;

	static int firstRowData = 2;

	static String createDiscardedFile = null;

	static String insertRecord = null;

	static String updateRecord = null;

	static String checkMandatoryField = null;

	static String currentWorkBook = null;

	static XSSFSheet sheet;

	static XSSFRow headRow;

	static XSSFRow commentRow;

	static XSSFRow rowData;

	static int NoC;

	static int totalNumOfRows;

	static XSSFWorkbook workbookOut = null;

	static XSSFWorkbook wbDiscarded = null;

	static XSSFSheet sheetOut;

	static XSSFSheet sheetDiscarded;

	static int recCountOut = 0;

	static String getKeyTabString = "";

	static String postString = "";

	static String postStringKT = "";

	static String destTableName = "";

	static int driverFieldPosition = -1;

	static String driverFieldName = "u_enc_device_type";

	static String driverFieldNamePL = "u_enc_picklist_configuration";

	static String driverFieldValue = "";

	static int thePos = -1;

	static Boolean okToProceed = Boolean.valueOf(true);

	static String[] fieldListMand = null;

	static String[] fieldListPKey = null;

	static String[] fieldListAttr = null;

	static String[] fieldListGroup = null;

	static String[] fieldListExtRef = null;

	static String fieldMandatoryDefaultNotPermitted = "N.A.";

	static String currentStep = "";

	static String retCode = "0";

	static String retMesg = "";

	static String retRowSysID = "";

	public void convertFile(CloseableHttpClient httpClient, String proxyhost, String proxyport, String pdataFlow, String FlowName, String xlsFileName, String xlsOutput) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, HttpException, JSONException {
		try {
			Utils utils = new Utils();
			ValidateInputFile validateInputFile = new ValidateInputFile();
			workbookOut = new XSSFWorkbook();
			sheetOut = workbookOut.createSheet(pdataFlow);
			utils.writeRowOut(sheetOut, 0, "Operation", "Exit Status", "Destination Table", "Sys ID", "Notes");
			currentStep = "CONFIGURATION PROPERTIES FILE";
			logger.info("******************** " + currentStep);
			try {
				appProps.load(new FileInputStream(applicationProps));
			} catch (Exception esdf) {
				retMesg = "Error reading Application Configuration Property file";
				logger.error(retMesg);
				okToProceed = Boolean.valueOf(false);
			}
			Object[] fieldConfPropRet = null;
			fieldConfPropRet = utils.loadFldConfProps();
			fldConfProps = (Properties)fieldConfPropRet[0];
			okToProceed = (Boolean)fieldConfPropRet[1];
			if (!okToProceed.booleanValue()) {
				retMesg = "Error reading Field Configuration Property file";
				logger.error(retMesg);
			}
			if (okToProceed.booleanValue()) {
				currentStep = "GET PROPERTY VALUES";
				logger.info("******************** " + currentStep);
				insertRecord = appProps.getProperty("insertRecord", "NOT_FOUND").trim();
				if (insertRecord.equalsIgnoreCase("NOT_FOUND")) {
					logger.info("Parameter insertRecord is not configured");
					okToProceed = Boolean.valueOf(false);
				} else {
					logger.info("Parameter insertRecord is configured to " + insertRecord);
				}
				updateRecord = appProps.getProperty("updateRecord", "NOT_FOUND").trim();
				if (updateRecord.equalsIgnoreCase("NOT_FOUND")) {
					logger.info("Parameter updateRecord is not configured");
					okToProceed = Boolean.valueOf(false);
				} else {
					logger.info("Parameter updateRecord is configured to " + updateRecord);
				}
				checkMandatoryField = appProps.getProperty("checkMandatoryField", "NOT_FOUND").trim();
				if (checkMandatoryField.equalsIgnoreCase("NOT_FOUND")) {
					logger.info("Parameter checkMandatoryField is not configured");
					okToProceed = Boolean.valueOf(false);
				} else {
					logger.info("Parameter checkMandatoryField is configured to " + checkMandatoryField);
				}
				getSheetNum = fldConfProps.getProperty("getSheetNum", "NOT_FOUND");
				if (getSheetNum.equalsIgnoreCase("NOT_FOUND"))
					okToProceed = Boolean.valueOf(false);
				firstRowDatax = fldConfProps.getProperty("firstRowData", "NOT_FOUND");
				if (!firstRowDatax.equalsIgnoreCase("NOT_FOUND"))
					firstRowData = Integer.parseInt(firstRowDatax) - 1;
				logger.info("Parameter firstRowData is configured to " + firstRowData);
				createDiscardedFile = appProps.getProperty("createDiscardedFile", "NOT_FOUND").trim();
				if (createDiscardedFile.equalsIgnoreCase("NOT_FOUND")) {
					logger.info("Parameter createDiscardedFile is not configured. Record Discarded will not be saved.");
					okToProceed = Boolean.valueOf(false);
				} else {
					logger.info("Parameter createDiscardedFile is configured to " + createDiscardedFile);
				}
				if (!okToProceed.booleanValue()) {
					retMesg = "Missing information in the Configuration File.";
					logger.error(retMesg);
				}
			}
			if (okToProceed.booleanValue()) {
				InputStream InputStream = new FileInputStream(new File(xlsFileName));
				XSSFWorkbook workBook = new XSSFWorkbook(InputStream);
				sheet = null;
				sheet = workBook.getSheetAt(Integer.parseInt(getSheetNum) - 1);
				currentWorkBook = workBook.getSheetName(Integer.parseInt(getSheetNum) - 1).toUpperCase().trim();
				logger.info("Current Workbook is " + currentWorkBook + " - Class requested " + pdataFlow);
			}
			if (okToProceed.booleanValue()) {
				headRow = sheet.getRow(0);
				commentRow = sheet.getRow(1);
				NoC = headRow.getLastCellNum();
				totalNumOfRows = sheet.getPhysicalNumberOfRows();
				logger.info("Total Number Of Cells: " + NoC + " - Total Number Of Rows: " + totalNumOfRows);
			}
			if (okToProceed.booleanValue() &&
					pdataFlow.equalsIgnoreCase("CMDB")) {
				currentStep = "VALIDATE CMDB DEVICE TYPE FIELD POSITION";
				logger.info("******************** " + currentStep);
				driverFieldPosition = utils.findPos(headRow, driverFieldName, NoC);
				if (driverFieldPosition != -1) {
					logger.info("Device Type Field Configuration " + driverFieldName + " found at position " + driverFieldPosition);
				} else {
					retMesg = "Device Type Field Configuration " + driverFieldName + " not found in the Source Data File";
					logger.error(retMesg);
					okToProceed = Boolean.valueOf(false);
				}
			}
			if (okToProceed.booleanValue())
				if (createDiscardedFile.equalsIgnoreCase("Yes")) {
					wbDiscarded = new XSSFWorkbook();
					sheetDiscarded = wbDiscarded.createSheet("DISCARDED");
					utils.writeRowDiscarded(
							sheetDiscarded,
							headRow,
							0,
							NoC);
					utils.writeRowDiscarded(
							sheetDiscarded,
							commentRow,
							1,
							NoC);
				}
			if (okToProceed.booleanValue()) {
				int totalRowsElab = 0;
				int rowsInserted = 0;
				int rowsUpdated = 0;
				int rowDiscarded = 2;
				String recordToDiscard = "YES";
				int krowSrc = 0;
				for (int krow = firstRowData; krow < totalNumOfRows; krow++) {
					try {
						krowSrc = krow + 1;
						logger.info("######################## Start Processing Row " + krowSrc + " ########################");
						long startTime = System.currentTimeMillis();
						rowData = sheet.getRow(krow);
						recordToDiscard = "YES";
						okToProceed = Boolean.valueOf(true);
						retCode = "0";
						retMesg = "";
						retRowSysID = "";
						if (!utils.checkRowHasData(rowData)) {
							retCode = "9001";
							retMesg = "The Current Row is Empty. The Row is Skipped (is not an Error).";
							logger.warn(retMesg);
							okToProceed = Boolean.valueOf(false);
						}
						if (okToProceed.booleanValue()) {
							recCountOut++;
							totalRowsElab++;
							currentStep = "PROCESSING ROW FOR " + pdataFlow;
							logger.info("################################ " + currentStep + " ############################################");
							String fieldListMandx = "";
							String fieldListAttrx = "";
							String fieldListGrpx = "";
							String fieldListExtRefx = "";
							String fieldKeyTabx = "";
							String sheetContext = "";
							String fieldDsName = "";
							String fieldDsValue = "";
							retCode = "0";
							retMesg = "";
							retRowSysID = "";
							if (okToProceed.booleanValue()) {
								retCode = "9003";
								String str;
								switch ((str = pdataFlow).hashCode()) {
									case 2072168:
										if (str.equals("CMDB")) {
											currentStep = "GET PROPERTIES FOR " + pdataFlow;
											logger.info("******************** " + currentStep);
											destTableName = "";
											fieldListMand = null;
											fieldListPKey = null;
											fieldListAttr = null;
											fieldListGroup = null;
											fieldListExtRef = null;
											driverFieldValue =
													utils.getCellVal(driverFieldName, rowData.getCell((short)driverFieldPosition))
															.replaceAll("[^A-Za-z0-9]", "_").toUpperCase().trim();
											if (driverFieldValue == null ||
													driverFieldValue == "" ||
													driverFieldValue.toString().isEmpty()) {
												logger.warn("Device Type not configured for this row.");
												driverFieldValue = "NOT_CONFIGURED";
											}
											logger.info("Device Type " + driverFieldValue);
											sheetContext = fldConfProps.getProperty("sheet.context." + driverFieldValue, "NOT_FOUND").trim();
											if (sheetContext.equalsIgnoreCase("NOT_FOUND"))
												sheetContext = "Default";
											logger.info("Device Type for the parameters: " + sheetContext);
											destTableName = fldConfProps.getProperty("cmdb.tablename." + sheetContext, "NOT_FOUND").trim();
											if (destTableName.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Destination Table Name not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												logger.info("Destination Table Name parameter: " + destTableName);
											}
											if (checkMandatoryField.equalsIgnoreCase("Yes")) {
												fieldListMandx = fldConfProps.getProperty("cmdb.fieldmandatory." + sheetContext, "NOT_FOUND").trim();
												if (fieldListMandx.equalsIgnoreCase("NOT_FOUND")) {
													retMesg = "Mandatory Field List parameter not configured";
													logger.error(retMesg);
													okToProceed = Boolean.valueOf(false);
												} else {
													fieldListMand = fldConfProps.getProperty("cmdb.fieldmandatory." + sheetContext, "NOT_FOUND").trim().split(",");
													logger.info("Mandatory Field List parameter (" + fieldListMand.length + ") " + fieldListMandx);
												}
											}
											fieldListAttrx = fldConfProps.getProperty("cmdb.fieldAttr." + sheetContext, "NOT_FOUND").trim();
											if (fieldListAttrx.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Attribute Field List parameter not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												fieldListAttr = fldConfProps.getProperty("cmdb.fieldAttr." + sheetContext, "NOT_FOUND").trim().split(",");
												logger.info("Attribute Field List parameter (" + fieldListAttr.length + ") " + fieldListAttrx);
											}
											fieldListGrpx = fldConfProps.getProperty("cmdb.fieldGroup." + sheetContext, "NOT_FOUND").trim();
											if (fieldListGrpx.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Group Field List parameter not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												fieldListGroup = fldConfProps.getProperty("cmdb.fieldGroup." + sheetContext, "NOT_FOUND").trim().split(",");
												logger.info("Group Field List parameter (" + fieldListGroup.length + ") " + fieldListGrpx);
											}
											fieldListExtRefx = fldConfProps.getProperty("cmdb.externalref." + sheetContext, "NOT_FOUND").trim();
											if (fieldListExtRefx.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "External Reference Field List not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												fieldListExtRef = fldConfProps.getProperty("cmdb.externalref." + sheetContext, "NOT_FOUND").trim().split(",");
												logger.info("External Reference Field List parameter (" + fieldListExtRef.length + ") " + fieldListExtRefx);
											}
											fieldKeyTabx = fldConfProps.getProperty("cmdb.keytab." + sheetContext, "NOT_FOUND").trim();
											if (fieldKeyTabx.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Key Field List parameter not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												fieldListPKey = fldConfProps.getProperty("cmdb.keytab." + sheetContext, "NOT_FOUND").trim().split(",");
												logger.info("Key Field List parameter (" + fieldListPKey.length + ") " + fieldKeyTabx);
											}
											fieldDsName = fldConfProps.getProperty("cmdb.datasource.name", "NOT_FOUND").trim();
											if (fieldDsName.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Datasource name parameter not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												logger.info("Datasource name parameter : " + fieldDsName);
											}
											fieldDsValue = fldConfProps.getProperty("cmdb.datasource.value", "NOT_FOUND").trim();
											if (fieldDsValue.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Datasource value parameter not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												logger.info("Datasource value parameter : " + fieldDsValue);
											}
											if (okToProceed.booleanValue())
												logger.info("[" + currentStep + "] Properties for CMDB are OK.");
											break;
										}
									case 2545479:
										if (str.equals("SITE")) {
											currentStep = "GET PROPERTIES FOR " + pdataFlow;
											logger.info("******************** " + currentStep);
											destTableName = "";
											fieldListMand = null;
											fieldListPKey = null;
											fieldListAttr = null;
											fieldListGroup = null;
											fieldListExtRef = null;
											destTableName = fldConfProps.getProperty("site.tablename", "NOT_FOUND").trim();
											if (destTableName.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Destination Table Name not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												logger.info("Destination Table Name parameter: " + destTableName);
											}
											if (checkMandatoryField.equalsIgnoreCase("Yes")) {
												fieldListMandx = fldConfProps.getProperty("site.fieldmandatory", "NOT_FOUND").trim();
												if (fieldListMandx.equalsIgnoreCase("NOT_FOUND")) {
													retMesg = "Mandatory Field List parameter not configured";
													logger.error(retMesg);
													okToProceed = Boolean.valueOf(false);
												} else {
													logger.info("Mandatory Field List parameter: " + fieldListMandx);
													fieldListMand = fldConfProps.getProperty("site.fieldmandatory", "NOT_FOUND").trim().split(",");
												}
											}
											fieldListAttrx = fldConfProps.getProperty("site.fieldAttr", "NOT_FOUND").trim();
											if (fieldListAttrx.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Attribute Field List parameter not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												logger.info("Attribute Field List parameter: " + fieldListAttrx);
												fieldListAttr = fldConfProps.getProperty("site.fieldAttr", "NOT_FOUND").trim().split(",");
											}
											fieldListGrpx = fldConfProps.getProperty("site.fieldGroup", "NOT_FOUND").trim();
											if (fieldListGrpx.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Group Field List parameter not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												logger.info("Group Field List parameter: " + fieldListGrpx);
												fieldListGroup = fldConfProps.getProperty("site.fieldGroup", "NOT_FOUND").trim().split(",");
											}
											fieldKeyTabx = fldConfProps.getProperty("site.keytab", "NOT_FOUND").trim();
											if (fieldKeyTabx.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Key Field List not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												logger.info("Key Field List parameter: " + fieldKeyTabx);
												fieldListPKey = fldConfProps.getProperty("site.keytab", "NOT_FOUND").trim().split(",");
											}
											fieldDsName = fldConfProps.getProperty("site.datasource.name", "NOT_FOUND").trim();
											if (fieldDsName.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Datasource name parameter not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												logger.info("Datasource name parameter : " + fieldDsName);
											}
											fieldDsValue = fldConfProps.getProperty("site.datasource.value", "NOT_FOUND").trim();
											if (fieldDsValue.equalsIgnoreCase("NOT_FOUND")) {
												retMesg = "Datasource value parameter not configured";
												logger.error(retMesg);
												okToProceed = Boolean.valueOf(false);
											} else {
												logger.info("Datasource value parameter : " + fieldDsValue);
											}
											if (okToProceed.booleanValue())
												logger.info("[" + currentStep + "] Properties for Site are OK.");
											break;
										}
									default:
										retMesg = "Current Sheet not configured (admitted values are SITE or CMDB)";
										logger.error(retMesg);
										okToProceed = Boolean.valueOf(false);
										break;
								}
							}
							if (okToProceed.booleanValue()) {
								currentStep = "VALIDATE FIELD POSITION";
								logger.info("******************** " + currentStep + " ********************");
								Object[] validateFileInputAttr = null;
								validateFileInputAttr = validateInputFile.doValidate(headRow, rowData, fieldListAttr, NoC);
								okToProceed = (Boolean)validateFileInputAttr[0];
								retMesg = (String)validateFileInputAttr[1];
								if (okToProceed.booleanValue()) {
									logger.info("[" + currentStep + "] Attribute Fields Validation is OK.");
								} else {
									retCode = "9011";
									logger.error(retMesg);
								}
							}
							if (okToProceed.booleanValue()) {
								currentStep = "VALIDATE GROUP FIELD POSITION";
								logger.info("******************** " + currentStep + " ********************");
								Object[] validateFileInputFG = null;
								validateFileInputFG = validateInputFile.doValidate(headRow, rowData, fieldListGroup, NoC);
								okToProceed = (Boolean)validateFileInputFG[0];
								retMesg = (String)validateFileInputFG[1];
								if (okToProceed.booleanValue()) {
									logger.info("[" + currentStep + "] Group Fields Validation is OK.");
								} else {
									retCode = "9012";
									logger.error(retMesg);
								}
							}
							if (okToProceed.booleanValue())
								if (checkMandatoryField.equalsIgnoreCase("Yes")) {
									currentStep = "VALIDATE MANDATORY FIELDS";
									logger.info("******************** " + currentStep + " ********************");
									for (int flMandPos = 0; flMandPos < fieldListMand.length; flMandPos++) {
										String theMandatoryField =
												utils.getCellVal(fieldListMand[flMandPos], rowData.getCell(utils.findPos(headRow, fieldListMand[flMandPos], NoC))).trim();
										if (theMandatoryField == null ||
												theMandatoryField == "" ||
												theMandatoryField.toString().isEmpty() ||
												theMandatoryField == fieldMandatoryDefaultNotPermitted) {
											retCode = "9013";
											retMesg = "Mandatory field " + fieldListMand[flMandPos] + " is null or has value Not Available (N.A.). The record is discarded.";
											okToProceed = Boolean.valueOf(false);
											break;
										}
									}
									if (okToProceed.booleanValue()) {
										logger.info("[" + currentStep + "] Mandatory Fields Validation is OK.");
									} else {
										logger.error(retMesg);
									}
								}
							if (okToProceed.booleanValue()) {
								currentStep = "VALIDATE KEY FIELDS";
								logger.info("******************** " + currentStep + " ********************");
								Object[] validateFileInputKey = null;
								validateFileInputKey = validateInputFile.doValidate(headRow, rowData, fieldListPKey, NoC);
								okToProceed = (Boolean)validateFileInputKey[0];
								retMesg = (String)validateFileInputKey[1];
								if (okToProceed.booleanValue()) {
									logger.info("[" + currentStep + "] Key Fields Validation is OK.");
								} else {
									retCode = "9015";
									logger.error(retMesg);
								}
							}
							if (okToProceed.booleanValue()) {
								currentStep = "VALIDATE KEY FIELDS";
								logger.info("******************** " + currentStep + " ********************");
								getKeyTabString = "";
								postStringKT = "";
								for (int flKPos = 0; flKPos < fieldListPKey.length; flKPos++) {
									String theValueKT = "";
									String externalRefResultretMesg = "";
									String externalRefKT = fldConfProps.getProperty("field.externalref." + fieldListPKey[flKPos], "NOT_FOUND").trim();
									if (!externalRefKT.equalsIgnoreCase("NOT_FOUND")) {
										logger.info("[" + currentStep + "] Key Field " + fieldListPKey[flKPos] + " is a External Field Reference");
										Object[] externalRefResult = null;
										externalRefResult = utils.getSysIDReference(
												httpClient, proxyhost, proxyport,
												fieldListPKey[flKPos],
												rowData,
												headRow,
												NoC);
										okToProceed = (Boolean)externalRefResult[0];
										theValueKT = externalRefResult[1].toString().trim();
										externalRefResultretMesg = externalRefResult[2].toString().trim();
									} else {
										String picklistFilterFieldREFx = fldConfProps.getProperty("field.picklist." + fieldListPKey[flKPos], "NOT_FOUND").trim();
										if (!picklistFilterFieldREFx.equalsIgnoreCase("NOT_FOUND")) {
											String picklistFilterFieldREF = fldConfProps.getProperty("field.picklist." + fieldListPKey[flKPos], "NOT_FOUND").split("\\|")[1].trim();
											logger.info("[" + currentStep + "] Key Field " + fieldListPKey[flKPos] + " is a Reference (" + picklistFilterFieldREF + ")");
											Object[] picklistFilterResultKeyT = null;
											picklistFilterResultKeyT = utils.getPLArray(picklistFilterFieldREF, fieldListPKey[flKPos].trim());
											okToProceed = (Boolean)picklistFilterResultKeyT[0];
											theValueKT = picklistFilterResultKeyT[1].toString().trim();
											String str1 = picklistFilterResultKeyT[2].toString().trim();
										} else {
											logger.info("[" + currentStep + "] Key Field " + fieldListPKey[flKPos] + " is an Attribute");
											try {
												theValueKT = utils.getCellVal(fieldListPKey[flKPos], rowData.getCell(utils.findPos(headRow, fieldListPKey[flKPos], NoC))).trim();
											} catch (Exception e) {
												retCode = "9016";
												retMesg = "Error getting the Field Value for field attribute " + fieldListPKey[flKPos];
												logger.error(retMesg, e);
												okToProceed = Boolean.valueOf(false);
											}
										}
									}
									if (okToProceed.booleanValue() && theValueKT.length() > 0) {
										getKeyTabString = String.valueOf(getKeyTabString) +
												fieldListPKey[flKPos].split(":")[0].trim() +
												utils.SysParmsEqual() +
												URLEncoder.encode(theValueKT, "UTF-8") +
												utils.SysParmsAnd();
										postStringKT = String.valueOf(postStringKT) + "\"" +
												fieldListPKey[flKPos].split(":")[0].trim() + "\" : \"" +
												theValueKT + "\",";
									} else {
										retCode = "9017";
										retMesg = "Key field " + fieldListPKey[flKPos].split(":")[0].trim() + " is null or is not a valid value. The record is discarded [" + externalRefResultretMesg + "]";
										okToProceed = Boolean.valueOf(false);
										break;
									}
								}
								if (okToProceed.booleanValue()) {
									logger.info("[" + currentStep + "] Key Fields Validation is OK.");
									getKeyTabString = getKeyTabString.substring(0, getKeyTabString.length() - utils.SysParmsAnd().length());
								} else {
									logger.error(retMesg);
								}
							}
							postString = "";
							if (postStringKT.length() > 0)
								postString = String.valueOf(postString) + postStringKT;
							String fieldListValue = "";
							if (okToProceed.booleanValue()) {
								currentStep = "PROCESSING ATTR./REF. FIELDS";
								logger.info("******************** " + currentStep + " ********************");
								for (int flPos = 0; flPos < fieldListAttr.length; flPos++) {
									logger.info("***** [" + currentStep + "] STARTING WITH Field: " + fieldListAttr[flPos]);
									String fieldListValueT = "";
									fieldListValue = "";
									try {
										fieldListValueT = utils.getCellVal(fieldListAttr[flPos], rowData.getCell(utils.findPos(headRow, fieldListAttr[flPos], NoC))).trim();
									} catch (Exception e) {
										retCode = "9019";
										retMesg = "[" + currentStep + "] Error getting the Field Value for " + fieldListAttr[flPos];
										okToProceed = Boolean.valueOf(false);
										logger.error(retMesg, e);
									}
									if (okToProceed.booleanValue()) {
										String picklistFilterFieldREFx = fldConfProps.getProperty("field.picklist." + fieldListAttr[flPos], "NOT_FOUND").trim();
										if (!picklistFilterFieldREFx.equalsIgnoreCase("NOT_FOUND")) {
											String picklistFilterFieldREF = fldConfProps.getProperty("field.picklist." + fieldListAttr[flPos], "NOT_FOUND").split("\\|")[1].trim();
											try {
												String[] fieldListValueTMVal =

														utils.setFieldListMultipleValuesSeparator(
																fieldListAttr[flPos], fieldListValueT);
												for (int flPosMVal = 0; flPosMVal < fieldListValueTMVal.length; flPosMVal++) {
													Object[] ValidateDataTypeObjc = null;
													ValidateDataTypeObjc = validateInputFile.ValidateDataType(
															fieldListAttr[flPos], fieldListValueTMVal[flPosMVal]);
													okToProceed = (Boolean)ValidateDataTypeObjc[0];
													String fieldListValueVDTT = ValidateDataTypeObjc[1].toString().trim();
													retMesg = ValidateDataTypeObjc[2].toString().trim();
													if (!okToProceed.booleanValue())
														retCode = "9021";
													if (okToProceed.booleanValue()) {
														logger.info("[" + currentStep + "] Field " + fieldListAttr[flPos] + " is a Reference (" + picklistFilterFieldREF + ")");
														Object[] picklistFilterResultMVal = null;
														picklistFilterResultMVal = utils.getPLArray(picklistFilterFieldREF, fieldListValueVDTT.trim());
														okToProceed = (Boolean)picklistFilterResultMVal[0];
														String picklistFilterResultMValSysId = picklistFilterResultMVal[1].toString().trim();
														retMesg = picklistFilterResultMVal[2].toString().trim();
														if (!okToProceed.booleanValue())
															retCode = "9020";
														if (okToProceed.booleanValue() &&
																picklistFilterResultMValSysId.length() > 0)
															fieldListValue = String.valueOf(fieldListValue) + picklistFilterResultMValSysId + ",";
													}
												}
											} catch (Exception ab) {
												retMesg = "Reference Field multivalue " + fieldListAttr[flPos] + " return a null value.";
												retCode = "7021";
												okToProceed = Boolean.valueOf(false);
												logger.error(retMesg, ab);
											}
											if (okToProceed.booleanValue() &&
													fieldListValue.length() > 0)
												fieldListValue = fieldListValue.substring(0, fieldListValue.length() - 1);
										} else {
											fieldListValue = fieldListValueT;
											if (fieldListValue != null && fieldListValue != "" && !fieldListValue.isEmpty()) {
												logger.info("[" + currentStep + "] Attribute Field " + fieldListAttr[flPos] + " has value");
												Object[] ValidateDataTypeObja = null;
												ValidateDataTypeObja = validateInputFile.ValidateDataType(
														fieldListAttr[flPos], fieldListValue);
												okToProceed = (Boolean)ValidateDataTypeObja[0];
												fieldListValue = ValidateDataTypeObja[1].toString().trim();
												retMesg = ValidateDataTypeObja[2].toString().trim();
												if (!okToProceed.booleanValue())
													retCode = "9022";
											} else {
												logger.info("[" + currentStep + "] Attribute Field " + fieldListAttr[flPos] + " is empty");
											}
										}
										if (okToProceed.booleanValue() &&
												fieldListValue != null && fieldListValue != "" && !fieldListValue.isEmpty())
											postString = String.valueOf(postString) + "\"" +
													fieldListAttr[flPos].split(":")[0].trim() + "\" : \"" +
													fieldListValue.replaceAll("\"", "\\\\\"").trim() +
													"\",";
									}
									if (!okToProceed.booleanValue())
										break;
								}
							}
							if (okToProceed.booleanValue() &&
									pdataFlow.equalsIgnoreCase("CMDB")) {
								currentStep = "PROCESSING EXTERNAL REFERENCE FIELD";
								logger.info("******************** " + currentStep + " ********************");
								for (int flPos = 0; flPos < fieldListExtRef.length; flPos++) {
									logger.info("***** [" + currentStep + "] STARTING WITH External Reference Field : " + fieldListExtRef[flPos]);
									Object[] externalRefResult = null;
									externalRefResult = utils.getSysIDReference(
											httpClient, proxyhost, proxyport,
											fieldListExtRef[flPos],
											rowData,
											headRow,
											NoC);
									okToProceed = (Boolean)externalRefResult[0];
									String externalRefResultSysid = externalRefResult[1].toString().trim();
									retMesg = externalRefResult[2].toString().trim();
									if (okToProceed.booleanValue())
										if (externalRefResultSysid.length() > 0) {
											logger.info("[" + currentStep + "] External Reference Field result OK");
											postString = String.valueOf(postString) + "\"" +
													fieldListExtRef[flPos].split(":")[0].trim() + "\" : \"" +
													externalRefResultSysid.trim() + "\",";
										} else {
											logger.info("[" + currentStep + "] External Reference Field result - Sys ID is empty");
										}
									if (!okToProceed.booleanValue()) {
										retCode = "9930";
										break;
									}
								}
							}
							if (!okToProceed.booleanValue())
								logger.error(retMesg);
							int exitStatusCodeGetd = 999;
							String errorTextGetd = "";
							if (okToProceed.booleanValue()) {
								currentStep = "GET ACTION FOR CREATE/UPDATE RECORD";
								logger.info("++++++++++++++++++++ " + currentStep + " ++++++++++++++++++++");
								logger.debug("[" + currentStep + "] Get Action Request for Key Fields: " + getKeyTabString);
								logger.debug("[" + currentStep + "] Post Action Request : " + postString);
								Object[] fieldValueRetKT = null;
								GetAction getActionKT = new GetAction();
								fieldValueRetKT = getActionKT.theRequest(httpClient, proxyhost, proxyport,
										destTableName, getKeyTabString, "sys_id", "Yes");
								exitStatusCodeGetd = ((Integer)fieldValueRetKT[0]).intValue();
								retRowSysID = fieldValueRetKT[1].toString().trim();
								okToProceed = (Boolean)fieldValueRetKT[2];
								errorTextGetd = (String)fieldValueRetKT[3];
								logger.info("[" + currentStep + "] Return Value from Get Action: " +
										"the Sys ID " + retRowSysID + " - Status :" + exitStatusCodeGetd);
								if (okToProceed.booleanValue() && exitStatusCodeGetd < 300) {
									if (retRowSysID.length() == 0) {
										int exitStatusCodePost = 999;
										if (insertRecord.equalsIgnoreCase("Yes")) {
											currentStep = "CREATE RECORD";
											logger.info("++++++++++++++++++++ " + currentStep + " ++++++++++++++++++++");
											postString = String.valueOf(postString) + "\"" +
													fieldDsName.trim() + "\" : \"" +
													fieldDsValue.trim() + "\",";
											postString = "{" + postString.substring(0, postString.length() - 1) + "}";
											Object[] fieldValueRetPost = null;
											PostAction postAction = new PostAction();
											fieldValueRetPost = postAction.theRequest(httpClient, proxyhost, proxyport,
													destTableName, postString, "sys_id");
											exitStatusCodePost = ((Integer)fieldValueRetPost[0]).intValue();
											retRowSysID = fieldValueRetPost[1].toString().trim();
											okToProceed = (Boolean)fieldValueRetPost[2];
											retMesg = (String)fieldValueRetPost[3];
											logger.info("[" + currentStep + "] Return Value from Post Action: " +
													"Return Code " + exitStatusCodePost);
											if (okToProceed.booleanValue() && exitStatusCodePost < 300) {
												logger.info("[" + currentStep + "] Sys ID Created: " + retRowSysID);
												Object[] postGroupTableRet = null;
												String postGroupTableRetCode = "";
												String postGroupTableRetMesg = "";
												postGroupTableRet = FieldGroupConf.postGroupTable(
														"INSERT",
														httpClient, proxyhost, proxyport,
														pdataFlow,
														fldConfProps,
														fieldListGroup,
														rowData,
														headRow,
														NoC,
														retRowSysID);
												postGroupTableRetCode = postGroupTableRet[0].toString().trim();
												postGroupTableRetMesg = postGroupTableRet[1].toString().trim();
												okToProceed = (Boolean)postGroupTableRet[2];
												logger.debug("post Group Table: " +
														postGroupTableRetCode + "-" + postGroupTableRetMesg + "-" + okToProceed);
												retCode = "9091-" + postGroupTableRetCode;
												retMesg = "CMDB CI Created - " + postGroupTableRetMesg;
												logger.info("++++++++++++++++++++ " + retMesg + " ++++++++++++++++++++");
												rowsInserted++;
												if (okToProceed.booleanValue())
													recordToDiscard = "NO";
											} else {
												logger.error("++++++++++++++++++++ " + retMesg + " ++++++++++++++++++++");
												okToProceed = Boolean.valueOf(false);
											}
										} else {
											retCode = "8002";
											retMesg = "Insert Option is Disabled. Record is not inserted.";
											logger.warn(retMesg);
										}
									} else {
										int exitStatusCodePut = 999;
										if (updateRecord.equalsIgnoreCase("Yes")) {
											currentStep = "UPDATE RECORD";
											logger.info("++++++++++++++++++++ " + currentStep + " ++++++++++++++++++++");
											postString = "{" + postString.substring(0, postString.length() - 1) + "}";
											Object[] fieldValueRetPut = null;
											PutAction putAction = new PutAction();
											fieldValueRetPut = putAction.theRequest(httpClient, proxyhost, proxyport,
													destTableName, postString, retRowSysID, "sys_id");
											exitStatusCodePut = ((Integer)fieldValueRetPut[0]).intValue();
											retRowSysID = fieldValueRetPut[1].toString().trim();
											okToProceed = (Boolean)fieldValueRetPut[2];
											retMesg = (String)fieldValueRetPut[3];
											logger.info("[" + currentStep + "] Return Value from Put Action: " +
													"Return Code " + exitStatusCodePut);
											if (okToProceed.booleanValue() && exitStatusCodePut < 300) {
												Object[] postGroupTableRet = null;
												String postGroupTableRetCode = "";
												String postGroupTableRetMesg = "";
												postGroupTableRet = FieldGroupConf.postGroupTable(
														"UPDATE",
														httpClient, proxyhost, proxyport,
														pdataFlow,
														fldConfProps,
														fieldListGroup,
														rowData,
														headRow,
														NoC,
														retRowSysID);
												postGroupTableRetCode = postGroupTableRet[0].toString().trim();
												postGroupTableRetMesg = postGroupTableRet[1].toString().trim();
												okToProceed = (Boolean)postGroupTableRet[2];
												logger.debug("post Group Table: " +
														postGroupTableRetCode + "-" + postGroupTableRetMesg + "-" + okToProceed);
												retCode = "9092-" + postGroupTableRetCode;
												retMesg = "CMDB CI Updated - " + postGroupTableRetMesg;
												logger.info("++++++++++++++++++++ " + retMesg + " ++++++++++++++++++++");
												logger.info("[" + currentStep + "] Sys ID Updated: " + retRowSysID);
												rowsUpdated++;
												if (okToProceed.booleanValue())
													recordToDiscard = "NO";
											} else {
												retMesg = "Error during Record Update Operation : " + retCode;
												logger.error("++++++++++++++++++++ " + retMesg + " ++++++++++++++++++++");
												okToProceed = Boolean.valueOf(false);
											}
										} else {
											retCode = "8003";
											retMesg = "Update Option is Disabled. Record is not updated.";
											logger.warn(retMesg);
										}
									}
								} else {
									retCode = "9997";
									retMesg = "Error during the Get Action before Post record (" + errorTextGetd + "). The record is discarded.";
									logger.error(retMesg);
									okToProceed = Boolean.valueOf(false);
								}
							}
							if (okToProceed.booleanValue())
								logger.info("++++++++++++++++++++ Action Executed ++++++++++++++++++++");
							if (!okToProceed.booleanValue())
								logger.error("++++++++++++++++++++ Action Executed with errors ++++++++++++++++++++");
							if (createDiscardedFile.equalsIgnoreCase("Yes") &&
									recordToDiscard.equalsIgnoreCase("YES")) {
								logger.info("##### RECORD DISCARDED " + krowSrc);
								utils.writeRowDiscarded(
										sheetDiscarded,
										rowData,
										rowDiscarded,
										NoC);
								rowDiscarded++;
							}
							logger.info("##### RETURN CODE FOR ROW " + krowSrc + " : " +
									currentStep + "," +
									retCode + "," +
									destTableName + "," +
									retRowSysID + "," +
									retMesg);
							utils.writeRowOut(sheetOut, krow, currentStep, retCode, destTableName, retRowSysID, retMesg);
						}
						long endTime = System.currentTimeMillis();
						logger.info("##################### End Processing Row " + krowSrc + " (Time ms: " + (endTime - startTime) + ") #####################");
					} catch (Exception erow) {
						if (createDiscardedFile.equalsIgnoreCase("Yes") &&
								recordToDiscard.equalsIgnoreCase("YES")) {
							logger.info("##### RECORD DISCARDED " + krowSrc);
							utils.writeRowDiscarded(
									sheetDiscarded,
									rowData,
									rowDiscarded,
									NoC);
							rowDiscarded++;
						}
						logger.info("##### RETURN CODE FOR ROW " + krowSrc + " : " +
								currentStep + "," +
								retCode + "," +
								destTableName + "," +
								retRowSysID + "," +
								retMesg);
						utils.writeRowOut(sheetOut, krow, currentStep, retCode, destTableName, retRowSysID, retMesg);
						logger.error("Got Exception in row " + krow, erow);
					}
				}
				logger.info("##### END RESULT - Rows Processed (excluded blank rows) : " + totalRowsElab);
				logger.info("##### END RESULT - OK : Rows Inserted: " + rowsInserted + " - Rows Updated: " + rowsUpdated);
			}
			try {
				logger.debug("Closing output file ...");
				FileOutputStream outputStream = new FileOutputStream(xlsOutput);
				workbookOut.write(outputStream);
				outputStream.close();
			} catch (Exception ecf) {
				logger.error("Closing output file Exception", ecf);
			}
			if (createDiscardedFile.equalsIgnoreCase("Yes"))
				try {
					logger.debug("Closing output file Discarded ...");
					FileOutputStream outputStreamDSC = new FileOutputStream(xlsDiscarded);
					wbDiscarded.write(outputStreamDSC);
					outputStreamDSC.close();
				} catch (Exception ecf) {
					logger.error("Closing output file Discarded Exception", ecf);
				}
		} catch (Exception e) {
			logger.error("Got Exception in the procedure : ", e);
			try {
				logger.debug("Closing output file ...");
				FileOutputStream outputStream = new FileOutputStream(xlsOutput);
				workbookOut.write(outputStream);
				outputStream.close();
			} catch (Exception ecf) {
				logger.error("Closing output file Exception", ecf);
			}
			if (createDiscardedFile.equalsIgnoreCase("Yes"))
				try {
					logger.debug("Closing output file Discarded ...");
					FileOutputStream outputStreamDSC = new FileOutputStream(xlsDiscarded);
					wbDiscarded.write(outputStreamDSC);
					outputStreamDSC.close();
				} catch (Exception ecf) {
					logger.error("Closing output file Discarded Exception", ecf);
				}
		}
	}

	public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, HttpException, IOException, JSONException {
		try {
			Utils utilsM = new Utils();
			Version version = new Version();
			String psp = System.getProperty("password");
			String dataFlow = System.getProperty("snclass").toUpperCase();
			String snenv = System.getProperty("snenv");
			logger.info("####################################################################");
			logger.info("####################### LOADDATA ###################################");
			logger.info("####################################################################");
			logger.info("Class requested for Import: " + dataFlow);
			String proxyhost = "";
			String proxyport = "";
			String username = "";
			if (okToProceed.booleanValue()) {
				try {
					connProps.load(new FileInputStream(connectionProps));
				} catch (Exception esdfcc) {
					logger.error("Error reading Application Configuration Property file", esdfcc);
				}
				proxyhost = connProps.getProperty(String.valueOf(snenv) + ".ProxyHost", "NOT_FOUND").trim();
				if (proxyhost.equalsIgnoreCase("NOT_FOUND"))
					okToProceed = Boolean.valueOf(false);
				proxyport = connProps.getProperty(String.valueOf(snenv) + ".ProxyPort", "NOT_FOUND").trim();
				if (proxyport.equalsIgnoreCase("NOT_FOUND"))
					okToProceed = Boolean.valueOf(false);
				username = connProps.getProperty(String.valueOf(snenv) + ".username", "NOT_FOUND").trim();
				if (username.equalsIgnoreCase("NOT_FOUND"))
					okToProceed = Boolean.valueOf(false);
				if (!okToProceed.booleanValue()) {
					retMesg = "Missing information in the Connection Property File (Proxy EE Server).";
					logger.error(retMesg);
				}
			}
			if (okToProceed.booleanValue()) {
				CloseableHttpClient httpClient = null;
				try {
					SSLContext sslContext = (new SSLContextBuilder())
							.loadTrustMaterial(null, (certificate, authType) -> true).build();
					BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
					UsernamePasswordCredentials credentials =
							new UsernamePasswordCredentials(username, psp);
					basicCredentialsProvider.setCredentials(AuthScope.ANY, (Credentials)credentials);
					logger.debug("Opening Connection to " + snenv + " ...");
					httpClient = HttpClientBuilder.create()
							.setSSLContext(sslContext)
							.setSSLHostnameVerifier((HostnameVerifier)new NoopHostnameVerifier())
							.setDefaultCredentialsProvider((CredentialsProvider)basicCredentialsProvider)
							.build();
					logger.debug("Testing Connection to " + proxyhost);
					int exitStatusCodeGetTestConn = 0;
					Object[] testConn = null;
					GetAction getActionTestConn = new GetAction();
					testConn = getActionTestConn.theRequest(
							httpClient, proxyhost, proxyport,
							"sys_user", "user_name=" + username, "sys_id", "Yes");
					exitStatusCodeGetTestConn = ((Integer)testConn[0]).intValue();
					if (exitStatusCodeGetTestConn < 300) {
						logger.info("Connection to " + snenv + " OK");
					} else {
						retMesg = "Connection ERROR - Exit STatus: " + exitStatusCodeGetTestConn;
						logger.error(retMesg);
						okToProceed = Boolean.valueOf(false);
					}
				} catch (Exception e) {
					retMesg = "Connection ERROR";
					logger.error(retMesg, e);
					okToProceed = Boolean.valueOf(false);
				}
				if (okToProceed.booleanValue()) {
					String inPath = System.getenv("DATAHOMEDIR").trim();
					String outPath = System.getenv("OUTDIR").trim();
					File folder = new File(inPath);
					File[] listOfFiles = folder.listFiles();
					for (int i = 0; i < listOfFiles.length; i++) {
						if (listOfFiles[i].isFile()) {
							SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMddHHmm");
							Date nowS = new Date();
							String FlowID = formatter1.format(nowS);
							logger.info("######### Flow ID " + FlowID);
							String currentFilenName = listOfFiles[i].getName();
							String currentOutResultFile = String.valueOf(FlowID) + "_Result_" + currentFilenName;
							String currentOutDiscardFile = String.valueOf(FlowID) + "_DISCARDED_" + currentFilenName;
							xlsInput = String.valueOf(inPath) + "\\" + currentFilenName;
							xlsOutput = String.valueOf(outPath) + "\\" + currentOutResultFile;
							xlsDiscarded = String.valueOf(outPath) + "\\" + currentOutDiscardFile;
							logger.info("Processing Input File " + xlsInput);
							logger.info("Output File Name: " + xlsOutput);
							SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
							String loadStartDate = formatter2.format(nowS);
							String ldPostString =
									"{\"u_load_start_date\" : \"" + loadStartDate +
											"\"," +
											"\"u_load_notes\":\"" +
											"Processing Data. " +
											" Flow ID " + FlowID +
											" - Data requested: " + dataFlow +
											" - Data File: " + currentFilenName +
											" - Output File Name: " + currentOutResultFile + "/" + currentOutDiscardFile +
											" [" + version.getVersion() + "]" +
											"\"}";
							PostAction postActionELD = new PostAction();
							Object[] fieldValueRetELD = null;
							fieldValueRetELD = postActionELD.theRequest(
									httpClient, proxyhost, proxyport,
									"u_enc_loaddata", ldPostString, "sys_id");
							String fieldValueRetELDOut = (String)fieldValueRetELD[1];
							try {
								String str;
								switch ((str = dataFlow).hashCode()) {
									case 2072168:
										if (str.equals("CMDB")) {
											utilsM.populatePLArray(httpClient, proxyhost, proxyport);
											LoadData loaddataCMDB = new LoadData();
											loaddataCMDB.convertFile(httpClient, proxyhost, proxyport,
													dataFlow, FlowID, xlsInput, xlsOutput);
											break;
										}
									case 2545479:
										if (str.equals("SITE")) {
											utilsM.populatePLArray(httpClient, proxyhost, proxyport);
											LoadData loaddataSITE = new LoadData();
											loaddataSITE.convertFile(httpClient, proxyhost, proxyport,
													dataFlow, FlowID, xlsInput, xlsOutput);
											break;
										}
									case 43919423:
										if (str.equals("PICKLIST")) {
											LoadDataPL loaddatapl = new LoadDataPL();
											loaddatapl.convertFilepl(httpClient, proxyhost, proxyport,
													dataFlow, FlowID, xlsInput, xlsOutput);
											break;
										}
									default:
										retMesg = "The class Name requested is not valid. values admitted are CMDB, SITE or PICKLIST";
										logger.error(retMesg);
										okToProceed = Boolean.valueOf(false);
										break;
								}
							} catch (Exception e) {
								retMesg = "Processing ERROR. Error starting the classes to import Data.";
								logger.error(retMesg, e);
								okToProceed = Boolean.valueOf(false);
							}
							Date nowE = new Date();
							String loadEndDate = formatter2.format(nowE);
							String ldPutString =
									"{\"u_load_end_date\" : \"" + loadEndDate +
											"\"," +
											"\"u_load_notes\":\"" +
											"Data Processed. " +
											" Flow ID " + FlowID +
											" - Data requested: " + dataFlow +
											" - Data File: " + currentFilenName +
											" - Output File Name: " + currentOutResultFile + "/" + currentOutDiscardFile +
											" [" + version.getVersion() + "]" +
											"\"}";
							PutAction putActionELDP = new PutAction();
							Object[] fieldValueRetELDP = null;
							fieldValueRetELDP = putActionELDP.theRequest(
									httpClient, proxyhost, proxyport,
									"u_enc_loaddata", ldPutString, fieldValueRetELDOut, "sys_id");
						}
					}
				}
				logger.debug("Closing Http Connection ...");
				httpClient.close();
			}
			logger.info("####################################################################");
			logger.info("########################## END LOADDATA ############################");
			logger.info("####################################################################");
			System.exit(0);
		} catch (Exception e) {
			logger.error("Main Exception ", e);
		}
	}
}
