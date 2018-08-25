package cz.cvut.isctripregistrator;

import java.util.HashMap;
import java.util.Map;

import com.example.isctripregistrator.R;

public enum QueryError {
    ERR_AUTHENTICATION("AUTH", R.string.error_authentication),
    ERR_DATABASE("DB", R.string.error_database),
    ERR_INTERNAL("INTERNAL", R.string.error_internal),
    ERR_CARD("CARD", R.string.error_card),
    ERR_CONNECTION("CONNECTION", R.string.error_connection);

    private static final Map<String, QueryError> typeMap;
    private final String type;
    private final int messageResourceId;

    static {
        typeMap = new HashMap<>();
        for (QueryError e : values()) {
            typeMap.put(e.type, e);
        }
    }

    QueryError(String type, int message) {
        this.type = type;
        this.messageResourceId = message;
    }

    public int getMessageResourceId() {
        return messageResourceId;
    }

    public String getType() {
        return type;
    }

    public static QueryError getError(String type) {
        QueryError error = typeMap.get(type);
        return error == null ? ERR_INTERNAL : error;
    }
}
