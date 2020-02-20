package com.nttdata.enel.EnigmaInt;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import org.apache.http.HttpException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.json.JSONException;

public class FieldGroupConf {
    protected static Logger logger = Logger.getLogger("com/nttdata/enel/EnigmaInt");

    static String fieldRefCmdbCI = "u_enc_configuration_item";

    static String fieldRefSite = "u_enc_site_reference";

    static String fieldMad = "u_enc_mark_as_deleted_group";

    public static Object[] postGroupTable(String OperationRec, CloseableHttpClient httpClientQur, String proxyhostQur, String proxyportQur, String pdataFlow, Properties fldConfPropsQur, String[] fieldListGroupQur, XSSFRow rowDataQur, XSSFRow headRowQur, int NoCQur, String SysIdMaster) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, HttpException, IOException, JSONException {
        String retCodeQur = "6600";
        String retMesgQur = "";
        boolean okToProceedQur = true;
        Object[] theValueRetQUR = { retCodeQur, retMesgQur, Boolean.valueOf(okToProceedQur) };
        String currentStep = "";
        Utils utilsQur = new Utils();
        ValidateInputFile validateInputFileQur = new ValidateInputFile();
        String fieldListValueQur = "";
        int postStringFGExecutedWithData = 0;
        int postStringFGExecutedWithEmpty = 0;
        if (fieldListGroupQur != null) {
            currentStep = "PROCESSING GROUP FIELD";
            logger.info("******************** " + currentStep + " ********************");
            for (int flPos = 0; flPos < fieldListGroupQur.length; flPos++) {
                logger.info("***** [" + currentStep + "] STARTING WITH GROUP FIELD : " + fieldListGroupQur[flPos]);
                postStringFGExecutedWithData = 0;
                postStringFGExecutedWithEmpty = 0;
                String fieldgroupFilterField = fldConfPropsQur.getProperty("field.group." + fieldListGroupQur[flPos], "NOT_FOUND").trim();
                if (!fieldgroupFilterField.equalsIgnoreCase("NOT_FOUND")) {
                    int fieldGroupDestTableFieldLenTot = 0;
                    int fieldGroupDestTableFieldLenCnt = 0;
                    String[] fieldListGroupArr = fieldgroupFilterField.split("\\|");
                    String[] fieldGroupFromField = fieldListGroupArr[0].split(",");
                    String fieldGroupDestTable = fieldListGroupArr[1];
                    String[] fieldGroupDestTableField = fieldListGroupArr[2].split(",");
                    fieldGroupDestTableFieldLenTot = fieldGroupDestTableField.length;
                    String fieldgroupFilterFieldExtRef = fldConfPropsQur.getProperty("field.group.externalrefmv." + fieldListGroupQur[flPos], "NOT_FOUND").trim();
                    String verifyExtRef = "No";
                    String[] fieldListGroupArrExtRef = null;
                    String fieldGroupDestTableExtRef = null;
                    String[] fieldGroupDestTableFieldExtRef = null;
                    int fieldGroupDestTableFieldExtRefLenTot = 0;
                    int fieldGroupDestTableFieldExtRefLenCnt = 0;
                    if (!fieldgroupFilterFieldExtRef.equalsIgnoreCase("NOT_FOUND")) {
                        verifyExtRef = "Yes";
                        fieldListGroupArrExtRef = fieldgroupFilterFieldExtRef.split("\\|");
                        fieldGroupDestTableExtRef = fieldListGroupArrExtRef[1];
                        fieldGroupDestTableFieldExtRef = fieldListGroupArrExtRef[2].split(",");
                        fieldGroupDestTableFieldExtRefLenTot = fieldGroupDestTableFieldExtRef.length;
                    }
                    if (OperationRec.equalsIgnoreCase("UPDATE")) {
                        String getData =
                                String.valueOf(fieldRefCmdbCI) +
                                        utilsQur.SysParmsEqual() +
                                        SysIdMaster.trim();
                        madFieldGroup MadFieldGroup = new madFieldGroup();
                        MadFieldGroup.theRequest(httpClientQur, proxyhostQur, proxyportQur,
                                fieldGroupDestTable, getData,
                                "sys_id", "Yes");
                    }
                    int maxrowlen = -1;
                    int tmprowlen = 0;
                    try {
                        for (int i_fieldGroupFromField = 0; i_fieldGroupFromField < fieldGroupFromField.length; i_fieldGroupFromField++) {
                            tmprowlen = (
                                    utilsQur.setFieldListMultipleValuesSeparator(fieldGroupFromField[i_fieldGroupFromField].trim(),
                                            utilsQur.getCellVal(fieldGroupFromField[i_fieldGroupFromField], rowDataQur.getCell(utilsQur.findPos(headRowQur, fieldGroupFromField[i_fieldGroupFromField], NoCQur))))).length;
                            if (tmprowlen > maxrowlen)
                                maxrowlen = tmprowlen;
                        }
                    } catch (Exception ab) {
                        retMesgQur = "Group Field multivalue " + fieldListGroupQur[flPos] + " return a null value";
                        retCodeQur = "6622";
                        okToProceedQur = false;
                        logger.error(retMesgQur, ab);
                    }
                    logger.info("[" + currentStep + "] Group Field - Number of Records to Post for " + fieldListGroupQur[flPos] + " is " + maxrowlen);
                    if (okToProceedQur &&
                            maxrowlen > 0) {
                        for (int i_maxrowlen = 0; i_maxrowlen < maxrowlen; i_maxrowlen++) {
                            logger.info("[" + currentStep + "] Group Field - STARTING WITH ROW " + i_maxrowlen);
                            String getStringFGExtRef = "";
                            String postStringFG = "";
                            fieldGroupDestTableFieldLenCnt = 0;
                            fieldGroupDestTableFieldExtRefLenCnt = 0;
                            for (int i_fieldGroupFromField = 0; i_fieldGroupFromField < fieldGroupFromField.length; i_fieldGroupFromField++) {
                                logger.info("[" + currentStep + "] Group Field - STARTING WITH FIELD ELEMENT " + fieldGroupFromField[i_fieldGroupFromField]);
                                fieldListValueQur = "";
                                String fieldFunctionProp = fldConfPropsQur.getProperty("field.function." + fieldGroupFromField[i_fieldGroupFromField], "NOT_FOUND").trim();
                                if (fieldFunctionProp.equalsIgnoreCase("NOT_FOUND"))
                                    try {
                                        logger.info("[" + currentStep + "] Group Field " + fieldGroupFromField[i_fieldGroupFromField] + " without Function Configured.");
                                        fieldListValueQur =

                                                utilsQur.setFieldListMultipleValuesSeparator(fieldGroupFromField[i_fieldGroupFromField],
                                                        utilsQur.getCellVal(fieldGroupFromField[i_fieldGroupFromField], rowDataQur.getCell(utilsQur.findPos(headRowQur, fieldGroupFromField[i_fieldGroupFromField], NoCQur))))[
                                                        i_maxrowlen].trim();
                                    } catch (Exception ab) {
                                        fieldListValueQur = null;
                                    }
                                if (!okToProceedQur) {
                                    retCodeQur = "6623";
                                    retMesgQur = "The field " + fieldGroupFromField[i_fieldGroupFromField] +
                                            " return invalid value. The record is discarded.";
                                }
                                if (okToProceedQur &&
                                        fieldListValueQur != null && fieldListValueQur != "" && !fieldListValueQur.isEmpty()) {
                                    logger.info("[" + currentStep + "] validate datatype of the Group Field " + fieldGroupFromField[i_fieldGroupFromField]);
                                    Object[] ValidateDataTypeObjb = null;
                                    ValidateDataTypeObjb = validateInputFileQur.ValidateDataType(
                                            fieldGroupFromField[i_fieldGroupFromField], fieldListValueQur);
                                    okToProceedQur = ((Boolean)ValidateDataTypeObjb[0]).booleanValue();
                                    fieldListValueQur = ValidateDataTypeObjb[1].toString().trim();
                                    retMesgQur = ValidateDataTypeObjb[2].toString().trim();
                                    if (!okToProceedQur)
                                        retCodeQur = "6624";
                                }
                                if (okToProceedQur)
                                    if (fieldListValueQur != null && fieldListValueQur != "" && !fieldListValueQur.isEmpty()) {
                                        String picklistFilterFieldFG = fldConfPropsQur.getProperty("field.picklist." + fieldGroupFromField[i_fieldGroupFromField], "NOT_FOUND").trim();
                                        if (!picklistFilterFieldFG.equalsIgnoreCase("NOT_FOUND")) {
                                            String picklistFilterFieldFGTableName = picklistFilterFieldFG.split("\\|")[0].trim();
                                            String picklistFilterFieldFGFieldName = picklistFilterFieldFG.split("\\|")[1].trim();
                                            logger.info("[" + currentStep + "] Group Field " + fieldGroupFromField[i_fieldGroupFromField] + " is a Reference.");
                                            Object[] picklistFilterFGResult = null;
                                            picklistFilterFGResult = utilsQur.getPLArray(picklistFilterFieldFGFieldName, fieldListValueQur.trim());
                                            okToProceedQur = ((Boolean)picklistFilterFGResult[0]).booleanValue();
                                            fieldListValueQur = picklistFilterFGResult[1].toString().trim();
                                            String str1 = picklistFilterFGResult[2].toString().trim();
                                        }
                                        if (okToProceedQur) {
                                            if (fieldListValueQur.length() > 0) {
                                                if (verifyExtRef.equalsIgnoreCase("Yes"))
                                                    getStringFGExtRef =
                                                            String.valueOf(getStringFGExtRef) +
                                                                    fieldGroupDestTableFieldExtRef[i_fieldGroupFromField].split(":")[0].trim() +
                                                                    utilsQur.SysParmsEqual() +
                                                                    URLEncoder.encode(fieldListValueQur.trim(), "UTF-8") +
                                                                    utilsQur.SysParmsAnd();
                                                postStringFG = String.valueOf(postStringFG) + "\"" +
                                                        fieldGroupDestTableField[i_fieldGroupFromField].split(":")[0].trim() + "\" : \"" +
                                                        fieldListValueQur.trim() + "\",";
                                            } else {
                                                retCodeQur = "6625";
                                                retMesgQur = "Not existent value for Group Field " + fieldGroupFromField[i_fieldGroupFromField] + " from Picklist. The record is discarded.";
                                            }
                                        } else {
                                            retCodeQur = "6626";
                                            retMesgQur = "Not existent value for Group Field " + fieldGroupFromField[i_fieldGroupFromField] + " from Picklist. The record is discarded.";
                                        }
                                    } else {
                                        if (verifyExtRef.equalsIgnoreCase("Yes")) {
                                            getStringFGExtRef = String.valueOf(getStringFGExtRef) +
                                                    fieldGroupDestTableFieldExtRef[i_fieldGroupFromField].split(":")[0].trim() +
                                                    "ISEMPTY" +
                                                    utilsQur.SysParmsAnd();
                                            fieldGroupDestTableFieldExtRefLenCnt++;
                                        }
                                        fieldGroupDestTableFieldLenCnt++;
                                    }
                                if (!okToProceedQur)
                                    break;
                            }
                            if (okToProceedQur) {
                                logger.debug("+++++ Group Field: String Prepared For the Get Action External Ref: " + getStringFGExtRef);
                                logger.debug("+++++ Group Field: String Prepared For the Post Action: " + postStringFG);
                                if (verifyExtRef.equalsIgnoreCase("Yes"))
                                    if (fieldGroupDestTableFieldExtRefLenCnt != fieldGroupDestTableFieldExtRefLenTot) {
                                        logger.info("[" + currentStep + "] Group Field - Get record for External reference.");
                                        getStringFGExtRef = getStringFGExtRef.substring(0, getStringFGExtRef.length() - utilsQur.SysParmsAnd().length());
                                        Object[] fieldGroupListGetReturnExtRef = null;
                                        GetAction getActionFGExtRef = new GetAction();
                                        fieldGroupListGetReturnExtRef = getActionFGExtRef.theRequest(
                                                httpClientQur, proxyhostQur, proxyportQur,
                                                fieldGroupDestTableExtRef, getStringFGExtRef, "sys_id", "Yes");
                                        int exitStatusCodeGetFGExtRef = ((Integer)fieldGroupListGetReturnExtRef[0]).intValue();
                                        String fieldValueFGExtRef = fieldGroupListGetReturnExtRef[1].toString().trim();
                                        okToProceedQur = ((Boolean)fieldGroupListGetReturnExtRef[2]).booleanValue();
                                        String errorTextFGExtRef = (String)fieldGroupListGetReturnExtRef[3];
                                        if (okToProceedQur && exitStatusCodeGetFGExtRef < 300 && fieldValueFGExtRef.length() > 0) {
                                            logger.debug("Group Field - get record for External reference - Record exists. the Sys ID: " +
                                                    fieldValueFGExtRef);
                                        } else {
                                            logger.error("[" + currentStep + "] Group Field - get record for External reference - Record not found." +
                                                    " Exit code: " + exitStatusCodeGetFGExtRef + " - Error: " + errorTextFGExtRef);
                                            okToProceedQur = false;
                                        }
                                    } else {
                                        logger.debug("[" + currentStep + "] Group Field - Get Action - All External reference Fields are Empty.");
                                    }
                            }
                            if (okToProceedQur)
                                if (fieldGroupDestTableFieldLenCnt != fieldGroupDestTableFieldLenTot) {
                                    if (postStringFG.length() > 0) {
                                        if (pdataFlow.equalsIgnoreCase("CMDB"))
                                            postStringFG = "{\"" +
                                                    fieldRefCmdbCI.trim() + "\" : \"" + SysIdMaster.trim() + "\" , " +
                                                    "\"" + fieldMad + "\" : \"No\" , " +
                                                    postStringFG.substring(0, postStringFG.length() - 1) + "}";
                                        if (pdataFlow.equalsIgnoreCase("SITE"))
                                            postStringFG = "{\"" +
                                                    fieldRefSite.trim() + "\" : \"" + SysIdMaster.trim() + "\" , " +
                                                    "\"" + fieldMad + "\" : \"No\" , " +
                                                    postStringFG.substring(0, postStringFG.length() - 1) + "}";
                                        logger.info("[" + currentStep + "] Group Field - Execute Post Action on Table Name: " + fieldGroupDestTable);
                                        Object[] fieldValueRetPostFG = null;
                                        int exitStatusCodePostFG = 0;
                                        PostAction postActionFG = new PostAction();
                                        fieldValueRetPostFG = postActionFG.theRequest(httpClientQur, proxyhostQur, proxyportQur,
                                                fieldGroupDestTable, postStringFG, "sys_id");
                                        exitStatusCodePostFG = ((Integer)fieldValueRetPostFG[0]).intValue();
                                        String fieldValuePostFG = fieldValueRetPostFG[1].toString().trim();
                                        okToProceedQur = ((Boolean)fieldValueRetPostFG[2]).booleanValue();
                                        String errorTextPostFG = (String)fieldValueRetPostFG[3];
                                        if (okToProceedQur && exitStatusCodePostFG < 300) {
                                            logger.info("[" + currentStep + "] Group Field - Post Action executed with success. " +
                                                    "Record created with Sys ID: " + fieldValuePostFG);
                                            postStringFGExecutedWithData++;
                                        } else {
                                            retCodeQur = "6627";
                                            retMesgQur = "Error during the Post Action for Group Fields : " + exitStatusCodePostFG +
                                                    " - Error: " + errorTextPostFG +
                                                    ". The record is discarded.";
                                            okToProceedQur = false;
                                        }
                                    }
                                } else {
                                    postStringFGExecutedWithEmpty++;
                                    logger.info("[" + currentStep + "] Group Field - Get Action - All Fields are Empty.");
                                }
                            if (!okToProceedQur)
                                break;
                        }
                        if (!okToProceedQur)
                            logger.error(retMesgQur);
                        int postStringFGExecutedTotal = postStringFGExecutedWithEmpty + postStringFGExecutedWithData;
                        if (postStringFGExecutedTotal == maxrowlen) {
                            retCodeQur = "";
                            retMesgQur = "Group Table  " + fieldListGroupQur[flPos] + " OK";
                            logger.debug(retMesgQur);
                        }
                        if (postStringFGExecutedTotal < maxrowlen) {
                            retCodeQur = "6671";
                            retMesgQur = "Group Table " + fieldListGroupQur[flPos] +
                                    " KO [" + postStringFGExecutedWithData + "-" + postStringFGExecutedWithEmpty + "] / " + maxrowlen;
                            okToProceedQur = false;
                            logger.error(retMesgQur);
                        }
                    }
                }
                if (!okToProceedQur)
                    break;
            }
        }
        theValueRetQUR[0] = retCodeQur;
        theValueRetQUR[1] = retMesgQur;
        theValueRetQUR[2] = Boolean.valueOf(okToProceedQur);
        return theValueRetQUR;
    }
}
