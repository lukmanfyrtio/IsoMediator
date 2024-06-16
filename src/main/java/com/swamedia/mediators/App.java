package com.swamedia.mediators;

import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONObject;

/**
 * Hello world!
 *
 */
public class App extends AbstractMediator {
	public static void main(String[] args) {
		System.out.println("Hello World!");
	}

	@Override
	public boolean mediate(MessageContext synCtx) {
		org.apache.axis2.context.MessageContext axis2MessageContext = ((org.apache.synapse.core.axis2.Axis2MessageContext) synCtx)
				.getAxis2MessageContext();
		String jsonString = JsonUtil.jsonPayloadToString(axis2MessageContext);
		JSONObject jsonObject = new JSONObject(jsonString);
		byte[] iso8583Msg = jsonObject.getString("iso8583Message").getBytes();
		return false;
	}
}
