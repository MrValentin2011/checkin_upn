/*
 * Gestor de sesión de usuario
 */
package config.app;

import model.User;

/**
 * Gestor singleton de sesión del usuario actual.
 * Almacena información del usuario autenticado para acceso en toda la aplicación.
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {
    }

    /**
     * Obtiene la instancia singleton
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Establece el usuario actual
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Obtiene el usuario actual
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Obtiene el ID del usuario actual
     */
    public int getCurrentUserId() {
        if (currentUser != null) {
            return currentUser.getId();
        }
        return 0;
    }

    /**
     * Obtiene el nombre del usuario actual
     */
    public String getCurrentUsername() {
        if (currentUser != null) {
            return currentUser.getUsername();
        }
        return "Desconocido";
    }

    /**
     * Verifica si hay usuario autenticado
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Limpia la sesión (cierre de sesión)
     */
    public void clearSession() {
        currentUser = null;
    }
}
