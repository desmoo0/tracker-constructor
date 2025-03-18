package http;

public enum HttpStatus {
    OK("200"),
    CREATED("201"),
    NOT_FOUND("404"),
    METHOD_NOT_ALLOWED("405"),
    NOT_ACCEPTABLE("406"),
    INTERNAL_SERVER_ERROR("500");

    HttpStatus(String status) {
        this.status = status;
    }

    private final String status;

    public String getCode() {
        return status;
    }
}