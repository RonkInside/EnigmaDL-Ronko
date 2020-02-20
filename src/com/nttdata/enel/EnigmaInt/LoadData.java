package com.nttdata.enel.EnigmaInt;

import java.io.*;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.log4j.Logger;

public class LoadData {

	protected static Logger   logger             = Logger.getLogger("com/nttdata/enel/EnigmaInt");
	static Properties         connProps          = new Properties();
	static Properties         appProps           = new Properties();
	static Properties         fldConfProps       = new Properties();	
	//static String             connectionProps    = System.getenv("CONNECTIONPROPS");
	//static String             applicationProps   = System.getenv("APPLICATIONPROPS");
	//static String             fieldConfProps     = System.getenv("FIELDCONF");

	static String             connectionProps    = "C:\\ENEL\\ENIGMA\\conf\\connection.properties";
	static String             applicationProps   = "C:\\ENEL\\ENIGMA\\conf\\application.properties";
	static String             fieldConfProps     = System.getenv("FIELDCONF");


	static XSSFCell cellkhead; 

	static String basicAuth   = "";

	static String xlsInput   = null;
	static String xlsOutput  = null;
	static String xlsDiscarded = null;

	static String getSheetNum=null;
	static String firstRowDatax=null;
	static int    firstRowData=2;
	static String createDiscardedFile=null;
	static String insertRecord=null;
	static String updateRecord=null;
	static String checkMandatoryField=null;

	static String currentWorkBook=null;

	static XSSFSheet sheet;
	static XSSFRow   headRow;
	static XSSFRow   commentRow;
	static XSSFRow   rowData;

	static int    NoC; // numberOfCells
	static int    totalNumOfRows;	

	static XSSFWorkbook workbookOut=null;
	static XSSFWorkbook wbDiscarded=null;
	static XSSFSheet    sheetOutSite;
	static XSSFSheet    sheetOutCmdb;
	static XSSFSheet    sheetDiscarded;
	static int          recCountOut=0;

	static String getKeyTabString="";
	static String postString="";
	static String postStringKT="";
	static String postStringRef="";

	static String destTableName="";

	static int    driverFieldPosition=-1;
	static String driverFieldName="u_enc_device_type";
	static String driverFieldValue="";
	static String [][] thePicklistArray = new String [170000][3];

	static int   thePos=-1;

	static Boolean  okToProceed=true;

	static String[] fieldListMand=null;
	static String[] fieldListPKey=null;
	static String[] fieldListAttr=null;
	static String[] fieldListGroup=null;
	static String[] fieldListExtRef=null;


	static String   fieldMandatoryDefaultNotPermitted="N.A.";

	//variable for the output
	static String   currentStep="";
	static int      retCode=0;
	static String   retMesg="";
	static String   retRowSysID="";

