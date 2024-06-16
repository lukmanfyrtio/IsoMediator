package com.swamedia.mediators.utils;import java.util.LinkedHashMap;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;

public class ISO8583Generator {

    private static final String HEADER = "ISO1987";

    public static void main(String[] args) throws Exception {
        // create message factory with default settings
        MessageFactory<IsoMessage> messageFactory = ConfigParser.createDefault();

        // creating iso message with MTI as financial
        IsoMessage isoMessage = messageFactory.newMessage(0x200);

        // set header
        isoMessage.setIsoHeader(HEADER);

        // set data fields
        isoMessage.setValue(11, "123456", IsoType.LLVAR, 6);
        isoMessage.setValue(7, "230612", IsoType.DATE6, 6);
        isoMessage.setValue(78, "1234567890", IsoType.LLVAR, 10);
        isoMessage.setValue(37, "230612", IsoType.LLVAR, 6);
        isoMessage.setValue(6, "CHK", IsoType.LLVAR, 3);
        isoMessage.setValue(13, "230612", IsoType.DATE6, 6);
        isoMessage.setValue(32, "12345678901", IsoType.LLVAR, 11);
        isoMessage.setValue(102, "123456789012345", IsoType.LLVAR, 15);
        isoMessage.setValue(104, "9929292929", IsoType.LLVAR, 10);
        isoMessage.setValue(49, "840", IsoType.LLVAR, 3);
        isoMessage.setValue(86, "9929292929", IsoType.LLVAR, 10);

        System.out.println("New ISO8583 message:\n" + new String(isoMessage.writeData()));

        readIsoMessage(isoMessage.writeData());

    }

    private static void readIsoMessage(byte[] messageStream) throws Exception {
        // create message factory with data element specification
        MessageFactory<IsoMessage> messageFactory = ConfigParser.createFromClasspathConfig("fields.xml");

        // reading ISO8583 message came through TCP/IP connection. So the message is coming as a byte stream
        IsoMessage receivedIsoMessage = messageFactory.parseMessage(messageStream, HEADER.length());
        

        System.out.println("\nReceived ISO8583 message:\n" + new String(receivedIsoMessage.writeData()));

        System.out.println("\nHeader: " + receivedIsoMessage.getIsoHeader());
        System.out.println("Data Elements\n-------------");
        printIsoField(receivedIsoMessage, 11);
        printIsoField(receivedIsoMessage, 7);
        printIsoField(receivedIsoMessage, 6);
        printIsoField(receivedIsoMessage, 49);
        printIsoField(receivedIsoMessage, 32);
    }
    
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

    private static void printIsoField(IsoMessage isoMessage, int fieldNumber) {
        IsoValue<Object> isoValue = isoMessage.getField(fieldNumber);
        System.out.println(fieldNumber + " : " + isoValue.getType() + " : " + isoValue.getLength() + " : " + isoValue.getValue());
    }

}