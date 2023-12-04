package BitCoinDemo;

import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

class ParameterDecoder {
    static String getParam(List<AbstractMap.SimpleEntry<String, String>> params, String param) {
        String value = null;
        for (AbstractMap.SimpleEntry<String, String> entry : params) {
            if (entry.getKey().equals(param)) {
                value = entry.getValue();
                break;
            }
        }
        if (value == null) return "";
        return value;
    }

    static List<AbstractMap.SimpleEntry<String, String>> getParams(URI requestURI) {
        String[] params = requestURI.toString().split("\\?")[1].split("&");
        List<AbstractMap.SimpleEntry<String, String>> result = new ArrayList<>();
        for (String param : params) {
            if (param.split("=").length == 2) {
                result.add(new AbstractMap.SimpleEntry<>(param.split("=")[0], param.split("=")[1]));
            }
        }
        return result;
    }
}