	public void convertFile(
			CloseableHttpClient httpClient,	String proxyhost, String proxyport, 
			String FlowName, 
			String xlsFileName,	String xlsOutput) 
					throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, HttpException, JSONException
	{
		try {

			com.nttdata.enel.EnigmaInt.Utils utils = new com.nttdata.enel.EnigmaInt.Utils();
			com.nttdata.enel.EnigmaInt.FieldsFunct fieldsFunct = new com.nttdata.enel.EnigmaInt.FieldsFunct();

			com.nttdata.enel.EnigmaInt.ValidateInputFile validateInputFile = new com.nttdata.enel.EnigmaInt.ValidateInputFile();

			/* OPEN FILE FOR OUTPUT ********************************************** */
			workbookOut = new XSSFWorkbook();

			// Open output file
			sheetOutSite = workbookOut.createSheet("SITE");
			sheetOutCmdb = workbookOut.createSheet("CMDB");
			utils.writeRowOut(sheetOutSite, 0, "Operation", "Exit Status", "Destination Table", "Sys ID", "Notes");
			utils.writeRowOut(sheetOutCmdb, 0, "Operation", "Exit Status", "Destination Table", "Sys ID", "Notes");

			// CONFIGURATION PROPERTIES FILE
			currentStep="CONFIGURATION PROPERTIES FILE";
			logger.info("******************** "+ currentStep);

			try {
				appProps.load(new FileInputStream(applicationProps));
			} catch (Exception esdf) {
				retMesg="Error reading Application Configuration Property file";				
				logger.error(retMesg);
				okToProceed=false;
			}
			try {
				fldConfProps.load(new FileInputStream(fieldConfProps));
			} catch (Exception esdf) {
				retMesg="Error reading Field Configuration Property file";
				logger.error(retMesg);
				okToProceed=false;
			}

			if (okToProceed) {
				// GET PROPERTY VALUES
				currentStep="GET PROPERTY VALUES";
				logger.info("******************** "+ currentStep);

				insertRecord=appProps.getProperty("insertRecord","NOT_FOUND").trim();
				if (insertRecord.equalsIgnoreCase("NOT_FOUND")) {
					logger.info("Parameter insertRecord is not configured");
					okToProceed=false; 
				} else {
					logger.info("Parameter insertRecord is configured to " + insertRecord);
				}

				updateRecord=appProps.getProperty("updateRecord","NOT_FOUND").trim();
				if (updateRecord.equalsIgnoreCase("NOT_FOUND")) { 
					logger.info("Parameter updateRecord is not configured");
					okToProceed=false; 
				} else {
					logger.info("Parameter updateRecord is configured to " + updateRecord);
				}

				checkMandatoryField=appProps.getProperty("checkMandatoryField","NOT_FOUND").trim();
				if (checkMandatoryField.equalsIgnoreCase("NOT_FOUND")) { 
					logger.info("Parameter checkMandatoryField is not configured");
					okToProceed=false; 
				} else {
					logger.info("Parameter checkMandatoryField is configured to " + checkMandatoryField);
				}

				getSheetNum=fldConfProps.getProperty("getSheetNum","NOT_FOUND");
				if (getSheetNum.equalsIgnoreCase("NOT_FOUND")) { okToProceed=false; }

				firstRowDatax=fldConfProps.getProperty("firstRowData","NOT_FOUND");
				if (firstRowDatax.equalsIgnoreCase("NOT_FOUND")) { 
					logger.info("Parameter firstRowData is not configured");
					okToProceed=false; 
				} else {
					firstRowData=Integer.parseInt(firstRowDatax);
					logger.info("Parameter firstRowData is configured to " + firstRowData);
				}

				createDiscardedFile=appProps.getProperty("createDiscardedFile","NOT_FOUND").trim();
				if (createDiscardedFile.equalsIgnoreCase("NOT_FOUND")) { 
					logger.info("Parameter createDiscardedFile is not configured");
					okToProceed=false; 
				} else {
					logger.info("Parameter createDiscardedFile is configured to " + createDiscardedFile);
				}

				if (!okToProceed) {
					retMesg="Missing information in the Configuration File.";
					logger.error(retMesg);
				}
			}

			if (okToProceed) { 

				// General - open input file
				InputStream InputStream=new FileInputStream(new File(xlsFileName));
				XSSFWorkbook workBook  = new XSSFWorkbook (InputStream);
				sheet = null;

				// General - get sheet
				sheet = workBook.getSheetAt(Integer.parseInt(getSheetNum));
				currentWorkBook=workBook.getSheetName(Integer.parseInt(getSheetNum)).toUpperCase().trim();
				logger.info("Current Workbook is " + currentWorkBook);
				if (!(currentWorkBook.equalsIgnoreCase("CMDB")  
						|| currentWorkBook.equalsIgnoreCase("SITE"))
						// || currentWorkBook.equalsIgnoreCase("PICKLIST"))
						) {
					retMesg="The current Sheet Name is not valid. values admitted are CMDB and SITE";
					logger.error(retMesg);
					okToProceed=false;
				}
			}

			if (okToProceed) {				
				// General - number of rows and number of cells calculated on first row
				headRow        = sheet.getRow(0);
				commentRow     = sheet.getRow(1);
				NoC            = headRow.getLastCellNum();
				totalNumOfRows = sheet.getPhysicalNumberOfRows();				
				logger.info("Total Number Of Cells: " + NoC+ " - Total Number Of Rows: " + totalNumOfRows);
			} 

			if (okToProceed) {
				if (currentWorkBook.equalsIgnoreCase("CMDB")) {
					// Get Device Type Field Position
					currentStep="VALIDATE DEVICE TYPE FIELD POSITION";
					logger.info("******************** "+ currentStep);

					driverFieldPosition=utils.findPos(headRow,driverFieldName,NoC);
					if (driverFieldPosition != -1) {			
						logger.info("Device Type Field Configuration " + driverFieldName +" found at position "+driverFieldPosition);
					} else { 
						retMesg="Device Type Field Configuration " + driverFieldName +" not found in the Source Data File";
						logger.error(retMesg);
						okToProceed=false;
					}
				}
			}

			if (okToProceed) { 
				// if configured, write discarded records to a file
				if (createDiscardedFile.equalsIgnoreCase("Yes")) {
					wbDiscarded = new XSSFWorkbook();
					sheetDiscarded = wbDiscarded.createSheet("DISCARDED");	
					utils.writeRowDiscarded (
							sheetDiscarded,
							headRow,
							0,
							NoC);
					utils.writeRowDiscarded (
							sheetDiscarded,
							commentRow,
							1,
							NoC);
				}
			}

			if (okToProceed) { 

				/* 
				 * 
				 * 
				 * 
				 * 
				 * 
				 * 
				 *  ***** FOR EACH ROW ****************************************************** 
				 * 
				 * 
				 * 
				 * 
				 * 
				 * 
				 * */			

				int totalRowsElab=0;
				int rowsInserted=0;
				int rowsUpdated=0;
				int rowDiscarded=2;
				String recordToDiscard="YES";

				for (int krow=firstRowData; krow<totalNumOfRows; krow++) {

					try {

						logger.info("######################## Start Processing Row " + krow + " ########################");
						long startTime = System.currentTimeMillis();
						rowData = sheet.getRow(krow);
						recordToDiscard="YES";
						okToProceed=true;

						retCode=0;
						retMesg="";
						retRowSysID="";

						/* 
						 * 
						 * Check if the row has no element to import
						 *  
						 */
						if (!(utils.checkRowHasData(rowData))) {
							retCode=9001;
							retMesg="The Current Row is Empty. The Row is Skipped (is not an Error).";
							logger.warn(retMesg);
							okToProceed=false; 
						}

						// The row contains elements to import
						if (okToProceed) {

							++recCountOut;
							totalRowsElab++;
							currentStep="PROCESSING ROW FOR "+currentWorkBook;
							logger.info("################################ "+ currentStep + " ############################################");

							String fieldListMandx="";
							String fieldListAttrx="";
							String fieldListGrpx="";
							String fieldListExtRefx="";
							String fieldKeyTabx="";
							String sheetContext="";

							retCode=0;
							retMesg="";
							retRowSysID="";

							if (!(okToProceed)) {
								retCode=9002;
								retMesg="Error in the current record. The Entire Row is Discarded"; 
								logger.error(retMesg);
							}

							/*
							 * 
							 * 
							 * GET PROPERTIES
							 * 
							 * 
							 */
							if (okToProceed) {
								retCode=9003;
								switch (currentWorkBook) {

								case "SITE": 
									currentStep="GET PROPERTIES FOR "+currentWorkBook;
									logger.info("******************** "+ currentStep);
									destTableName="";
									fieldListMand=null;
									fieldListPKey=null;
									fieldListAttr=null;
									fieldListGroup=null;
									fieldListExtRef=null;

									// Get Destination Table
									destTableName=fldConfProps.getProperty("site.tablename","NOT_FOUND").trim();
									if (destTableName.equalsIgnoreCase("NOT_FOUND")) {
										retMesg="Destination Table Name not configured";
										logger.error(retMesg);
										okToProceed=false; 
									} else {
										logger.info("Destination Table Name parameter: " + destTableName);		
									}								

									// Get List of Mandatory Field in configuration properties
									if (checkMandatoryField.equalsIgnoreCase("Yes")) {
										fieldListMandx=fldConfProps.getProperty("site.fieldmandatory","NOT_FOUND").trim();
										if (fieldListMandx.equalsIgnoreCase("NOT_FOUND")) {
											retMesg="Mandatory Field List parameter not configured";
											logger.error(retMesg);
											okToProceed=false; 
										} else {
											logger.info("Mandatory Field List parameter: " + fieldListMandx);
											fieldListMand=fldConfProps.getProperty("site.fieldmandatory","NOT_FOUND").trim().split(",");
										}									
									}

									// Get List of Attribute Fields in configuration properties
									fieldListAttrx=fldConfProps.getProperty("site.fieldAttr","NOT_FOUND").trim();
									if (fieldListAttrx.equalsIgnoreCase("NOT_FOUND")) {
										retMesg="Attribute Field List parameter not configured";
										logger.error(retMesg);
										okToProceed=false; 
									} else {
										logger.info("Attribute Field List parameter: " + fieldListAttrx);
										fieldListAttr=fldConfProps.getProperty("site.fieldAttr","NOT_FOUND").trim().split(",");
									}

									// Get List of Group Fields in configuration properties
									fieldListGrpx=fldConfProps.getProperty("site.fieldGroup","NOT_FOUND").trim();
									if (fieldListGrpx.equalsIgnoreCase("NOT_FOUND")) {
										retMesg="Group Field List parameter not configured";
										logger.error(retMesg);
										okToProceed=false; 
									} else {
										logger.info("Group Field List parameter: " + fieldListGrpx);
										fieldListGroup=fldConfProps.getProperty("site.fieldGroup","NOT_FOUND").trim().split(",");
									}

									// Get List of Key Fields in configuration properties
									fieldKeyTabx=fldConfProps.getProperty("site.keytab","NOT_FOUND").trim();
									if (fieldKeyTabx.equalsIgnoreCase("NOT_FOUND")) { 
										retMesg="Key Field List not configured";
										logger.error(retMesg);
										okToProceed=false; 
									} else {
										logger.info("Key Field List parameter: " + fieldKeyTabx);
										fieldListPKey=fldConfProps.getProperty("site.keytab","NOT_FOUND").trim().split(",");
									}

									if (okToProceed) {
										logger.info("["+currentStep+"] Properties for Site are OK.");
									}

									break;

								case "CMDB": 
									currentStep="GET PROPERTIES FOR "+currentWorkBook;
									logger.info("******************** "+ currentStep);
									destTableName="";
									fieldListMand=null;
									fieldListPKey=null;
									fieldListAttr=null;
									fieldListGroup=null;
									fieldListExtRef=null;

									driverFieldValue=
											utils.getCellVal(rowData.getCell((short) driverFieldPosition))
											.replaceAll("[^A-Za-z0-9]", "_").toUpperCase().trim();
									if (driverFieldValue==null || 
											driverFieldValue==""   || 
											driverFieldValue.toString().isEmpty()) {
										retMesg="Device Type not configured for this row.";
										logger.error(retMesg);
										okToProceed=false; 									
									} else {
										logger.info("Device Type " + driverFieldValue);
									}

									sheetContext=fldConfProps.getProperty("sheet.context."+driverFieldValue,"NOT_FOUND").trim();
									if (sheetContext.equalsIgnoreCase("NOT_FOUND")) { 
										sheetContext="Default";
									}
									logger.info("Device Type for the parameters: " + sheetContext);

									// Get Destination Table
									destTableName=fldConfProps.getProperty("cmdb.tablename."+sheetContext,"NOT_FOUND").trim();
									if (destTableName.equalsIgnoreCase("NOT_FOUND")) {
										retMesg="Destination Table Name not configured";
										logger.error(retMesg);
										okToProceed=false; 
									} else {
										logger.info("Destination Table Name parameter: " + destTableName);		
									}								

									// Get List of Mandatory Field in configuration properties
									if (checkMandatoryField.equalsIgnoreCase("Yes")) {
										fieldListMandx=fldConfProps.getProperty("cmdb.fieldmandatory."+sheetContext,"NOT_FOUND").trim();
										if (fieldListMandx.equalsIgnoreCase("NOT_FOUND")) {
											retMesg="Mandatory Field List parameter not configured";
											logger.error(retMesg);
											okToProceed=false; 
										} else {
											fieldListMand=fldConfProps.getProperty("cmdb.fieldmandatory."+sheetContext,"NOT_FOUND").trim().split(",");
											logger.info("Mandatory Field List parameter ("+ fieldListMand.length +") "+ fieldListMandx);
										}
									}										

									// Get List of Attribute Fields in configuration properties
									fieldListAttrx=fldConfProps.getProperty("cmdb.fieldAttr."+sheetContext,"NOT_FOUND").trim();
									if (fieldListAttrx.equalsIgnoreCase("NOT_FOUND")) {
										retMesg="Attribute Field List parameter not configured";
										logger.error(retMesg);
										okToProceed=false; 
									} else {
										fieldListAttr=fldConfProps.getProperty("cmdb.fieldAttr."+sheetContext,"NOT_FOUND").trim().split(",");
										logger.info("Attribute Field List parameter ("+ fieldListAttr.length +") "+ fieldListAttrx);
									}

									// Get List of Group Fields in configuration properties
									fieldListGrpx=fldConfProps.getProperty("cmdb.fieldGroup."+sheetContext,"NOT_FOUND").trim();
									if (fieldListGrpx.equalsIgnoreCase("NOT_FOUND")) {
										retMesg="Group Field List parameter not configured";
										logger.error(retMesg);
										okToProceed=false; 
									} else {
										fieldListGroup=fldConfProps.getProperty("cmdb.fieldGroup."+sheetContext,"NOT_FOUND").trim().split(",");
										logger.info("Group Field List parameter ("+ fieldListGroup.length +") "+ fieldListGrpx);
									}

									// Get List of External Fields in configuration properties
									fieldListExtRefx=fldConfProps.getProperty("cmdb.externalref."+sheetContext,"NOT_FOUND").trim();
									if (fieldListExtRefx.equalsIgnoreCase("NOT_FOUND")) {
										retMesg="External Reference Field List not configured";
										logger.error(retMesg);
										okToProceed=false; 
									} else {
										fieldListExtRef=fldConfProps.getProperty("cmdb.externalref."+sheetContext,"NOT_FOUND").trim().split(",");
										logger.info("External Reference Field List parameter ("+ fieldListExtRef.length +") "+ fieldListExtRefx);
									}

									// Get List of Key Fields in configuration properties
									fieldKeyTabx=fldConfProps.getProperty("cmdb.keytab."+sheetContext,"NOT_FOUND").trim();
									if (fieldKeyTabx.equalsIgnoreCase("NOT_FOUND")) { 
										retMesg="Key Field List parameter not configured";
										logger.error(retMesg);
										okToProceed=false; 
									} else {
										fieldListPKey=fldConfProps.getProperty("cmdb.keytab."+sheetContext,"NOT_FOUND").trim().split(",");
										logger.info("Key Field List parameter ("+ fieldListPKey.length +") "+ fieldKeyTabx);
									}


									if (okToProceed) {
										logger.info("["+currentStep+"] Properties for CMDB are OK.");
									}
									break;
									//picklist
								default: 
									retMesg="Current Sheet not configured (admitted values are SITE and CMDB)";
									logger.error(retMesg);
									okToProceed=false;
								}
							}


							/* 
							 * 
							 * 
							 * 
							 * 
							 * ************************************************************************
							 * ******** VALIDATE SECTION ********************************************** 
							 * ************************************************************************
							 * 
							 * 
							 * 
							 * 
							 * */


							if (okToProceed) {
								/* 
								 * 
								 * VALIDATE FIELDS POSITION ********************************************* 
								 * 
								 * */
								currentStep="VALIDATE FIELD POSITION";
								logger.info("******************** "+ currentStep + " ********************");

								Object[] validateFileInputAttr = null;
								validateFileInputAttr=validateInputFile.doValidate (headRow, rowData, fieldListAttr, NoC);
								okToProceed=(Boolean) validateFileInputAttr[0];
								retMesg=(String) validateFileInputAttr[1];

								if (okToProceed) {
									logger.info("["+currentStep+"] Attribute Fields Validation are OK.");
								} else {
									retCode=9011;
									logger.error(retMesg);
								}

							}  // END VALIDATE FIELDS POSITION


							if (okToProceed) {
								/* 
								 * 
								 * VALIDATE GROUP FIELDS POSITION ********************************************* 
								 * 
								 * */
								currentStep="VALIDATE GROUP FIELD POSITION";
								logger.info("******************** "+ currentStep + " ********************");

								Object[] validateFileInputFG = null;
								validateFileInputFG=validateInputFile.doValidate (headRow, rowData, fieldListGroup, NoC);
								okToProceed=(Boolean) validateFileInputFG[0];
								retMesg=(String) validateFileInputFG[1];

								if (okToProceed) {
									logger.info("["+currentStep+"] Group Fields Validation are OK.");
								} else {
									retCode=9012;
									logger.error(retMesg);
								}

							} // END VALIDATE GROUP FIELDS POSITION

							if (okToProceed) {
								/* 
								 * 
								 * VALIDATE MANDATORY FIELDS ************************************************** 
								 * 
								 * */				
								if (checkMandatoryField.equalsIgnoreCase("Yes")) {
									currentStep="VALIDATE MANDATORY FIELDS";
									logger.info("******************** "+ currentStep + " ********************");

									for (int flMandPos=0; flMandPos<fieldListMand.length; flMandPos++) {	
										String theMandatoryField=
												utils.getCellVal(rowData.getCell(utils.findPos(headRow,fieldListMand[flMandPos],NoC))).trim();
										if (theMandatoryField==null || 
												theMandatoryField==""   || 
												theMandatoryField.toString().isEmpty() ||
												theMandatoryField==fieldMandatoryDefaultNotPermitted) {
											retCode=9013;
											retMesg="Mandatory field "+fieldListMand[flMandPos]+ " is null or has value Not Available (N.A.). The record is discarded.";
											okToProceed=false;	
											break;
										}
									}

									if (okToProceed) {
										logger.info("["+currentStep+"] Mandatory Fields are OK.");
									} else {
										logger.error(retMesg);
									}

								}
							}  // END VALIDATE MANDATORY FIELDS


							if (okToProceed) { 
								/* 
								 * 
								 * VALIDATE KEY FIELDS ********************************************************
								 *  
								 *  */
								currentStep="VALIDATE KEY FIELDS";
								logger.info("******************** "+ currentStep + " ********************");

								getKeyTabString="";
								postStringKT="";
								for (int flKPos=0; flKPos<fieldListPKey.length; flKPos++) {	
									String theValueKT="";

									String externalRefKT=fldConfProps.getProperty("field.externalref."+fieldListPKey[flKPos],"NOT_FOUND").trim();
									if (!(externalRefKT.equalsIgnoreCase("NOT_FOUND"))) {

										/*
										 *  PROCESSING EXTERNAL FIELD REFERENCE
										 */

										logger.info("["+currentStep+"] Key Field "+fieldListPKey[flKPos] + " is a External Field Reference");
										Object[] externalRefResult = null;
										externalRefResult=utils.getSysIDReference 
												( httpClient, proxyhost,  proxyport,
														fieldListPKey[flKPos],
														rowData,
														headRow,
														NoC);
										okToProceed=(Boolean) externalRefResult[0];
										theValueKT=externalRefResult[1].toString().trim();
										String externalRefResultretMesg=externalRefResult[2].toString().trim();  // the return mesg from call
										//logger.debug("["+currentStep+"] External Field Value is "+ theValueKT); //DATA

									} else {

										String picklistFilterFieldREF=fldConfProps.getProperty("field.picklist."+fieldListPKey[flKPos],"NOT_FOUND").trim();
										if (!(picklistFilterFieldREF.equalsIgnoreCase("NOT_FOUND"))) {

											/*
											 *  Field Reference
											 */

											logger.info("["+currentStep+"] Key Field "+fieldListPKey[flKPos] + " is a Reference");
											Object[] picklistFilterResultKeyT = null;
											// Picklist parameters for getPLArray is field name, field value
											picklistFilterResultKeyT=getPLArray(picklistFilterFieldREF,fieldListPKey[flKPos].trim());
											okToProceed=(Boolean) picklistFilterResultKeyT[0];
											theValueKT=picklistFilterResultKeyT[1].toString().trim();
											String picklistFilterResultKeyTretMesg=picklistFilterResultKeyT[2].toString().trim(); // the return mesg from call
											//logger.debug("["+currentStep+"] Reference Field Value "+ theValueKT); //DATA

										} else {

											/*
											 *  Field Attribute
											 */

											logger.info("["+currentStep+"] Key Field "+fieldListPKey[flKPos] + " is an Attribute");
											try {
												theValueKT=utils.getCellVal(rowData.getCell(utils.findPos(headRow,fieldListPKey[flKPos],NoC))).trim();
											} catch(Exception e) {
												retCode=9016;
												retMesg="Error getting the Field Value for field attribute "+fieldListPKey[flKPos];
												logger.error(retMesg,e);
												okToProceed=false;										
											}
										}
									}

									//for each field
									if ((okToProceed) && (theValueKT.length()>1)) {
										getKeyTabString=getKeyTabString+
												fieldListPKey[flKPos].split(":")[0].trim() +
												utils.SysParmsEqual() + 
												URLEncoder.encode(theValueKT,"UTF-8") + 
												utils.SysParmsAnd();

										postStringKT=postStringKT+"\""+
												fieldListPKey[flKPos].split(":")[0].trim()+"\" : \""+
												theValueKT+"\",";  //doublequotes
									} else {
										retCode=9017;
										// can be also return mesg from calls getPLArray and getSysIDReference 
										retMesg="Key field "+fieldListPKey[flKPos].split(":")[0].trim()+ " is null or is not a valid value. The record is discarded.";
										okToProceed=false;	
										break;
									}

								}  // FOR LOOP Key fields

								// for each row in loop validate key field
								if (okToProceed) {
									logger.info("["+currentStep+"] Key Fields are OK.");
									getKeyTabString = getKeyTabString.substring(0, getKeyTabString.length() - utils.SysParmsAnd().length()); 
									logger.debug("["+currentStep+"] The Get Action Request for Key Fields: "+getKeyTabString);  //DATA
								} else {
									logger.error(retMesg);

								}

							}  // END VALIDATE KEY FIELDS

							/*
							 * 
							 *  ******** END VALIDATE SECTION ****************************************
							 *  
							 */



							/* 
							 * 
							 * 
							 * 
							 * 
							 * ************************************************************************
							 * ******** PREPARE Get and Post String SECTION *************************** 
							 * ************************************************************************
							 * 
							 * 
							 * 
							 * 
							 * */

							postString="";  // String for the Post and Put Request
							if (postStringKT.length()>1) {  
								// Add Post String of Key Fields
								postString=postString+postStringKT;  //even if postString is null
							}

							String fieldListValue="";  // the value

							if (okToProceed) {
								/* 
								 * 
								 * 
								 * PROCESSING ATTRIBUTE AND REFERENCE (PICKLIST) FIELDS ******************************************** 
								 * 
								 * 
								 * */
								currentStep="PROCESSING ATTR./REF. FIELDS";
								logger.info("******************** "+ currentStep + " ********************");

								for (int flPos=0; flPos<fieldListAttr.length; flPos++) {
									logger.info("***** ["+currentStep+"] START WITH Field: "+ fieldListAttr[flPos]);

									String fieldListValueT="";  // temp value var for elab
									fieldListValue="";

									try {
										//get the value
										fieldListValueT=utils.getCellVal(rowData.getCell(utils.findPos(headRow,fieldListAttr[flPos],NoC))).trim();
									} catch(Exception e) {
										retCode=9019;
										retMesg="["+currentStep+"] Error getting the Field Value for "+fieldListAttr[flPos];
										okToProceed=false;	
										logger.error(retMesg, e);
									}

									if (okToProceed) {
										String picklistFilterFieldREF=fldConfProps.getProperty("field.picklist."+fieldListAttr[flPos],"NOT_FOUND").trim();
										if (!(picklistFilterFieldREF.equalsIgnoreCase("NOT_FOUND"))) {

											/* 
											 * 
											 * REFERENCE (PICKLIST) FIELD ******************************************** 
											 * 
											 * */

											try {	
												String [] fieldListValueTMVal= 
														// Multiple Values Separator: return splitted value
														utils.setFieldListMultipleValuesSeparator(
																fieldListAttr[flPos], fieldListValueT);
												for (int flPosMVal=0; flPosMVal<fieldListValueTMVal.length; flPosMVal++) {

													// Validate data type of field - integer and date (FieldName, FieldValue)
													// fieldListValueTMVal[flPosMVal] is the field value
													Object[] ValidateDataTypeObjc=null;
													ValidateDataTypeObjc=validateInputFile.ValidateDataType
															(fieldListAttr[flPos], fieldListValueTMVal[flPosMVal]);
													okToProceed=(Boolean) ValidateDataTypeObjc[0];
													String fieldListValueVDTT=ValidateDataTypeObjc[1].toString().trim();
													retMesg=ValidateDataTypeObjc[2].toString().trim();

													if (!okToProceed) {
														retCode=9021;
													}


													if (okToProceed) {
														// fieldListValueTMVal[flPosMVal] is the value
														logger.info("["+currentStep+"] Reference Field "+ fieldListAttr[flPos]);
														logger.debug("["+currentStep+"] Reference Field Value "+ fieldListValueVDTT); //DATA

														// Get Picklist Value - picklistFilterFieldREF is the picklist name
														Object[] picklistFilterResultMVal = null;
														picklistFilterResultMVal=getPLArray(picklistFilterFieldREF,fieldListValueVDTT.trim());
														okToProceed=(Boolean) picklistFilterResultMVal[0];
														String picklistFilterResultMValSysId=picklistFilterResultMVal[1].toString().trim();
														retMesg=picklistFilterResultMVal[2].toString().trim(); // the return mesg from call

														if (!okToProceed) {
															retCode=9020;															
														}

														if (okToProceed) {
															if (picklistFilterResultMValSysId.length()>1) {
																fieldListValue=fieldListValue+picklistFilterResultMValSysId+",";
															}
														}
													}

												}

											} catch (Exception ab) {
												retMesg="Reference Field Get Cell multivalue return a null value.";
												retCode=7021;
												okToProceed=false;
												logger.error(retMesg,ab);
											}

											if (okToProceed) {
												if (fieldListValue.length()>1) {
													fieldListValue=fieldListValue.substring(0, fieldListValue.length() - 1);
												}
											}

										} else {

											/* 
											 * 
											 * ATTRIBUTE FIELD ******************************************** 
											 * 
											 * */

											fieldListValue=fieldListValueT;
											if (!(fieldListValue==null || fieldListValue=="" || fieldListValue.isEmpty())) {
												logger.info("["+currentStep+"] Attribute Field "+ fieldListAttr[flPos]+" has value");
												logger.debug("["+currentStep+"] Attribute Field Value "+ fieldListValue); //DATA

												// Validate data type of field - integer and date (FieldName, FieldValue)
												Object[] ValidateDataTypeObja=null;
												ValidateDataTypeObja=validateInputFile.ValidateDataType
														(fieldListAttr[flPos], fieldListValue);
												okToProceed=(Boolean) ValidateDataTypeObja[0];
												fieldListValue=ValidateDataTypeObja[1].toString().trim();
												retMesg=ValidateDataTypeObja[2].toString().trim();

												if (!okToProceed) {
													retCode=9022;
												}

											} else {
												logger.info("["+currentStep+"] Attribute Field "+ fieldListAttr[flPos]+" is empty");
											}
										}						

										if (okToProceed) {
											if (!(fieldListValue==null || fieldListValue=="" || fieldListValue.isEmpty())) {
												postString=postString+"\""+
														fieldListAttr[flPos].split(":")[0].trim()+"\" : \""+
														fieldListValue.replaceAll("\"", "\\\"").trim()+
														"\","; //DQ
											}
										}
									}

									if (!okToProceed) {
										break;
									}
								}  // loop PROCESSING Attr./Ref. FIELDS
							}								

							if (okToProceed) {
								logger.debug("+++++ postString after "+currentStep+" "+postString);
							}


							if (okToProceed) {
								/* 
								 * 
								 * 
								 * 
								 * PROCESSING FIELD GROUP ******************************************** 
								 * 
								 * 
								 * 
								 * */
								currentStep="PROCESSING GROUP FIELD";
								logger.info("******************** "+ currentStep + " ********************");

								for (int flPos=0; flPos<fieldListGroup.length; flPos++) {
									logger.info("***** ["+currentStep+"] STARTING WITH Group Field: "+ fieldListGroup[flPos]);

									String fieldgroupFilterField=fldConfProps.getProperty("field.group."+fieldListGroup[flPos],"NOT_FOUND").trim();							
									if (!(fieldgroupFilterField.equalsIgnoreCase("NOT_FOUND"))) {

										/*											 * 
										 *  Configure the variables for field group
										 */
										String     fieldListFGValueArrVal="";
										String[]   fieldListGroupArr=fieldgroupFilterField.split("\\|");
										String[]   fieldGroupFromField=fieldListGroupArr[0].split(",");  // fields from file
										String     fieldGroupDestTable=fieldListGroupArr[1];  // destination table
										String[]   fieldGroupDestTableField=fieldListGroupArr[2].split(",");  // fields of the destination table 

										//Field group with multiplevalue reference - check existence of referenced record
										String     fieldgroupFilterFieldExtRef=fldConfProps.getProperty("field.group.externalrefmv."+fieldListGroup[flPos],"NOT_FOUND").trim();
										String     verifyExtRef="";
										String[]   fieldListGroupArrExtRef=null;
										String     fieldGroupDestTableExtRef=null;
										String[]   fieldGroupDestTableFieldExtRef=null;
										int        fieldGroupDestTableFieldExtRefLenTot=0;
										int        fieldGroupDestTableFieldExtRefLenCnt=0;
										if (!(fieldgroupFilterFieldExtRef.equalsIgnoreCase("NOT_FOUND"))) {
											verifyExtRef="Yes";
											fieldListGroupArrExtRef=fieldgroupFilterFieldExtRef.split("\\|");
											fieldGroupDestTableExtRef=fieldListGroupArrExtRef[1];  // destination table - External Ref
											fieldGroupDestTableFieldExtRef=fieldListGroupArrExtRef[2].split(",");  // fields of the destination table - External Ref
											fieldGroupDestTableFieldExtRefLenTot=fieldGroupDestTableFieldExtRef.length;
										}

										/*
										 * 											 *  
										 *  Get MAX number of entries for each field for the next loop
										 *  											 *  
										 */
										int maxrowlen=-1;
										int tmprowlen=0;
										try {
											for (int i_fieldGroupFromField=0; i_fieldGroupFromField<fieldGroupFromField.length; i_fieldGroupFromField++) {
												tmprowlen= // get the number of elements in the field value
														utils.setFieldListMultipleValuesSeparator(fieldGroupFromField[i_fieldGroupFromField],
																utils.getCellVal(rowData.getCell(utils.findPos(headRow,fieldGroupFromField[i_fieldGroupFromField],NoC)))
																).length;
												if (tmprowlen > maxrowlen) { maxrowlen=tmprowlen; }
											}
										} catch (Exception ab) {
											retMesg="Group Field Get Cell multivalue return a null value";
											retCode=7022;
											okToProceed=false;
											logger.error(retMesg,ab);
										}
										logger.info("["+currentStep+"] Group Field - Number of Elements to Post for "+fieldListGroup[flPos]+" is "+ maxrowlen);


										/*
										 * 
										 *  Prepare Get and Post Requests - maxrowlen is the number of record to post
										 *  
										 */
										if (okToProceed) {
											if (maxrowlen>0) {

												for (int i_maxrowlen=0; i_maxrowlen<maxrowlen; i_maxrowlen++) {
													/*
													 * 
													 *  FOR EACH ROW of field group
													 *  
													 */
													logger.info("["+currentStep+"] Group Field - STARTING WITH ROW "+i_maxrowlen);
													String getStringFG=""; 
													String getStringFGExtRef="";
													String postStringFG="";

													for (int i_fieldGroupFromField=0; i_fieldGroupFromField<fieldGroupFromField.length; i_fieldGroupFromField++) {

														/*
														 *  
														 *  For each field in the ROW 
														 *  *  Field name is fieldGroupFromField[i_fieldGroupFromField]
														 *  
														 */			

														logger.info("["+currentStep+"] Group Field - STARTING WITH FIELD ELEMENT "+fieldGroupFromField[i_fieldGroupFromField]);
														fieldListValue="";  // the field value


														/*
														 * 
														 * FUNCTION
														 * 
														 */

														String fieldFunctionProp=fldConfProps.getProperty("field.function."+fieldGroupFromField[i_fieldGroupFromField],"NOT_FOUND").trim();
														if (!(fieldFunctionProp.equalsIgnoreCase("NOT_FOUND"))) { 

															/*
															 * 
															 * Start Applying the function if the field is configured in the property file
															 * 
															 */

															String[] fieldFunctionPropArr=fieldFunctionProp.split("\\|");													
															logger.info("["+currentStep+"] Group Field - "+fieldGroupFromField[i_fieldGroupFromField] +
																	" is a field to elaborate from field "+fieldFunctionPropArr[0]);

															Object[] ExtractResultFG = null;
															try {
																String groupFieldValueTPfieldExtract=  // is the single value - field to extract
																		(utils.setFieldListMultipleValuesSeparator(fieldFunctionPropArr[0],
																				utils.getCellVal(rowData.getCell(utils.findPos(headRow,fieldFunctionPropArr[0],NoC)))
																				))[i_maxrowlen].trim();
																ExtractResultFG=fieldsFunct.Elab(  
																		// get the value of field defined in fieldExtractArr[0] in the current row
																		groupFieldValueTPfieldExtract, fieldFunctionPropArr);
																okToProceed    = (Boolean) ExtractResultFG[0];
																fieldListValue = (String) ExtractResultFG[1];
															} catch (Exception ab) {
																retMesg="Group Field function Get Cell multivalue return a null value";
																retCode=7023;
																okToProceed=false;
																logger.error(retMesg,ab);
															}

														} 

														else {

															/*
															 *  Group Field without Function Configured
															 */

															try {	
																logger.info("["+currentStep+"] Group Field "+fieldGroupFromField[i_fieldGroupFromField]+ " without Function Configured.");
																fieldListValue=  // is the current field value
																		// return splitted value
																		(utils.setFieldListMultipleValuesSeparator(fieldGroupFromField[i_fieldGroupFromField],
																				utils.getCellVal(rowData.getCell(utils.findPos(headRow,fieldGroupFromField[i_fieldGroupFromField],NoC)))
																				))[i_maxrowlen].trim();
															} catch (Exception ab) {
																// value not present at the element in i_maxrowlen
																retMesg="Group Field Get Cell multivalue return a null value";
																retCode=7024;
																okToProceed=false;
																logger.error(retMesg,ab);
															}
														} 
														// END FUNCTION


														if (!okToProceed) {
															retCode=9023;
															retMesg="The function for the field "+fieldGroupFromField[i_fieldGroupFromField]+
																	" return invalid value. The record is discarded.";
														}

														if (okToProceed) {
															logger.debug("["+currentStep+"] The Value for Field "+fieldGroupFromField[i_fieldGroupFromField]+ " is "+ fieldListValue);  //DATA
														}

														/*
														 * 
														 * Validate Data Type
														 * 
														 */

														if (okToProceed) {
															if (!(fieldListValue==null || fieldListValue=="" || fieldListValue.isEmpty())) {

																// Validate data type of field - integer and date (FieldName, FieldValue)
																logger.info("["+currentStep+"] validate datatype of the Group Field "+fieldGroupFromField[i_fieldGroupFromField]);
																Object[] ValidateDataTypeObjb=null;
																ValidateDataTypeObjb=validateInputFile.ValidateDataType
																		(fieldGroupFromField[i_fieldGroupFromField], fieldListValue);
																okToProceed=(Boolean) ValidateDataTypeObjb[0];
																fieldListValue=ValidateDataTypeObjb[1].toString().trim();
																retMesg=ValidateDataTypeObjb[2].toString().trim();
																if (!okToProceed) {
																	retCode=9024;
																} 
															}
														}

														/*
														 * 
														 * GROUP FIELD IS A REFERENCE FIELD
														 * 
														 */

														if (okToProceed) {
															if (!(fieldListValue==null || fieldListValue=="" || fieldListValue.isEmpty())) {
																/*
																 * check if the group field is a reference															
																 */
																String picklistFilterFieldFG=fldConfProps.getProperty("field.picklist."+fieldGroupFromField[i_fieldGroupFromField],"NOT_FOUND").trim();
																if (!(picklistFilterFieldFG.equalsIgnoreCase("NOT_FOUND"))) {
																	/*
																	 *  group field is a reference, get sys id from Picklist
																	 */
																	logger.info("["+currentStep+"] Group Field "+fieldGroupFromField[i_fieldGroupFromField]+ " is a Reference.");
																	Object[] picklistFilterFGResult = null;
																	picklistFilterFGResult=getPLArray(picklistFilterFieldFG,fieldListValue.trim());
																	okToProceed    = (Boolean) picklistFilterFGResult[0];
																	fieldListValue = picklistFilterFGResult[1].toString().trim();  // fieldListValue is the sys id of the referenced picklist
																	String picklistFilterFGResultretMesg = picklistFilterFGResult[2].toString().trim(); // the return mesg from call

																}								

																if (okToProceed) {
																	if (fieldListValue.length()>1) {
																		logger.debug("+++++ Field Group Dest. Table Fields "+
																				fieldGroupDestTableField[i_fieldGroupFromField]+" - value: "+fieldListValue);  //DATA
																		// QUERY CONSTRUCTOR
																		getStringFG=
																				getStringFG+
																				fieldGroupDestTableField[i_fieldGroupFromField].split(":")[0].trim()+
																				utils.SysParmsEqual()+
																				URLEncoder.encode(fieldListValue.trim(),"UTF-8")+
																				utils.SysParmsAnd();
																		if (verifyExtRef.equalsIgnoreCase("Yes")) {
																			logger.debug("+++++ Field Group Dest. Table Fields ExtRef "+
																					fieldGroupDestTableFieldExtRef[i_fieldGroupFromField]+" - value: "+fieldListValue);  //DATA
																			getStringFGExtRef=
																					getStringFGExtRef+
																					fieldGroupDestTableFieldExtRef[i_fieldGroupFromField].split(":")[0].trim()+
																					utils.SysParmsEqual()+
																					URLEncoder.encode(fieldListValue.trim(),"UTF-8")+
																					utils.SysParmsAnd();
																		}

																		postStringFG=postStringFG+"\""+
																				fieldGroupDestTableField[i_fieldGroupFromField].split(":")[0].trim()+"\" : \""+
																				fieldListValue.trim()+"\","; //doublequotes
																	} else {
																		retCode=9925;
																		retMesg="Not existent value in the Picklist for Group Field "+ fieldGroupFromField[i_fieldGroupFromField] + " from Picklist. The record is discarded.";
																	} 
																} else {
																	retCode=9926;
																	retMesg="Not existent value in the Picklist for Group Field "+ fieldGroupFromField[i_fieldGroupFromField] + " from Picklist. The record is discarded.";
																}
															} else {
																// QUERY CONSTRUCTOR
																getStringFG=getStringFG+
																		fieldGroupDestTableField[i_fieldGroupFromField].split(":")[0].trim() 
																		+"ISEMPTY" +
																		utils.SysParmsAnd();
																if (verifyExtRef.equalsIgnoreCase("Yes")) {
																	getStringFGExtRef=getStringFGExtRef+
																			fieldGroupDestTableFieldExtRef[i_fieldGroupFromField].split(":")[0].trim() 
																			+"ISEMPTY" +
																			utils.SysParmsAnd(); 
																	fieldGroupDestTableFieldExtRefLenCnt++;
																}
															}

														} 


														if (!okToProceed) {
															//logger.error(retMesg);
															break;
														}	

													} // END for each field in the row



													if (okToProceed) {
														/*
														 * 
														 * 
														 * 
														 *  Group Field - Post the Request - Check if the record exists (Get Action)
														 *  
														 *  
														 *  
														 */

														logger.debug("+++++ Group Field: String Prepared For the Get Action: "  + getStringFG);
														logger.debug("+++++ Group Field: String Prepared For the Get Action External Ref: " + getStringFGExtRef);														
														logger.debug("+++++ Group Field: String Prepared For the Post Action: " + postStringFG);


														if (verifyExtRef.equalsIgnoreCase("Yes")) {
															/*
															 * 
															 * Get Action - verify if exists a record for External reference
															 * 
															 */

															logger.info("["+currentStep+"] Group Field - Get record for External reference.");
															getStringFGExtRef=getStringFGExtRef.substring(0, getStringFGExtRef.length() - utils.SysParmsAnd().length());
															Object[] fieldGroupListGetReturnExtRef = null;
															GetAction getActionFGExtRef = new GetAction();
															fieldGroupListGetReturnExtRef=getActionFGExtRef.theRequest(
																	httpClient, proxyhost, proxyport, 
																	fieldGroupDestTableExtRef,getStringFGExtRef,"sys_id","Yes");
															int exitStatusCodeGetFGExtRef = (int) fieldGroupListGetReturnExtRef[0];
															String fieldValueFGExtRef = fieldGroupListGetReturnExtRef[1].toString().trim();  // the sys id
															okToProceed               = (Boolean) fieldGroupListGetReturnExtRef[2];
															String errorText          = (String) fieldGroupListGetReturnExtRef[3];

															if ((okToProceed) && (exitStatusCodeGetFGExtRef < 300) && (fieldValueFGExtRef.length()>1) )
															{
																logger.info("["+currentStep+"] Group Field - get record for External reference - Record exists."+
																		" Exit code: "+exitStatusCodeGetFGExtRef); 
																logger.debug("Group Field - get record for External reference - Record exists."+
																		" the Sys ID: "+ fieldValueFGExtRef);  //DATA
															} else {
																logger.info("["+currentStep+"] Group Field - get record for External reference - Record not found."+
																		" Exit code: "+exitStatusCodeGetFGExtRef+" - Error: "+errorText);
															}
														}
													}

													if (okToProceed) {	
														/*
														 * 
														 * Group Field - Get Action - Get record for field group
														 * 
														 */

														getStringFG=getStringFG.substring(0, getStringFG.length() - utils.SysParmsAnd().length());
														Object[] fieldGroupListGetReturn = null;
														GetAction getActionFG = new GetAction();
														fieldGroupListGetReturn=getActionFG.theRequest(
																httpClient, proxyhost, proxyport, 
																fieldGroupDestTable,getStringFG,"sys_id","Yes");
														int exitStatusCodeGetFG = (int) fieldGroupListGetReturn[0];
														String fieldValueFG     = fieldGroupListGetReturn[1].toString().trim();  // the sys id
														okToProceed             = (Boolean) fieldGroupListGetReturn[2];
														String errorText        = (String) fieldGroupListGetReturn[3];

														/*
														 * 
														 *  (The record does not exists) Execute Post Action
														 *  
														 */

														if ((okToProceed) && (exitStatusCodeGetFG < 300)) {
															if (fieldValueFG==null || fieldValueFG=="" || fieldValueFG.toString().isEmpty()) {																
																if (postStringFG.length()>1) {																	
																	if (insertRecord.equalsIgnoreCase("Yes")) {

																		/*
																		 * 
																		 * 
																		 *  Post Action - add new record
																		 *  
																		 *  
																		 */

																		postStringFG = "{"+ postStringFG.substring(0, postStringFG.length() - 1) + "}";
																		logger.info("["+currentStep+"] Group Field - Execute Post Action on Table Name: "+fieldGroupDestTable);
																		Object[] fieldValueRetPostFG = null;
																		int exitStatusCodePostFG=0;
																		PostAction postActionFG = new PostAction();
																		fieldValueRetPostFG=postActionFG.theRequest(httpClient, proxyhost, proxyport, 
																				fieldGroupDestTable, postStringFG,"sys_id");
																		exitStatusCodePostFG=(int) fieldValueRetPostFG[0];

																		if (exitStatusCodePostFG < 300) {
																			// fieldListFGValueArrVal is the sys ids to post the record in the main table
																			logger.info("["+currentStep+"] Group Field - Post Action executed. Record created with Sys ID: "+
																					fieldValueRetPostFG[1]);  //DATA
																			fieldListFGValueArrVal=
																					fieldListFGValueArrVal+fieldValueRetPostFG[1]+",";

																		} else {
																			retCode=9927;
																			retMesg="Error during the Post Action for Group Fields : " + exitStatusCodePostFG + ". The record is discarded.";
																			logger.error(retMesg);
																			okToProceed=false;
																		}
																	} else {
																		retCode=8001;
																		retMesg="Insert Option is Disabled. Record is not inserted.";
																		//logger.warn(retMesg);
																		okToProceed=false;
																	}
																}
															} else {

																/*
																 * 
																 * Record already exists in the group table
																 * 
																 */
																logger.info("["+currentStep+"] Group Field - Get Action executed. The record exists with Sys ID: "+
																		fieldValueFG); //DATA
																fieldListFGValueArrVal=fieldListFGValueArrVal+fieldValueFG+",";

															}
														}  else { 
															retCode=9929;
															retMesg="Error during the Get Action for Group Fields ("+errorText+"). The record is discarded.";
															//logger.error(retMesg);
															okToProceed=false;
														}
													}  

													if (!okToProceed) {
														break;
													}

												}  // END Group Field - for each row - until maxrowlen>0

												// END Group Field - after post - for each row to post											

												if (okToProceed) {
													if (fieldListFGValueArrVal.length()>1) {
														postString=postString+"\""+
																fieldListGroup[flPos].split(":")[0].trim()+"\" : \""+ 
																fieldListFGValueArrVal.substring(0, fieldListFGValueArrVal.length() - 1).trim()+"\","; //doublequotes
														logger.debug("["+currentStep+"] Group Fields Processed, ALL POSTSTRING : "+postString); //DATA
													}
												} else {
													logger.error(retMesg);
												}

											}  // END if maxrowlen>0
										}
									}

									if (!okToProceed) {
										break; // BREAK PROCESSING GROUP FIELD
									}

								}  // END for each field in fieldListGroup

							}  // END PROCESSING GROUP FIELD


							if (okToProceed) {
								logger.debug("+++++ postString after "+currentStep+" "+postString); //DATA
							}


							if (okToProceed) {
								if (currentWorkBook.equalsIgnoreCase("CMDB")) { // only for CMDB Data

									/* 
									 * 
									 * 
									 * PROCESSING EXTERNAL FIELD REFERENCE ******************************************** 
									 * 
									 * 
									 * */

									currentStep="PROCESSING EXTERNAL REFERENCE FIELD";
									logger.info("******************** "+ currentStep + " ********************");

									for (int flPos=0; flPos<fieldListExtRef.length; flPos++) {
										logger.info("***** ["+currentStep+"] STARTING WITH External Reference Field : "+ fieldListExtRef[flPos]);									

										Object[] externalRefResult = null;
										externalRefResult=utils.getSysIDReference 
												( httpClient, proxyhost,  proxyport,
														fieldListExtRef[flPos],
														rowData,
														headRow,
														NoC);

										okToProceed=(Boolean) externalRefResult[0];
										String externalRefResultSysid=externalRefResult[1].toString().trim();
										retMesg=externalRefResult[2].toString().trim();

										if (okToProceed) {
											if (externalRefResultSysid.length()>1) {
												logger.info("["+currentStep+"] External Reference Field result OK");
												logger.debug("["+currentStep+"] External Reference Field result OK - Sys ID: "+externalRefResultSysid); //DATA

												// post the external reference value
												postString=postString+"\""+
														fieldListExtRef[flPos].split(":")[0].trim()+"\" : \""+ 
														externalRefResultSysid.trim()+"\","; //doublequotes

											} else {
												logger.info("["+currentStep+"] External Reference Field result - Sys ID is empty");
											}
										}

										if (!okToProceed) {
											retCode=9930;
											break;
										}
									}
								}


							}  // END PROCESSING EXTERNAL FIELD REFERENCE


							if (!okToProceed) {
								logger.error(retMesg);
							}

							/* ************************************************************************************* */								

							/*
							 * 
							 * 
							 * 
							 * 
							 * 
							 * 
							 * ******************************************************************
							 * *************** EXECUTE ACTION SECTION ***************************
							 * ******************************************************************
							 * 
							 * 
							 * 
							 * 
							 * 
							 * 
							 */

							int exitStatusCodeGetd = 999;
							String errorText = "";

							if (okToProceed) {
								/* 
								 * 
								 * 
								 * GET ACTION ******************************************************** 
								 * 
								 * 
								 * */
								currentStep="GET ACTION FOR CREATE/UPDATE RECORD";
								logger.info("++++++++++++++++++++ "+ currentStep + " ++++++++++++++++++++");

								logger.debug("["+currentStep+"] Post Action String : "+postString); //DATA

								// Execute Get Action - Key fields on getKeyTabString
								Object[] fieldValueRetKT = null;
								GetAction getActionKT = new GetAction();
								fieldValueRetKT=getActionKT.theRequest(httpClient, proxyhost, proxyport,
										destTableName,getKeyTabString,"sys_id","Yes");
								exitStatusCodeGetd = (int) fieldValueRetKT[0];
								retRowSysID        = fieldValueRetKT[1].toString().trim();
								okToProceed        = (Boolean) fieldValueRetKT[2];
								errorText   = (String) fieldValueRetKT[3];
								logger.info("["+currentStep+"] Return Value from Get Action: the Sys ID " + retRowSysID + " - Status :"  + retCode);


								if ((okToProceed) && (exitStatusCodeGetd < 300)) {							
									if (retRowSysID==null || 
											retRowSysID==""   || 
											retRowSysID.toString().isEmpty()) {

										/* 
										 * 
										 * 
										 * 
										 * POST ACTION ******************************************************* 
										 * 
										 * 
										 * 
										 * */

										if (insertRecord.equalsIgnoreCase("Yes")) {
											currentStep="CREATE RECORD";
											logger.info("++++++++++++++++++++ "+ currentStep + " ++++++++++++++++++++");

											postString = "{"+ postString.substring(0, postString.length() - 1) + "}"; 
											Object[] fieldValueRetPost = null;
											PostAction postAction = new PostAction();											
											fieldValueRetPost=postAction.theRequest(httpClient, proxyhost, proxyport, 
													destTableName, postString,"sys_id");
											retCode=(int) fieldValueRetPost[0];
											retRowSysID=fieldValueRetPost[1].toString().trim();
											retMesg=fieldValueRetPost[2].toString().trim();
											logger.info("["+currentStep+"] Return Value from Post Action: Return Code " + retCode);
											if (retCode < 300) {
												retMesg="Create Record OK";
												logger.info("++++++++++++++++++++ "+retMesg+" ++++++++++++++++++++");
												rowsInserted++;
												// no record to discard
												recordToDiscard="NO";
											} else {
												logger.error("++++++++++++++++++++ "+retMesg+" ++++++++++++++++++++");														
												okToProceed=false;
											}

										} else {
											retCode=8002;
											retMesg="Insert Option is Disabled. Record is not inserted.";
											logger.warn(retMesg);
										}
									}
									else {

										/* 
										 * 
										 * 
										 * 
										 * PUT ACTION ******************************************************** 
										 * 
										 * 
										 * 
										 * */

										if (updateRecord.equalsIgnoreCase("Yes")) {
											currentStep="UPDATE RECORD";
											logger.info("++++++++++++++++++++ "+ currentStep + " ++++++++++++++++++++");
											postString = "{"+ postString.substring(0, postString.length() - 1) + "}"; 
											Object[] fieldValueRetPut = null;
											PutAction putAction = new PutAction();
											fieldValueRetPut=putAction.theRequest(httpClient, proxyhost, proxyport, 
													destTableName, postString, retRowSysID,"sys_id");
											retCode=(int) fieldValueRetPut[0];
											retRowSysID=fieldValueRetPut[1].toString().trim();
											logger.info("["+currentStep+"] Return Value from Put Action: the Sys ID " + retRowSysID);

											if (retCode < 300) {
												retMesg="Update Record OK";
												logger.info("++++++++++++++++++++ "+retMesg+" ++++++++++++++++++++");														
												rowsUpdated++;
												// no record to discard
												recordToDiscard="NO";
											} else {
												retMesg="Error during Record Update Operation : " + retCode;
												logger.error("++++++++++++++++++++ "+retMesg+" ++++++++++++++++++++");
												okToProceed=false;
											}

										} else {
											retCode=8003;
											retMesg="Update Option is Disabled. Record is not updated.";
											logger.warn(retMesg);
										}
									}  
								} else {
									retCode=9997;
									retMesg="Error during the Get Action before Post record ("+errorText+"). The record is discarded.";
									logger.error(retMesg);
									okToProceed=false;
								}

							}  // Execute Get Action

							if (okToProceed) {
								logger.info("++++++++++++++++++++ Get / Post / Put Action Executed." +" ++++++++++++++++++++");
							}

							// END EXECUTE ACTION SECTION 


							/* 
							 * 
							 * 
							 * ROW OUTPUT FILE *************************************************** 
							 * 
							 * 
							 * */	

							if (createDiscardedFile.equalsIgnoreCase("Yes")) {
								logger.info("Writing Record discarded in the output file");
								if (recordToDiscard.equalsIgnoreCase("YES")) {
									logger.info("##### RECORD DISCARDED "+krow);
									utils.writeRowDiscarded (
											sheetDiscarded,
											rowData,
											rowDiscarded,
											NoC);
									rowDiscarded++;
								}
							} 

							// "Operation", "Exit Status", "Destination Table", "Sys ID", "Notes"
							logger.info("##### RETURN CODE FOR ROW : "+krow + " : "+
									currentStep+","+
									Integer.toString(retCode)+","+
									destTableName+","+
									retRowSysID+","+ 
									retMesg);

							if (currentWorkBook.equalsIgnoreCase("SITE")) {
								utils.writeRowOut(sheetOutSite, krow, currentStep,Integer.toString(retCode), destTableName, retRowSysID, retMesg);
							}
							if (currentWorkBook.equalsIgnoreCase("CMDB")) {
								utils.writeRowOut(sheetOutCmdb, krow, currentStep, Integer.toString(retCode), destTableName, retRowSysID, retMesg);	
							}


							//} //LOOP ON THE SHEET

						} // okToProceed - ROW IS NOT NULL

						long endTime = System.currentTimeMillis();
						logger.info("##################### End Processing Row " + krow + " (Time ms: "+ (endTime-startTime) + ") #####################");

					} catch(Exception erow) {
						if (createDiscardedFile.equalsIgnoreCase("Yes")) {
							logger.info("Writing Record discarded in the output file");
							if (recordToDiscard.equalsIgnoreCase("YES")) {
								logger.info("##### RECORD DISCARDED "+krow);
								utils.writeRowDiscarded (
										sheetDiscarded,
										rowData,
										rowDiscarded,
										NoC);
								rowDiscarded++;
							}
						} 

						logger.info("##### RETURN CODE FOR ROW : "+krow + " : "+
								currentStep+","+
								Integer.toString(retCode)+","+
								destTableName+","+
								retRowSysID+","+ 
								retMesg);

						if (currentWorkBook.equalsIgnoreCase("SITE")) {
							utils.writeRowOut(sheetOutSite, krow, currentStep,Integer.toString(retCode), destTableName, retRowSysID, retMesg);
						}
						if (currentWorkBook.equalsIgnoreCase("CMDB")) {
							utils.writeRowOut(sheetOutCmdb, krow, currentStep, Integer.toString(retCode), destTableName, retRowSysID, retMesg);	
						}						
						logger.error("Got Exception in row "+ krow, erow);
					}
				} // END FOR EACH ROW - krow


				/*
				 * 
				 * 
				 *    AFTER ROW PROCESSING LOOP
				 * 
				 * 
				 */

				logger.info("##### END RESULT - Rows Processed (excluded blank rows) : "+totalRowsElab);				
				logger.info("##### END RESULT - OK : Rows Inserted: "+rowsInserted + " - Rows Updated: " +rowsUpdated );
			} 

			/*
			 * 
			 *  write and close output file	
			 *  		
			 */
			try {
				logger.debug("Closing output file ...");
				FileOutputStream outputStream = new FileOutputStream(xlsOutput);
				workbookOut.write(outputStream);
				outputStream.close();
			} catch(Exception ecf) {
				logger.error("Closing output file Exception" , ecf);
			}

			if (createDiscardedFile.equalsIgnoreCase("Yes")) {
				try {
					logger.debug("Closing output file Discarded ...");
					FileOutputStream outputStreamDSC = new FileOutputStream(xlsDiscarded);
					wbDiscarded.write(outputStreamDSC);
					outputStreamDSC.close();
				} catch(Exception ecf) {
					logger.error("Closing output file Discarded Exception" , ecf);
				}
			}


		} catch(Exception e) {
			logger.error("Got Exception in the procedure : " , e);
		}

	}

