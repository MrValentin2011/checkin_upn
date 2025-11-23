package util.exception;

/**
 * Excepción personalizada para operaciones de check-in.
 * Permite distinguir errores de dominio de otros errores.
 */
public class CheckInException extends Exception {
    private String errorCode;
    private Object context;

    public CheckInException(String message) {
        super(message);
        this.errorCode = "GENERIC_ERROR";
    }

    public CheckInException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERIC_ERROR";
    }

    public CheckInException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CheckInException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public CheckInException(String message, String errorCode, Object context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "CheckInException{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                ", context=" + context +
                '}';
    }
}
