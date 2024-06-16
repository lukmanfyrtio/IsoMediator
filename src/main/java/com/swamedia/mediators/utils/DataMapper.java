package com.swamedia.mediators.utils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class DataMapper {
    public static Map<String, String> mapData(JsonNode request, Map<Integer, String> iso8583Data) {
        Map<String, String> iso20022Data = new HashMap<>();

        JsonNode dataMapping = request.get("dataMapping");
        if (dataMapping != null && dataMapping.isArray()) {
            Iterator<JsonNode> elements = dataMapping.elements();

            while (elements.hasNext()) {
                JsonNode mapping = elements.next();
                JsonNode iso8583KeyNode = mapping.get("iso8583DataElementId");
                JsonNode iso20022KeyNode = mapping.get("iso20022Xml");

                if (iso8583KeyNode != null && iso20022KeyNode != null) {
                    Integer iso8583Key = iso8583KeyNode.asInt();
                    String iso20022Key = iso20022KeyNode.asText();

                    String iso8583Value = iso8583Data.get(iso8583Key);
                    if (iso8583Value != null) {
                        iso20022Data.put(iso20022Key, iso8583Value);
                    }
                }
            }
        }

        return iso20022Data;
    }
}
