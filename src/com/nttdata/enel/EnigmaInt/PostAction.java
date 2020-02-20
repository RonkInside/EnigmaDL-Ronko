package com.nttdata.enel.EnigmaInt;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class PostAction {
	protected static Logger logger = Logger.getLogger("com/nttdata/enel/EnigmaInt");

	public Object[] theRequest(CloseableHttpClient client, String ProxyHost, String ProxyPort, String destTableName, String postData, String returnValueField) throws HttpException, IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException {
		logger.info("++++++++++++++++++++ START POST ACTION Request ++++++++++++++++++++");
		Object[] theValueRet = { "999", "", Boolean.valueOf(true), "" };
		CloseableHttpResponse responsePost = null;
		try {
			HttpPost httppost = new HttpPost(
					"https://" + ProxyHost + ":" + ProxyPort + "/api/now/v2/table/" + destTableName);
			httppost.setHeader("Accept", "application/json");
			httppost.setHeader("Content-Type", "application/json");
			logger.info("POST ACTION - Table: " + destTableName + " - Request: " + postData);
			httppost.setEntity((HttpEntity)new ByteArrayEntity(postData.getBytes("utf-8")));
			logger.debug("POST ACTION - RequestLine:" + httppost.getRequestLine());
			responsePost = client.execute((HttpUriRequest)httppost);
			StatusLine statusLinePost = responsePost.getStatusLine();
			int StatusCodeInt = statusLinePost.getStatusCode();
			theValueRet[0] = Integer.valueOf(StatusCodeInt);
			logger.info("POST ACTION - Status Line: " + statusLinePost + " - Status Code:" + StatusCodeInt);
			String responsePostBody = EntityUtils.toString(responsePost.getEntity());
			if (StatusCodeInt < 300) {
				JSONObject obj = new JSONObject(responsePostBody);
				theValueRet[1] = obj.getJSONObject("result").getString(returnValueField);
				logger.info("POST ACTION - Body: " + theValueRet[1]);
			} else {
				logger.info("POST ACTION - Body (Error): " + responsePostBody);
				theValueRet[2] = Boolean.valueOf(false);
				theValueRet[3] = responsePostBody;
			}
			responsePost.close();
		} catch (Exception e) {
			theValueRet[0] = Integer.valueOf(999);
			theValueRet[1] = "";
			theValueRet[2] = Boolean.valueOf(false);
			theValueRet[3] = "Got Exception in the Get Action";
			logger.error(theValueRet[3], e);
		} finally {
			responsePost.close();
		}
		logger.info("++++++++++++++++++++ End Post Request ++++++++++++++++++++");
		return theValueRet;
	}
}
