package com.ecommerce.inventoryservice.dto;

import java.util.List;

public class BulkUpdateResponse {

    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<UpdateResult> results;

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<UpdateResult> getResults() {
        return results;
    }

    public void setResults(List<UpdateResult> results) {
        this.results = results;
    }
}