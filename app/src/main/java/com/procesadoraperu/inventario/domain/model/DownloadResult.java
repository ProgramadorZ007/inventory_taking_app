package com.procesadoraperu.inventario.domain.model;

/**
 * Representa el resultado de una descarga de catálogo de productos.
 * Utiliza factory methods estáticos para construcción.
 */
public class DownloadResult {

    public enum Status { SUCCESS, ERROR, EMPTY }

    private final Status status;
    private final int productCount;
    private final String errorMessage;

    private DownloadResult(Status status, int productCount, String errorMessage) {
        this.status = status;
        this.productCount = productCount;
        this.errorMessage = errorMessage;
    }

    public static DownloadResult success(int count) {
        return new DownloadResult(Status.SUCCESS, count, null);
    }

    public static DownloadResult error(String message) {
        return new DownloadResult(Status.ERROR, 0, message);
    }

    public static DownloadResult empty() {
        return new DownloadResult(Status.EMPTY, 0, null);
    }

    public Status getStatus() {
        return status;
    }

    public int getProductCount() {
        return productCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
