package com.nttdata.enel.EnigmaInt;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class PutAction {
	protected static Logger logger = Logger.getLogger("com/nttdata/enel/EnigmaInt");

	public Object[] theRequest(CloseableHttpClient client, String ProxyHost, String ProxyPort, String destTableName, String putData, String theSysID, String returnValueField) throws HttpException, IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException {
		logger.info("++++++++++++++++++++ START PUT ACTION Request - Sys ID " + theSysID + " ++++++++++++++++++++");
		Object[] theValueRet = { "999", "", Boolean.valueOf(true), "" };
		CloseableHttpResponse responsePut = null;
		try {
			HttpPut httpput = new HttpPut(
					"https://" + ProxyHost + ":" + ProxyPort + "/api/now/v2/table/" +
							destTableName + "/" + theSysID + "?sysparm_fields=sys_id");
			httpput.setHeader("Accept", "application/json");
			httpput.setHeader("Content-Type", "application/json");
			logger.info("PUT ACTION - Table: " + destTableName + " - Request: " + putData);
			httpput.setEntity((HttpEntity)new ByteArrayEntity(putData.getBytes("utf-8")));
			logger.debug("PUT ACTION - RequestLine: " + httpput.getRequestLine());
			responsePut = client.execute((HttpUriRequest)httpput);
			StatusLine statusLinePut = responsePut.getStatusLine();
			int StatusCodeIntPut = statusLinePut.getStatusCode();
			theValueRet[0] = Integer.valueOf(StatusCodeIntPut);
			logger.info("PUT ACTION - Status Line: " + statusLinePut + " - Status Code: " + StatusCodeIntPut);
			String responsePostBody = EntityUtils.toString(responsePut.getEntity());
			if (StatusCodeIntPut < 300) {
				JSONObject obj = new JSONObject(responsePostBody);
				theValueRet[1] = obj.getJSONObject("result").getString(returnValueField);
				logger.info("PUT ACTION - Body: " + theValueRet[1]);
			} else {
				logger.info("PUT ACTION - Body (Error): " + responsePostBody);
				theValueRet[2] = Boolean.valueOf(false);
				theValueRet[3] = responsePostBody;
			}
			responsePut.close();
		} catch (Exception e) {
			theValueRet[0] = Integer.valueOf(999);
			theValueRet[1] = "";
			theValueRet[2] = Boolean.valueOf(false);
			theValueRet[3] = "Got Exception in the Get Action";
			logger.error(theValueRet[3], e);
		} finally {
			responsePut.close();
		}
		logger.info("++++++++++++++++++++ End Put Request ++++++++++++++++++++");
		return theValueRet;
	}
}
