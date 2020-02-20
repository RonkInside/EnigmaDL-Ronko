package com.nttdata.enel.EnigmaInt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
	protected static Logger logger = Logger.getLogger("com/nttdata/enel/EnigmaInt");

	static Properties fldConfProps = new Properties();

	static Properties fldConfPropsGC = new Properties();

	static String[][] thePicklistArrayQUR = new String[170000][3];

	public Object[] loadFldConfProps() {
		Properties fldConfPropsFn = new Properties();
		boolean okToProceedConfFn = true;
		Object[] thePropRet = { fldConfProps, Boolean.valueOf(okToProceedConfFn) };
		try {
			fldConfPropsFn.load(new FileInputStream(System.getenv("FIELDCONF")));
		} catch (Exception esdf) {
			logger.error("Error reading Field Configuration Property file", esdf);
		}
		thePropRet[0] = fldConfPropsFn;
		thePropRet[1] = Boolean.valueOf(okToProceedConfFn);
		return thePropRet;
	}

	public int findPos(XSSFRow theRow, String fieldNameToFind, int numberOfCells) {
		int FieldPosOnSheet = -1;
		for (int kLhead = 0; kLhead < numberOfCells; kLhead++) {
			XSSFCell cellkRow = theRow.getCell(kLhead);
			if (cellkRow != null &&
					cellkRow.getCellType() != 3 &&
					cellkRow.getCellType() != 5) {
				String FieldTextString = cellkRow.getStringCellValue().trim();
				if (FieldTextString.equalsIgnoreCase(fieldNameToFind)) {
					FieldPosOnSheet = kLhead;
					break;
				}
			}
		}
		return FieldPosOnSheet;
	}

	public String getCellVal(String fieldName, XSSFCell theCell) {
		String tempG = "";
		try {
			if (theCell != null &&
					theCell.getCellType() != 3 &&
					theCell.getCellType() != 5) {
				boolean tempB;
				Object[] fieldConfPropRetGC = null;
				fieldConfPropRetGC = loadFldConfProps();
				fldConfPropsGC = (Properties)fieldConfPropRetGC[0];
				String stringGC = fldConfPropsGC.getProperty("field.datatype.string." + fieldName, "NOT_FOUND").trim();
				String decimalGC = fldConfPropsGC.getProperty("field.datatype.decimal." + fieldName, "NOT_FOUND").trim();
				switch (theCell.getCellType()) {
					case 1:
						tempG = theCell.getStringCellValue().trim().replaceAll("\\r\\n|\\r|\\n", " ");
						if (!decimalGC.equalsIgnoreCase("NOT_FOUND"))
							tempG = tempG.replace(",", ".").trim();
						return tempG;
					case 0:
						if (DateUtil.isCellDateFormatted((Cell)theCell)) {
							Date tempD = theCell.getDateCellValue();
							SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
							tempG = simpleDateFormat.format(tempD);
						} else if (stringGC.equalsIgnoreCase("NOT_FOUND")) {
							tempG = Double.valueOf(theCell.getNumericCellValue()).toString().trim();
						} else {
							tempG = Long.valueOf((long)theCell.getNumericCellValue()).toString().trim();
						}
						return tempG;
					case 4:
						tempB = theCell.getBooleanCellValue();
						if (tempB) {
							tempG = "true";
						} else {
							tempG = "false";
						}
						return tempG;
				}
				tempG = theCell.getStringCellValue().trim().replaceAll("\\r\\n|\\r|\\n", " ");
			}
		} catch (Exception e) {
			logger.error("Get Cell Value - Exception ", e);
		}
		return tempG;
	}

	public Object[] getSysIDReference(CloseableHttpClient httpClient, String proxyhost, String proxyport, String extRef, XSSFRow rowData, XSSFRow headRow, int NoC) throws UnsupportedEncodingException {
		boolean okToProceedUtils = true;
		String fieldValueRefRetGSIR = "";
		String retMesgGSIR = "";
		Object[] theValueRet = { Boolean.valueOf(okToProceedUtils), fieldValueRefRetGSIR, retMesgGSIR };
		Object[] fieldConfPropRet = null;
		fieldConfPropRet = loadFldConfProps();
		fldConfProps = (Properties)fieldConfPropRet[0];
		okToProceedUtils = ((Boolean)fieldConfPropRet[1]).booleanValue();
		if (!okToProceedUtils) {
			retMesgGSIR = "Error reading Field Configuration Property file";
			logger.error(retMesgGSIR);
		}
		String getStringExtRef = "";
		String[] extRefArr = null;
		String[] extRefFromField = null;
		String extRefDestTable = "";
		String[] extRefDestTableField = null;
		String extRefVal = fldConfProps.getProperty("field.externalref." + extRef, "NOT_FOUND").trim();
		if (!extRefVal.equalsIgnoreCase("NOT_FOUND")) {
			logger.info("External Reference: " + extRefVal);
			extRefArr = extRefVal.split("\\|");
			extRefFromField = extRefArr[0].split(",");
			extRefDestTable = extRefArr[1];
			extRefDestTableField = extRefArr[2].split(",");
			for (int i_extRefDestTableField = 0; i_extRefDestTableField < extRefDestTableField.length; i_extRefDestTableField++) {
				String fieldListValueExtRef = "";
				try {
					fieldListValueExtRef =
							getCellVal(extRefFromField[i_extRefDestTableField], rowData.getCell(findPos(headRow, extRefFromField[i_extRefDestTableField], NoC))).trim();
				} catch (Exception ab) {
					logger.error("Group Field Get Cell Value return null.");
				}
				logger.debug("Get the Sys ID Reference - STARTING WITH ELEMENT " +
						extRefFromField[i_extRefDestTableField] + " -> " +
						extRefDestTableField[i_extRefDestTableField]);
				if (fieldListValueExtRef != null && fieldListValueExtRef != "" && !fieldListValueExtRef.isEmpty())
					getStringExtRef =
							String.valueOf(getStringExtRef) +
									extRefDestTableField[i_extRefDestTableField].split(":")[0].trim() +
									SysParmsEqual() +
									URLEncoder.encode(fieldListValueExtRef.trim(), "UTF-8") +
									SysParmsAnd();
			}
			if (getStringExtRef.length() > 0) {
				getStringExtRef =
						getStringExtRef.substring(0, getStringExtRef.length() - SysParmsAnd().length());
				String errorTextGetRefGSIR = "";
				try {
					Object[] fieldValueRefRetKT = null;
					GetAction getActionRef = new GetAction();
					fieldValueRefRetKT = getActionRef.theRequest(
							httpClient, proxyhost, proxyport,
							extRefDestTable, getStringExtRef, "sys_id", "Yes");
					int exitStatusCodeGetRefGSIR = ((Integer)fieldValueRefRetKT[0]).intValue();
					fieldValueRefRetGSIR = fieldValueRefRetKT[1].toString().trim();
					okToProceedUtils = ((Boolean)fieldValueRefRetKT[2]).booleanValue();
					errorTextGetRefGSIR = (String)fieldValueRefRetKT[3];
					if (okToProceedUtils) {
						if (exitStatusCodeGetRefGSIR < 300) {
							if (fieldValueRefRetGSIR == null ||
									fieldValueRefRetGSIR == "" ||
									fieldValueRefRetGSIR.toString().isEmpty()) {
								okToProceedUtils = false;
								retMesgGSIR = "The Sys ID Reference return value is null for " + extRef + " " +
										"[Error from Get Request " + errorTextGetRefGSIR + "]. Record Discarded.";
								logger.error(retMesgGSIR);
							} else {
								logger.debug("The Sys ID Reference return Success");
							}
						} else {
							okToProceedUtils = false;
							retMesgGSIR = "The Sys ID Reference return value is null for " + extRef + " " +
									"[Error from Get Request " + errorTextGetRefGSIR + "]. Record Discarded.";
							logger.error(retMesgGSIR);
						}
					} else {
						okToProceedUtils = false;
						retMesgGSIR = "The Sys ID Reference return value is null for " + extRef + " " +
								"[Error from Get Request " + errorTextGetRefGSIR + "]. Record Discarded.";
						logger.error(retMesgGSIR);
					}
				} catch (Exception e) {
					okToProceedUtils = false;
					retMesgGSIR = "The Sys ID Reference return value is null for " + extRef +
							" [" + errorTextGetRefGSIR + "]. Record Discarded.";
					logger.error("Get Sys ID Reference - Exception ", e);
				}
			}
		}
		theValueRet[0] = Boolean.valueOf(okToProceedUtils);
		theValueRet[1] = fieldValueRefRetGSIR;
		theValueRet[2] = retMesgGSIR;
		return theValueRet;
	}

	public void writeRowDiscarded(XSSFSheet sheetDis, XSSFRow rowToDis, int RowNumCreateDis, int NoCDis) {
		XSSFRow rowDiscarded = sheetDis.createRow(RowNumCreateDis);
		for (int cellNumDis = 0; cellNumDis < NoCDis; cellNumDis++) {
			XSSFCell cellOrig = rowToDis.getCell(cellNumDis);
			if (cellOrig != null &&
					cellOrig.getCellType() != 3 &&
					cellOrig.getCellType() != 5) {
				XSSFCell cellDis;
				switch (cellOrig.getCellType()) {
					case 4:
						cellDis = rowDiscarded.createCell(cellNumDis);
						cellDis.setCellType(4);
						cellDis.setCellValue(rowToDis.getCell(cellNumDis).getBooleanCellValue());
						break;
					case 0:
						cellDis = rowDiscarded.createCell(cellNumDis);
						cellDis.setCellType(0);
						cellDis.setCellValue(rowToDis.getCell(cellNumDis).getNumericCellValue());
						break;
					case 1:
						cellDis = rowDiscarded.createCell(cellNumDis);
						cellDis.setCellType(1);
						cellDis.setCellValue(rowToDis.getCell(cellNumDis).getStringCellValue());
						break;
				}
			}
		}
	}

	public void writeRowOut(XSSFSheet sheetOutP, int Row_number, String Operation_Type, String Exit_Status, String Dest_Table, String Sys_ID, String Notes) {
		XSSFRow rowOut = sheetOutP.createRow(Row_number);
		XSSFCell cellOut = rowOut.createCell(0);
		cellOut.setCellValue(Operation_Type);
		cellOut = rowOut.createCell(1);
		cellOut.setCellValue(Exit_Status);
		cellOut = rowOut.createCell(2);
		cellOut.setCellValue(Dest_Table);
		cellOut = rowOut.createCell(3);
		cellOut.setCellValue(Sys_ID);
		cellOut = rowOut.createCell(4);
		cellOut.setCellValue(Notes);
	}

	public boolean checkRowHasData(XSSFRow r) {
		boolean hasData = true;
		if (r == null) {
			hasData = false;
		} else {
			hasData = false;
			for (Cell c : r) {
				if (c.getCellType() != 3) {
					hasData = true;
					break;
				}
			}
		}
		return hasData;
	}

	public String PLNameName() {
		return "u_enc_picklist_name";
	}

	public String PLValueName() {
		return "u_enc_picklist_value";
	}

	public String SysParmsEqual() {
		return "%3D";
	}

	public String SysParmsAnd() {
		return "%5E";
	}

	public String[] setFieldListMultipleValuesSeparator(String fieldName, String fieldValue) {
		String[] fieldValueArr = { "" };
		boolean okToProceedMVS = true;
		String retMesgMVS = "";
		Object[] fieldConfPropRet = null;
		fieldConfPropRet = loadFldConfProps();
		fldConfProps = (Properties)fieldConfPropRet[0];
		okToProceedMVS = ((Boolean)fieldConfPropRet[1]).booleanValue();
		if (!okToProceedMVS) {
			retMesgMVS = "Error reading Field Configuration Property file";
			logger.error(retMesgMVS);
		}
		String multiplevalueConf = fldConfProps.getProperty("field.multiplevalue." + fieldName, "NOT_FOUND").trim();
		if (!multiplevalueConf.equalsIgnoreCase("NOT_FOUND")) {
			if (fieldValue.length() > 0) {
				logger.info("The field " + fieldName + " is a multiple value field (separator " + multiplevalueConf + ")");
				String str;
				switch ((str = multiplevalueConf).hashCode()) {
					case 2156:
						if (str.equals("CO")) {
							fieldValueArr = fieldValue.split(":");
							break;
						}
					case 2189:
						if (str.equals("DQ")) {
							fieldValueArr = fieldValue.split(";(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
							if (fieldValueArr.length == 0)
								fieldValueArr = fieldValue.split(":(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
							break;
						}
					case 2640:
						if (str.equals("SC")) {
							fieldValueArr = fieldValue.split(";");
							break;
						}
					default:
						fieldValueArr = fieldValue.split(":");
						break;
				}
				for (int ain = 0; ain < fieldValueArr.length; ain++) {
					String newval = fieldValueArr[ain].replaceAll("\"", "");
					fieldValueArr[ain] = newval;
				}
			}
		} else {
			fieldValueArr[0] = fieldValue;
		}
		return fieldValueArr;
	}

	public Object[] getPLArray(String PLName, String PLValue) {
		boolean okToProceedPLA = false;
		String theValueRetrieved = "";
		String retMsgPLA = "Picklist value not found for " + PLName;
		Object[] theValueRetrievedArr = { Boolean.valueOf(okToProceedPLA), theValueRetrieved, retMsgPLA };
		if (PLValue != "" && PLValue != null && !PLValue.isEmpty()) {
			logger.info("Searching for Picklist " + PLName);
			int thePicklistArrayLen = thePicklistArrayQUR.length;
			for (int iGet = 0; iGet < thePicklistArrayLen; iGet++) {
				if (thePicklistArrayQUR[iGet][1] != null &&
						thePicklistArrayQUR[iGet][1].equalsIgnoreCase(PLName) &&
						thePicklistArrayQUR[iGet][2].equalsIgnoreCase(PLValue)) {
					okToProceedPLA = true;
					theValueRetrieved = thePicklistArrayQUR[iGet][0];
					retMsgPLA = "";
					logger.info("The Picklist has Sys Id " + theValueRetrieved);
					break;
				}
			}
		} else {
			okToProceedPLA = true;
			theValueRetrieved = "";
			retMsgPLA = "";
		}
		theValueRetrievedArr[0] = Boolean.valueOf(okToProceedPLA);
		theValueRetrievedArr[1] = theValueRetrieved;
		theValueRetrievedArr[2] = retMsgPLA;
		return theValueRetrievedArr;
	}

	public void populatePLArray(CloseableHttpClient client, String ProxyHost, String ProxyPort) throws HttpException, IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException {
		int countRowsPLLoaded = 0;
		logger.info("++++++++++++++++++++ LOAD PICKLIST VALUES ++++++++++++++++++++");
		CloseableHttpResponse responseGetPL = null;
		HttpGet httpgetPL = new HttpGet();
		httpgetPL = new HttpGet("https://" + ProxyHost + ":" + ProxyPort +
				"/api/now/v2/table/u_enc_cmdb_picklist_values" +
				"?sysparm_fields=sys_id,u_enc_picklist_name,u_enc_picklist_value");
		httpgetPL.setHeader("Accept", "application/json");
		httpgetPL.setHeader("Content-Type", "application/json");
		responseGetPL = client.execute((HttpUriRequest)httpgetPL);
		String retSrcPL = EntityUtils.toString(responseGetPL.getEntity());
		JSONObject resultPL = new JSONObject(retSrcPL);
		JSONArray tokenListPL = resultPL.getJSONArray("result");
		int tokenListLen = tokenListPL.length();
		for (int i = 0; i < tokenListLen; i++) {
			JSONObject oj = tokenListPL.getJSONObject(i);
			thePicklistArrayQUR[i][0] = oj.getString("sys_id");
			thePicklistArrayQUR[i][1] = oj.getString("u_enc_picklist_name");
			thePicklistArrayQUR[i][2] = oj.getString("u_enc_picklist_value");
			countRowsPLLoaded++;
		}
		responseGetPL.close();
		logger.info("Number of Picklist Records loaded: " + countRowsPLLoaded);
		logger.info("++++++++++++++++++++ END LOAD PICKLIST VALUES ++++++++++++++++++++");
	}
}