	/* ********************************************************************* */

	public static Object[] getPLArray(String PLName, String PLValue) {

		boolean okToProceedPLA=false;
		String theValueRetrieved="";
		String retMsgPLA="Picklist value not found for "+PLName;
		Object[] theValueRetrievedArr= { okToProceedPLA, theValueRetrieved , retMsgPLA };
		if (!(PLValue=="" || PLValue==null || PLValue.isEmpty())) {
			logger.info("Searching for Picklist "+ PLName);
			int thePicklistArrayLen=thePicklistArray.length;
			for (int iGet = 0; iGet < thePicklistArrayLen; iGet++) {
				if (thePicklistArray[iGet][1]!=null) {
					if (thePicklistArray[iGet][1].equalsIgnoreCase(PLName) &&
							thePicklistArray[iGet][2].equalsIgnoreCase(PLValue)) {

						okToProceedPLA=true;
						theValueRetrieved=thePicklistArray[iGet][0];
						retMsgPLA="";
						logger.info("The Picklist has Sys Id "+ theValueRetrieved);  //DATA
						break;
					}
				}
			}
		} else {
			okToProceedPLA=true;
			theValueRetrieved="";
			retMsgPLA="";
		}
		theValueRetrievedArr[0]=okToProceedPLA;
		theValueRetrievedArr[1]=theValueRetrieved;
		theValueRetrievedArr[2]=retMsgPLA;
		return theValueRetrievedArr;
	}

