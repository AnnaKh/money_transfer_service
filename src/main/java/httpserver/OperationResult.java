package httpserver;

/**
 * Callback class for HttpServer
 */
public class OperationResult {

    private final boolean isError;
    private final String text;

    private OperationResult(boolean isError, String text) {
        this.text = text;
        this.isError = isError;
    }

    public static OperationResult success() {
        return new OperationResult(false, "Operation completed");
    }

    public static OperationResult success(String text) {
        return new OperationResult(false, text);
    }

    public static OperationResult error(String text) {
        return new OperationResult(true, text);
    }

    public boolean isError() {
        return isError;
    }

    public String getText() {
        return text;
    }
}
