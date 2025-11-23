/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author USER
 */
public class PasswordUtil {
    /**
     * Genera el hash BCrypt de una contraseña.
     * @param password Contraseña en texto plano.
     * @return Hash seguro con salt incorporado.
     */
    public static String hashPassword(String password) {
        try {
            return BCrypt.hashpw(password, BCrypt.gensalt(12)); // 12 rondas de hashing
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash de contraseña", e);
        }
    }

    /**
     * Verifica una contraseña en texto plano contra su hash BCrypt.
     * @param plainPassword Contraseña ingresada por el usuario.
     * @param hashedPassword Hash almacenado en base de datos.
     * @return true si coincide, false en caso contrario.
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false; // Si el hash está corrupto o en formato distinto
        }
    }
}
