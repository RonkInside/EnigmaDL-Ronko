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
import java.util.Properties;
import org.apache.http.HttpException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONException;

public class LoadDataPL {
    protected static Logger logger = Logger.getLogger("com/nttdata/enel/EnigmaInt");

    static Properties appProps = new Properties();

    static Properties fldConfProps = new Properties();

    static String applicationProps = System.getenv("APPLICATIONPROPS");

    static int headRowNum = 0;

    static int labelRowNum = 2;

    static int firstRowData = 4;

    static int kcolNum = 1;

    static String getSheetNum = null;

    static String firstRowDatax = null;

    static String insertRecord = null;

    static String updateRecord = null;

    static String currentWorkBook = null;

    static XSSFSheet sheet;

    static int NoC;

    static int totalNumOfRows;

    static XSSFRow headRow;

    static XSSFRow labelRow;

    static XSSFRow rowData;

    static XSSFWorkbook workbookOut = null;

    static XSSFSheet sheetOut;

    static int sheetOutRowNum = 1;

    static int recCountOut = 0;

    static Boolean okToProceed = Boolean.valueOf(true);

    static String currentStep = "";

    static int retCode = 0;

    static String retMesg = "";

    static String retRowSysID = "";

    static String getKeyTabString = "";

    static String postString = "";

    static String destTableName = "";

