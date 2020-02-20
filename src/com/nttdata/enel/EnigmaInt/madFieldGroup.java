package com.nttdata.enel.EnigmaInt;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class madFieldGroup {
    protected static Logger logger = Logger.getLogger("com/nttdata/enel/EnigmaInt");

    String currentStep = "";

    String postStringMad = "";

    static String fieldMad = "u_enc_mark_as_deleted_group";

    public Object[] theRequest(CloseableHttpClient client, String ProxyHost, String ProxyPort, String destTableNameMad, String getDataMad, String returnValueField, String getValueWithSysparm) throws HttpException, IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException {
        this.currentStep = "MARK TO DELETE";
        logger.info("++++++++++++++++++++ " + this.currentStep + " ++++++++++++++++++++");
        logger.info("[" + this.currentStep + "] Table Name: " + destTableNameMad + " - Query: " + getDataMad);
        long startTimeGetReq = System.currentTimeMillis();
        int exitStatusCodeGet = 999;
        boolean okToProceedGetAction = true;
        String errorText = "";
        Object[] theValueRet = { Integer.valueOf(exitStatusCodeGet), Boolean.valueOf(okToProceedGetAction), errorText };
        try {
            StatusLine statusLineGet = null;
            CloseableHttpResponse responseGet = null;
            HttpEntity responseGetBody = null;
            HttpGet httpget = new HttpGet();
            if (getValueWithSysparm.equalsIgnoreCase("Yes")) {
                httpget = new HttpGet("https://" + ProxyHost + ":" + ProxyPort +
                        "/api/now/v2/table/" + destTableNameMad + "?sysparm_query=" + getDataMad);
            } else {
                httpget = new HttpGet("https://" + ProxyHost + ":" + ProxyPort +
                        "/api/now/v2/table/" + destTableNameMad + "?" + getDataMad);
            }
            try {
                httpget.setHeader("Accept", "application/json");
                httpget.setHeader("Content-Type", "application/json");
                responseGet = client.execute((HttpUriRequest)httpget);
                statusLineGet = responseGet.getStatusLine();
                exitStatusCodeGet = statusLineGet.getStatusCode();
                theValueRet[0] = Integer.valueOf(exitStatusCodeGet);
                responseGetBody = responseGet.getEntity();
                if (responseGetBody != null) {
                    String retSrc = "";
                    JSONObject result = null;
                    JSONArray tokenList = null;
                    JSONObject ojm = null;
                    retSrc = EntityUtils.toString(responseGetBody);
                    if (exitStatusCodeGet < 300) {
                        result = new JSONObject(retSrc);
                        tokenList = result.getJSONArray("result");
                        int tokenListLen = tokenList.length();
                        if (tokenListLen > 0)
                            for (int iMad = 0; iMad < tokenList.length(); iMad++) {
                                String token = "";
                                ojm = tokenList.getJSONObject(iMad);
                                if (ojm != null) {
                                    token = ojm.getString("sys_id");
                                    logger.info("[" + this.currentStep + "] Sys ID Marked: " + token);
                                    this.postStringMad = "{ \"" + fieldMad + "\" : \"Yes\" }";
                                    Object[] fieldValueRetPutMad = null;
                                    PutAction putActionMad = new PutAction();
                                    fieldValueRetPutMad = putActionMad.theRequest(client, ProxyHost, ProxyPort,
                                            destTableNameMad, this.postStringMad, token, "sys_id");
                                    int exitStatusCodePutMad = ((Integer)fieldValueRetPutMad[0]).intValue();
                                    String retRowSysIDMad = fieldValueRetPutMad[1].toString().trim();
                                    boolean okToProceedMad = ((Boolean)fieldValueRetPutMad[2]).booleanValue();
                                    String str1 = (String)fieldValueRetPutMad[3];
                                }
                            }
                    }
                    if (exitStatusCodeGet >= 300) {
                        logger.error("[" + this.currentStep + "] " + statusLineGet + " - Exit Code: " + exitStatusCodeGet);
                        logger.error(retSrc);
                        theValueRet[2] = Boolean.valueOf(false);
                        theValueRet[3] = statusLineGet + " - " + retSrc;
                    }
                } else {
                    theValueRet[2] = Boolean.valueOf(false);
                    theValueRet[3] = "[" + this.currentStep + "] Response Body NULL - Exit Code: " + exitStatusCodeGet;
                    logger.error(theValueRet[3]);
                }
                responseGet.close();
            } finally {
                responseGet.close();
            }
        } catch (Exception e) {
            theValueRet[0] = Integer.valueOf(999);
            theValueRet[1] = "";
            theValueRet[2] = Boolean.valueOf(false);
            theValueRet[3] = "Got Exception in the Get Action - " + this.currentStep;
            logger.error(theValueRet[3], e);
        }
        long endTimeGetReq = System.currentTimeMillis();
        logger.info("++++++++++++++++++++ End Get Request MAD (Time ms: " + (endTimeGetReq - startTimeGetReq) + ") ++++++++++++++++++++");
        return theValueRet;
    }
}
