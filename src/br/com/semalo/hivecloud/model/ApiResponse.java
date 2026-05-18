package br.com.semalo.hivecloud.model;

public class ApiResponse {

    public int status;
    public String body;

    public ApiResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }
}