    public void convertFilepl(CloseableHttpClient httpClient, String proxyhost, String proxyport, String pdataFlow, String FlowName, String xlsFileName, String xlsOutput) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, HttpException, JSONException, IOException {
        try {
            Utils utils = new Utils();
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
                getSheetNum = fldConfProps.getProperty("getSheetNum", "NOT_FOUND");
                if (getSheetNum.equalsIgnoreCase("NOT_FOUND"))
                    okToProceed = Boolean.valueOf(false);
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
                headRow = sheet.getRow(headRowNum);
                labelRow = sheet.getRow(labelRowNum);
                NoC = headRow.getLastCellNum();
                totalNumOfRows = sheet.getPhysicalNumberOfRows();
                logger.info("Total Number Of Cells: " + NoC + " - Total Number Of Rows: " + totalNumOfRows);
            }
            if (okToProceed.booleanValue()) {
                int totalRowsElab = 0;
                int rowsInserted = 0;
                int rowsUpdated = 0;
                int krowSrc = 0;
                for (int krow = firstRowData; krow < totalNumOfRows; krow++) {
                    krowSrc = krow + 1;
                    logger.info("######################## Start Processing Row " + krowSrc + " ########################");
                    long startTime = System.currentTimeMillis();
                    rowData = sheet.getRow(krow);
                    okToProceed = Boolean.valueOf(true);
                    retCode = 0;
                    retMesg = "";
                    retRowSysID = "";
                    if (!utils.checkRowHasData(rowData)) {
                        retCode = 9001;
                        retMesg = "The Current Row is Empty. The Row is Skipped (is not an Error).";
                        logger.warn(retMesg);
                        okToProceed = Boolean.valueOf(false);
                    }
                    if (okToProceed.booleanValue()) {
                        recCountOut++;
                        totalRowsElab++;
                        int kcolSrc = kcolNum;
                        for (int kcol = kcolNum; kcol < NoC; kcol++) {
                            kcolSrc = kcol + 1;
                            postString = "";
                            getKeyTabString = "";
                            destTableName = "";
                            int exitStatusCodeGetd = 999;
                            String errorTextGetd = "";
                            okToProceed = Boolean.valueOf(true);
                            retCode = 0;
                            retMesg = "";
                            retRowSysID = "";
                            String thePlValueName = utils.getCellVal("u_enc_picklist_name", headRow.getCell(kcol)).trim();
                            String thePlValueLabel = utils.getCellVal("u_enc_picklist_label", labelRow.getCell(kcol)).trim();
                            String thePlValueValue = utils.getCellVal("u_enc_picklist_value", rowData.getCell(kcol)).trim();
                            destTableName = fldConfProps.getProperty("field.picklist." + thePlValueName, "NOT_FOUND").split("\\|")[0].trim();
                            logger.info("STARTING WITH " + thePlValueName + " (" + thePlValueLabel + ") -> " + destTableName);
                            if (thePlValueName != null && !thePlValueName.isEmpty() && thePlValueName != "" &&
                                    thePlValueLabel != null && !thePlValueLabel.isEmpty() && thePlValueLabel != "" &&
                                    thePlValueValue != null && !thePlValueValue.isEmpty() && thePlValueValue != "" &&
                                    destTableName != null && !destTableName.isEmpty() && destTableName != "") {
                                getKeyTabString =
                                        "u_enc_picklist_name" +
                                                utils.SysParmsEqual() +
                                                thePlValueName +
                                                utils.SysParmsAnd() +
                                                "u_enc_picklist_value" +
                                                utils.SysParmsEqual() +
                                                URLEncoder.encode(thePlValueValue, "UTF-8");
                                postString = "{ \"u_enc_picklist_name\" : \"" +
                                        thePlValueName.trim() + "\" , " +
                                        "\"" + "u_enc_picklist_value" + "\" : \"" + thePlValueValue.trim() + "\" , " +
                                        "\"" + "u_enc_picklist_label" + "\" : \"" + thePlValueLabel.trim() + "\"" + " }";
                                currentStep = "GET ACTION FOR CREATE/UPDATE RECORD";
                                logger.info("++++++++++++++++++++ " + currentStep + " ++++++++++++++++++++");
                                logger.debug("[" + currentStep + "] Get Action Request  : " + getKeyTabString);
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
                                        if (insertRecord.equalsIgnoreCase("Yes")) {
                                            currentStep = "CREATE RECORD";
                                            logger.info("++++++++++++++++++++ " + currentStep + " ++++++++++++++++++++");
                                            Object[] fieldValueRetPost = null;
                                            PostAction postAction = new PostAction();
                                            fieldValueRetPost = postAction.theRequest(httpClient, proxyhost, proxyport,
                                                    destTableName, postString, "sys_id");
                                            int exitStatusCodePost = ((Integer)fieldValueRetPost[0]).intValue();
                                            retRowSysID = fieldValueRetPost[1].toString().trim();
                                            okToProceed = (Boolean)fieldValueRetPost[2];
                                            retMesg = (String)fieldValueRetPost[3];
                                            logger.info("[" + currentStep + "] Return Value from Post Action: " +
                                                    "Return Code " + exitStatusCodePost);
                                            if (okToProceed.booleanValue() && exitStatusCodePost < 300) {
                                                retCode = 9091;
                                                retMesg = "Create Record OK";
                                                logger.info("++++++++++++++++++++ " + retMesg + " ++++++++++++++++++++");
                                                logger.info("[" + currentStep + "] Sys ID: " + retRowSysID);
                                                rowsInserted++;
                                            } else {
                                                logger.error("++++++++++++++++++++ " + retMesg + " ++++++++++++++++++++");
                                                okToProceed = Boolean.valueOf(false);
                                            }
                                        } else {
                                            retCode = 8002;
                                            retMesg = "Insert Option is Disabled. Record is not inserted.";
                                            logger.warn(retMesg);
                                        }
                                    } else if (updateRecord.equalsIgnoreCase("Yes")) {
                                        currentStep = "UPDATE RECORD";
                                        logger.info("++++++++++++++++++++ " + currentStep + " ++++++++++++++++++++");
                                        Object[] fieldValueRetPut = null;
                                        PutAction putAction = new PutAction();
                                        fieldValueRetPut = putAction.theRequest(httpClient, proxyhost, proxyport,
                                                destTableName, postString, retRowSysID, "sys_id");
                                        int exitStatusCodePut = ((Integer)fieldValueRetPut[0]).intValue();
                                        retRowSysID = fieldValueRetPut[1].toString().trim();
                                        okToProceed = (Boolean)fieldValueRetPut[2];
                                        retMesg = (String)fieldValueRetPut[3];
                                        logger.info("[" + currentStep + "] Return Value from Put Action: " +
                                                "Return Code " + exitStatusCodePut);
                                        if (okToProceed.booleanValue() && exitStatusCodePut < 300) {
                                            retCode = 9092;
                                            retMesg = "Update Record OK";
                                            logger.info("++++++++++++++++++++ " + retMesg + " ++++++++++++++++++++");
                                            logger.info("[" + currentStep + "] Sys ID: " + retRowSysID);
                                            rowsUpdated++;
                                        } else {
                                            retMesg = "Error during Record Update Operation : " + retCode;
                                            logger.error("++++++++++++++++++++ " + retMesg + " ++++++++++++++++++++");
                                            okToProceed = Boolean.valueOf(false);
                                        }
                                    } else {
                                        retCode = 8003;
                                        retMesg = "Update Option is Disabled. Record is not updated.";
                                        logger.warn(retMesg);
                                    }
                                } else {
                                    retCode = 9997;
                                    retMesg = "Error during the Get Action before Post record (" + errorTextGetd + "). The record is discarded.";
                                    logger.error(retMesg);
                                    okToProceed = Boolean.valueOf(false);
                                }
                                if (okToProceed.booleanValue())
                                    logger.info("++++++++++++++++++++ Get / Post / Put Action Executed. ++++++++++++++++++++");
                            } else {
                                retCode = 8052;
                                retMesg = "Fields in the data source are empty. Record is discared.";
                                logger.warn(retMesg);
                            }
                            retMesg = "[" + krowSrc + "-" + kcolSrc + "] " + retMesg;
                            logger.info("##### RETURN CODE FOR ROW-COLUMN [" + krowSrc + "-" + kcolSrc + "] : " +
                                    currentStep + "," +
                                    Integer.toString(retCode) + "," +
                                    destTableName + "," +
                                    retRowSysID + "," +
                                    retMesg);
                            sheetOutRowNum++;
                            utils.writeRowOut(sheetOut, sheetOutRowNum, currentStep, Integer.toString(retCode), destTableName, retRowSysID, retMesg);
                        }
                    }
                    long endTime = System.currentTimeMillis();
                    logger.info("##################### End Processing Row " + krowSrc + " (Time ms: " + (endTime - startTime) + ") #####################");
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
        } catch (Exception e) {
            logger.error("Main Exception ", e);
        }
    }
}