	/* ********************************************************************* */

	public static String [][] populatePLArray
	(CloseableHttpClient client, 
			String ProxyHost, String ProxyPort) 
					throws HttpException, IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException  
	{
		int countRowsPLLoaded=0;
		logger.info("++++++++++++++++++++ LOAD PICKLIST VALUES ++++++++++++++++++++");

		try {
			CloseableHttpResponse responseGetPL = null;
			HttpGet httpgetPL = new HttpGet();
			httpgetPL = new HttpGet("https://"+ProxyHost+":"+ProxyPort+
					"/api/now/v2/table/u_enc_cmdb_picklist_values"+						
					"?sysparm_fields=sys_id,u_enc_picklist_name,u_enc_picklist_value"
					);				
			httpgetPL.setHeader("Accept", "application/json");
			httpgetPL.setHeader("Content-Type", "application/json");

			responseGetPL = client.execute(httpgetPL);
			String retSrcPL = EntityUtils.toString(responseGetPL.getEntity());
			JSONObject resultPL = new JSONObject(retSrcPL);
			JSONArray tokenListPL = resultPL.getJSONArray("result");
			int tokenListLen=tokenListPL.length();

			for (int i = 0; i < tokenListLen; i++) {
				JSONObject oj = tokenListPL.getJSONObject(i);
				thePicklistArray[i][0]=oj.getString("sys_id");
				thePicklistArray[i][1]=oj.getString("u_enc_picklist_name");
				thePicklistArray[i][2]=oj.getString("u_enc_picklist_value");
				countRowsPLLoaded++;
			}
			responseGetPL.close();			
			logger.info("Number of Picklist Records loaded: "+countRowsPLLoaded);
		} finally {
			//logger.debug("----------------- finally close client Get -----------------");
			//client.close();
		}
		logger.info("++++++++++++++++++++ END LOAD PICKLIST VALUES ++++++++++++++++++++");
		return thePicklistArray;
	}

