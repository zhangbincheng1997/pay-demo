package com.example.demo.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PayUtils {

    public static Map<String, String> requestToMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap();
        Map<String, String[]> requestParams = request.getParameterMap();
        Iterator iter = requestParams.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; ++i) {
                valueStr = i == values.length - 1 ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        return params;
    }
}
