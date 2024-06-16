package com.swamedia.mediators;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;
import com.swamedia.mediators.utils.DataMapper;
import com.swamedia.mediators.utils.Iso20022MessageGenerator;
import com.swamedia.mediators.utils.JsonRequestParser;

public class ISO20022Converter extends AbstractMediator {

	private static final String HEADER = "ISO1987";
	private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	private static final String XML_CONTENT_TYPE = "application/xml";
	private Logger logger = LoggerFactory.getLogger(ISO20022Converter.class);

	@Override
	public boolean mediate(org.apache.synapse.MessageContext synCtx) {
		MessageContext axis2MessageContext = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
		String jsonString = JsonUtil.jsonPayloadToString(axis2MessageContext);
		JSONObject jsonObject = new JSONObject(jsonString);
		logger.info("ISO8583 Message : \n : {}", jsonObject.getString("iso8583Message"));
		byte[] iso8583Msg = jsonObject.getString("iso8583Message").getBytes();
		try {

			URL resource = getClass().getClassLoader().getResource("fields.xml");
			if (resource == null) {
				throw new IllegalArgumentException("file not found!");
			} else {

				logger.info("File ada bro baru");
			}
			MessageFactory<IsoMessage> messageFactory = ConfigParser.createFromClasspathConfig(getClass().getClassLoader(), "fields.xml");
			// reading ISO8583 message came through TCP/IP connection. So the message is
			// coming as a byte stream

			IsoMessage receivedIsoMessage = messageFactory.parseMessage(iso8583Msg, HEADER.length());

			System.out.println("\nReceived ISO8583 message:\n" + new String(receivedIsoMessage.writeData()));

			System.out.println("\nHeader: " + receivedIsoMessage.getIsoHeader());
			System.out.println("Data Elements\n-------------");
			Map<Integer, String> iso8583Data = isoMessageToMap(receivedIsoMessage);

			try {
				// Parse the JSON request
				JsonNode request = JsonRequestParser.parseRequest(jsonString);

				// Get the ISO 20022 message type from the request
				JsonNode messageMappingCodeNode = request.get("messageMapping");
				if (messageMappingCodeNode == null) {
					throw new IllegalArgumentException("Invalid request: Missing messageMapping");
				}
				JsonNode messageTypeNode = messageMappingCodeNode.get("iso20022MessageCode");
				if (messageTypeNode == null) {
					throw new IllegalArgumentException("Invalid request: Missing iso20022MessageCode");
				}
				String messageType = messageTypeNode.asText();

				// Map ISO 8583 data to ISO 20022 data
				Map<String, String> iso20022Data = DataMapper.mapData(request, iso8583Data);

				// Generate the ISO 20022 message
				String iso20022Message = Iso20022MessageGenerator.generateIso20022Message(messageType, iso20022Data);

				try {
					JsonUtil.removeJsonPayload(axis2MessageContext);
					OMElement omXML = convertStringToOM(iso20022Message);
					axis2MessageContext.getEnvelope().getBody().addChild(omXML.getFirstElement());
				} catch (XMLStreamException e) {
					logger.error(e.getMessage());
					handleException("Error creating SOAP Envelope from source " + iso20022Message, synCtx);
				}
				org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
				a2mc.setProperty(Constants.Configuration.MESSAGE_TYPE, XML_CONTENT_TYPE);
				a2mc.setProperty(Constants.Configuration.CONTENT_TYPE, XML_CONTENT_TYPE);
				handleSpecialProperties(XML_CONTENT_TYPE, a2mc);
				// Output the generated ISO 20022 message
				System.out.println(iso20022Message);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return true;
	}

	private OMElement convertStringToOM(String value) throws XMLStreamException, OMException {
		javax.xml.stream.XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(new StringReader(value));
		StAXBuilder builder = new StAXOMBuilder(xmlReader);
		return builder.getDocumentElement();
	}

	public static LinkedHashMap<Integer, String> isoMessageToMap(IsoMessage isoMessage) {
		LinkedHashMap<Integer, String> fieldMap = new LinkedHashMap<>();

		// Iterate over all fields in the IsoMessage
		for (int i = 2; i <= 128; i++) { // Iterate through the standard range of ISO8583 fields
			IsoValue<?> isoValue = isoMessage.getField(i);
			if (isoValue != null && isoValue.getValue() != null) {
				String value = isoValue.toString();
				fieldMap.put(i, value);
				printIsoField(isoMessage, i);
			}
		}

		return fieldMap;
	}

	private static void printIsoField(IsoMessage isoMessage, int fieldNumber) {
		IsoValue<Object> isoValue = isoMessage.getField(fieldNumber);
		System.out.println(
				fieldNumber + " : " + isoValue.getType() + " : " + isoValue.getLength() + " : " + isoValue.getValue());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void handleSpecialProperties(Object resultValue, org.apache.axis2.context.MessageContext axis2MessageCtx) {

		axis2MessageCtx.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, resultValue);
		Object o = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
		Map headers = (Map) o;
		if (headers != null) {
			headers.remove(HTTP.CONTENT_TYPE);
			headers.put(HTTP.CONTENT_TYPE, resultValue);
		}
	}
}