	/* ********************************************************************* */

	public static void main (String[] args) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, HttpException, IOException, JSONException
	{
		try {
			com.nttdata.enel.EnigmaInt.Utils utils = new com.nttdata.enel.EnigmaInt.Utils();
			String username=System.getProperty("username");
			String psp=System.getProperty("password");
			String dataFlow=System.getProperty("snclass");

			logger.info("####################################################################");
			logger.info("####################### LOADDATA ###################################");
			logger.info("####################################################################");			

			try {
				connProps.load(new FileInputStream(connectionProps));
			} catch (Exception esdfcc) {
				logger.error("Error reading Application Configuration Property file", esdfcc);
			}		
			String proxyhost=connProps.getProperty("ProxyHost","NOT_FOUND").trim();
			if (proxyhost.equalsIgnoreCase("NOT_FOUND")) { okToProceed=false; }
			String proxyport=connProps.getProperty("ProxyPort","NOT_FOUND").trim();
			if (proxyport.equalsIgnoreCase("NOT_FOUND")) { okToProceed=false; }

			if (!okToProceed) {
				retMesg="Missing information in the Configuration File (Proxy EE Server).";
				logger.error(retMesg);
			}

			if (okToProceed) {
				SSLContext sslContext = new SSLContextBuilder()
						.loadTrustMaterial(null, (certificate, authType) -> true).build();

				CredentialsProvider provider = new BasicCredentialsProvider();
				UsernamePasswordCredentials credentials
				= new UsernamePasswordCredentials(username, psp);
				provider.setCredentials(AuthScope.ANY, credentials);

				logger.debug("Opening Http Connection ...");
				CloseableHttpClient httpClient = HttpClientBuilder.create()
						.setSSLContext(sslContext)
						.setSSLHostnameVerifier(new NoopHostnameVerifier())
						.setDefaultCredentialsProvider(provider)
						.build();
				logger.debug("Testing Connection to "+proxyhost);
				int exitStatusCodeGetTestConn=0;
				try {
					Object[] testConn = null;
					com.nttdata.enel.EnigmaInt.GetAction getActionTestConn = new com.nttdata.enel.EnigmaInt.GetAction();
					testConn=getActionTestConn.theRequest(
							httpClient, proxyhost, proxyport, 
							"u_enc_cmdb_picklist_values",utils.PLNameName()+"=DEVICE_TYPE","sys_id","Yes");
					exitStatusCodeGetTestConn=(int) testConn[0];
					if (exitStatusCodeGetTestConn < 300) {
						logger.info("Connection OK");
					} else {
						retMesg="Connection ERROR - Exit STatus: "+exitStatusCodeGetTestConn;
						logger.error(retMesg);
						okToProceed=false;
					}
				} catch(Exception e) {
					retMesg="Connection ERROR - Exit STatus: "+exitStatusCodeGetTestConn;
					logger.error(retMesg , e);
					okToProceed=false;
				} 

				if (okToProceed) {
					String inPath=System.getenv("DATAHOMEDIR").trim();
					String outPath=System.getenv("OUTDIR").trim();
					File folder = new File(inPath);
					File[] listOfFiles = folder.listFiles();
					for (int i = 0; i < listOfFiles.length; i++) {
						if (listOfFiles[i].isFile()) {
							SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMddHHmm");
							Date nowS = new Date(); 
							String FlowID = formatter1.format(nowS);
							logger.info("Flow ID " + FlowID);
							xlsInput = inPath + "\\" + listOfFiles[i].getName();
							xlsOutput = outPath + "\\" + 
									FlowID+"_Result_"+listOfFiles[i].getName();
							xlsDiscarded = outPath + "\\" + 
									FlowID+"_DISCARDED_"+listOfFiles[i].getName();
							logger.info("Processing Input File " + xlsInput);
							logger.info("Output File Name: " + xlsOutput);



							try {
								populatePLArray(httpClient,proxyhost, proxyport);
								LoadData loaddata = new LoadData ();
								loaddata.convertFile(httpClient,proxyhost, proxyport, 
										FlowID,xlsInput,xlsOutput);
							} catch(Exception e) {
								retMesg="Processing ERROR";
								logger.error(retMesg , e);
								okToProceed=false;
							} 	


						}  //  listOfFiles[i].isFile()
					}
				}

				logger.debug("Closing Http Connection ...");
				httpClient.close();
			}

			logger.info("####################################################################");
			logger.info("########################## END LOADDATA ############################");
			logger.info("####################################################################");

			System.exit(0);	
		} catch(Exception e) {
			logger.error("Main Exception " , e);
		} 
	}
}