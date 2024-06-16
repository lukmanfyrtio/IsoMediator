package com.swamedia.mediators.utils;

import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;

public class ConvertISO8583ToISO20022 {
	private static final String HEADER = "ISO1987";

	// Create a method to convert IsoMessage to Map<Integer, String>
	public static LinkedHashMap<Integer, String> isoMessageToMap(IsoMessage isoMessage) {
		LinkedHashMap<Integer, String> fieldMap = new LinkedHashMap<>();

		// Iterate over all fields in the IsoMessage
		for (int i = 2; i <= 128; i++) { // Iterate through the standard range of ISO8583 fields
			IsoValue<?> isoValue = isoMessage.getField(i);
			if (isoValue != null && isoValue.getValue() != null) {
				String value = isoValue.toString();
				fieldMap.put(i, value);
			}
		}

		return fieldMap;
	}

	public static void main(String[] args) throws IOException, ParseException {
		String jsonString = "{\r\n" + "  \"messageMapping\": {\r\n" + "    \"iso8583MessageCode\": \"xxxx\",\r\n"
				+ "    \"iso20022MessageCode\": \"MxPain00100103\"\r\n" + "  },\r\n" + "  \"dataMapping\": [\r\n"
				+ "    {\r\n" + "      \"iso8583DataElementId\": \"11\",\r\n"
				+ "      \"iso20022Xml\": \"GrpHdr.msgId\"\r\n" + "    },\r\n" + "    {\r\n"
				+ "      \"iso8583DataElementId\": \"7\",\r\n" + "      \"iso20022Xml\": \"GrpHdr.creDtTm\"\r\n"
				+ "    },\r\n" + "    {\r\n" + "      \"iso8583DataElementId\": \"78\",\r\n"
				+ "      \"iso20022Xml\": \"GrpHdr.nbOfTxs\"\r\n" + "    },\r\n" + "    {\r\n"
				+ "      \"iso8583DataElementId\": \"37\",\r\n" + "      \"iso20022Xml\": \"PmtInf.PmtInfId\"\r\n"
				+ "    },\r\n" + "    {\r\n" + "      \"iso8583DataElementId\": \"6\",\r\n"
				+ "      \"iso20022Xml\": \"PmtInf.PmtMtd\"\r\n" + "    },\r\n" + "    {\r\n"
				+ "      \"iso8583DataElementId\": \"13\",\r\n" + "      \"iso20022Xml\": \"PmtInf.ReqdExctnDt\"\r\n"
				+ "    },\r\n" + "    {\r\n" + "      \"iso8583DataElementId\": \"32\",\r\n"
				+ "      \"iso20022Xml\": \"DbtrAcct.IBAN\"\r\n" + "    },\r\n" + "    {\r\n"
				+ "      \"iso8583DataElementId\": \"102\",\r\n" + "      \"iso20022Xml\": \"DbtrAgt.BIC\"\r\n"
				+ "    },\r\n" + "    {\r\n" + "      \"iso8583DataElementId\": \"104\",\r\n"
				+ "      \"iso20022Xml\": \"CdtTrfTxInf.PmtId.EndToEndId\"\r\n" + "    },\r\n" + "    {\r\n"
				+ "      \"iso8583DataElementId\": \"49\",\r\n"
				+ "      \"iso20022Xml\": \"CdtTrfTxInf.Amt.InstdAmt.Ccy\"\r\n" + "    },\r\n" + "    {\r\n"
				+ "      \"iso8583DataElementId\": \"86\",\r\n"
				+ "      \"iso20022Xml\": \"CdtTrfTxInf.Amt.InstdAmt.Value\"\r\n" + "    }\r\n" + "  ]\r\n" + "}\r\n"
				+ "";

		// Sample ISO 8583 data
		byte[] messageStream = "ISO198702008628000108008000000404000500000003CHK230612061234562306121112345678901062306120384010123456789010992929292915123456789012345109929292929"
				.getBytes();
		MessageFactory<IsoMessage> messageFactory = ConfigParser.createFromClasspathConfig("fields.xml");
		// reading ISO8583 message came through TCP/IP connection. So the message is
		// coming as a byte stream
		IsoMessage receivedIsoMessage = messageFactory.parseMessage(messageStream, HEADER.length());

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

			// Output the generated ISO 20022 message
			System.out.println(iso20022Message);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}
	}

}
