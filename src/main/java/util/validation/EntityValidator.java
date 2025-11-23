package util.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Servicio centralizado de validación de datos.
 * Utiliza Jakarta Bean Validation para validar objetos.
 * Usa ParameterMessageInterpolator para evitar dependencias de EL.
 */
public class EntityValidator {
    private static final Logger logger = LoggerFactory.getLogger(EntityValidator.class);
    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();
        validator = factory.getValidator();
    }

    private EntityValidator() {
    }

    /**
     * Valida un objeto y retorna mensaje de error si hay validaciones fallidas.
     */
    public static ValidationResult validate(Object object) {
        if (object == null) {
            return new ValidationResult(false, "El objeto a validar no puede ser nulo");
        }

        Set<ConstraintViolation<Object>> violations = validator.validate(object);

        if (violations.isEmpty()) {
            return new ValidationResult(true, "");
        }

        StringBuilder errors = new StringBuilder();
        for (ConstraintViolation<Object> violation : violations) {
            errors.append(violation.getPropertyPath())
                  .append(": ")
                  .append(violation.getMessage())
                  .append("\n");
        }

        logger.warn("Validación fallida para {}: {}", object.getClass().getSimpleName(), errors.toString());
        return new ValidationResult(false, errors.toString().trim());
    }

    /**
     * Valida un pasajero.
     */
    public static ValidationResult validatePassenger(String firstName, String lastName, 
                                                      String docType, String docNumber) {
        if (firstName == null || firstName.isBlank()) {
            return new ValidationResult(false, "El nombre del pasajero es obligatorio");
        }
        if (firstName.length() < 2 || firstName.length() > 100) {
            return new ValidationResult(false, "El nombre debe tener entre 2 y 100 caracteres");
        }
        if (lastName == null || lastName.isBlank()) {
            return new ValidationResult(false, "El apellido del pasajero es obligatorio");
        }
        if (lastName.length() < 2 || lastName.length() > 100) {
            return new ValidationResult(false, "El apellido debe tener entre 2 y 100 caracteres");
        }
        if (docType == null || !docType.matches("(DNI|PASSPORT|CEDULA)")) {
            return new ValidationResult(false, "Tipo de documento inválido");
        }
        if (docNumber == null || !docNumber.matches("^[A-Z0-9]{6,50}$")) {
            return new ValidationResult(false, "Número de documento inválido");
        }
        return new ValidationResult(true, "");
    }

    /**
     * Valida PNR.
     */
    public static ValidationResult validatePNR(String pnr) {
        if (pnr == null || !pnr.matches("^[A-Z0-9]{6}$")) {
            return new ValidationResult(false, "El PNR debe tener 6 caracteres alfanuméricos");
        }
        return new ValidationResult(true, "");
    }

    /**
     * Valida código de asiento.
     */
    public static ValidationResult validateSeatCode(String seatCode) {
        if (seatCode == null || !seatCode.matches("^[0-9]{1,3}[A-Z]$")) {
            return new ValidationResult(false, "Código de asiento inválido (ej: 1A, 12B)");
        }
        return new ValidationResult(true, "");
    }

    /**
     * DTO para resultado de validación.
     */
    public static class ValidationResult {
        public boolean isValid;
        public String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return isValid ? "Validación exitosa" : "Error: " + errorMessage;
        }
    }
}
