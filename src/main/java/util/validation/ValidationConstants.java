package util.validation;

import jakarta.validation.constraints.*;

/**
 * Validaciones centralizadas para entidades.
 * Utiliza Jakarta Bean Validation para mantener la integridad de datos.
 */
public class ValidationConstants {

    public static class PassengerValidation {
        @NotBlank(message = "El nombre del pasajero es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        public String firstName;

        @NotBlank(message = "El apellido del pasajero es obligatorio")
        @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
        public String lastName;

        @NotBlank(message = "El tipo de documento es obligatorio")
        @Pattern(regexp = "(DNI|PASSPORT|CEDULA)", message = "Tipo de documento inválido")
        public String docType;

        @NotBlank(message = "El número de documento es obligatorio")
        @Pattern(regexp = "^[A-Z0-9]{6,50}$", message = "Número de documento inválido")
        public String docNumber;

        @Email(message = "Email inválido")
        public String email;

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Teléfono inválido")
        public String phone;
    }

    public static class ReservationValidation {
        @NotBlank(message = "El PNR es obligatorio")
        @Pattern(regexp = "^[A-Z0-9]{6}$", message = "El PNR debe tener 6 caracteres alfanuméricos")
        public String pnr;

        @NotNull(message = "El ID de pasajero es obligatorio")
        @Positive(message = "El ID de pasajero debe ser positivo")
        public Integer passengerId;

        @NotNull(message = "El ID de vuelo es obligatorio")
        @Positive(message = "El ID de vuelo debe ser positivo")
        public Integer flightId;

        @Pattern(regexp = "(Booked|Cancelled)", message = "Estado de reserva inválido")
        public String status;
    }

    public static class UserValidation {
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 100, message = "El usuario debe tener entre 4 y 100 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._@-]+$", message = "El usuario contiene caracteres no permitidos")
        public String username;

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 200, message = "La contraseña debe tener entre 8 y 200 caracteres")
        public String password;

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        public String fullName;

        @Email(message = "Email inválido")
        public String email;

        @NotNull(message = "El rol es obligatorio")
        @Positive(message = "El ID de rol debe ser positivo")
        public Integer roleId;
    }

    public static class BaggageValidation {
        @NotNull(message = "El peso del equipaje es obligatorio")
        @DecimalMin(value = "0.1", message = "El peso debe ser mayor a 0.1 kg")
        @DecimalMax(value = "150.0", message = "El peso no puede exceder 150 kg")
        public Double weight;

        @Positive(message = "La cantidad de piezas debe ser positiva")
        @Max(value = 10, message = "No se permiten más de 10 piezas de equipaje")
        public Integer pieces;

        @NotBlank(message = "El tipo de equipaje es obligatorio")
        @Pattern(regexp = "(Carry-on|Checked|Special)", message = "Tipo de equipaje inválido")
        public String type;
    }

    public static class FlightValidation {
        @NotBlank(message = "El número de vuelo es obligatorio")
        @Pattern(regexp = "^[A-Z]{2}[0-9]{4}$", message = "Formato de número de vuelo inválido (ej: AA1234)")
        public String flightNumber;

        @NotNull(message = "La capacidad del vuelo es obligatoria")
        @Positive(message = "La capacidad debe ser positiva")
        public Integer capacity;

        @Pattern(regexp = "(Scheduled|Delayed|Cancelled|Closed)", message = "Estado de vuelo inválido")
        public String status;
    }
}
