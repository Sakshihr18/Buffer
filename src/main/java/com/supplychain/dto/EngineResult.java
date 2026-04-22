package com.supplychain.dto;

import java.util.List;
import java.util.Map;

public class EngineResult {

    private String engineName;
    private boolean success;
    private String message;
    private Map<String, Object> data;
    private List<Map<String, Object>> results;
    private long timestamp;

    public EngineResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public static EngineResult success(String engineName, String message) {
        EngineResult result = new EngineResult();
        result.engineName = engineName;
        result.success = true;
        result.message = message;
        return result;
    }

    public static EngineResult success(String engineName, String message, Map<String, Object> data) {
        EngineResult result = success(engineName, message);
        result.data = data;
        return result;
    }

    public static EngineResult error(String engineName, String message) {
        EngineResult result = new EngineResult();
        result.engineName = engineName;
        result.success = false;
        result.message = message;
        return result;
    }

    public String getEngineName() { return engineName; }
    public void setEngineName(String engineName) { this.engineName = engineName; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public List<Map<String, Object>> getResults() { return results; }
    public void setResults(List<Map<String, Object>> results) { this.results = results; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}