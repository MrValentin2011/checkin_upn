/*
 * Servicio de FAQ
 */
package service.impl;

import dao.impl.FAQDao;
import model.FAQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Servicio para gestión de preguntas frecuentes.
 * Maneja búsqueda, categorización y estadísticas de FAQ.
 */
public class FAQService {
    private static final Logger logger = LoggerFactory.getLogger(FAQService.class);
    private static FAQService instance;
    private final FAQDao dao;

    private FAQService() {
        this.dao = new FAQDao();
        FAQDao.createTableIfNotExists();
    }

    /**
     * Obtiene la instancia singleton
     */
    public static synchronized FAQService getInstance() {
        if (instance == null) {
            instance = new FAQService();
        }
        return instance;
    }

    /**
     * Crea una nueva FAQ
     */
    public FAQ createFAQ(String category, String question, String answer, String keywords) {
        FAQ faq = new FAQ(category, question, answer, keywords);
        if (dao.insert(faq)) {
            logger.info("FAQ creada: {} en categoría {}", faq.getFaqId(), category);
            return faq;
        }
        return null;
    }

    /**
     * Obtiene una FAQ por ID
     */
    public FAQ getFAQById(int faqId) {
        return dao.findById(faqId);
    }

    /**
     * Obtiene FAQs por categoría
     */
    public List<FAQ> getFAQsByCategory(String category) {
        return dao.getByCategory(category);
    }

    /**
     * Búsqueda de FAQs
     */
    public List<FAQ> searchFAQs(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return dao.search(query.trim());
    }

    /**
     * Obtiene todas las FAQs
     */
    public List<FAQ> getAllFAQs() {
        return dao.getAll();
    }

    /**
     * Obtiene las FAQs más vistas
     */
    public List<FAQ> getPopularFAQs(int limit) {
        return dao.getTopViewed(limit);
    }

    /**
     * Obtiene las FAQs más útiles
     */
    public List<FAQ> getMostHelpfulFAQs(int limit) {
        return dao.getMostHelpful(limit);
    }

    /**
     * Obtiene las categorías disponibles
     */
    public Set<String> getCategories() {
        return dao.getCategories();
    }

    /**
     * Marca una FAQ como útil
     */
    public void markFAQHelpful(int faqId) {
        dao.markHelpful(faqId);
    }

    /**
     * Marca una FAQ como no útil
     */
    public void markFAQNotHelpful(int faqId) {
        dao.markNotHelpful(faqId);
    }

    /**
     * Inicializa FAQs de demostración
     */
    public void initializeDefaultFAQs() {
        if (getAllFAQs().isEmpty()) {
            createFAQ("CHECKIN", "¿Cómo hago mi check-in?",
                "Para hacer check-in, ingresa tu número de reserva, pasaporte y boleto en el sistema. " +
                "El sistema verificará tu información y completará el check-in automáticamente.",
                "check-in, reserva, boleto, pasaporte");

            createFAQ("CHECKIN", "¿Puedo hacer check-in en línea?",
                "Sí, puedes hacer check-in en línea accediendo al portal web con tu número de reserva. " +
                "También puedes usar esta aplicación en los mostradores del aeropuerto.",
                "check-in online, web, portal, remoto");

            createFAQ("BAGGAGE", "¿Cuánto equipaje puedo llevar?",
                "Esto depende de tu categoría de boleto y aerolínea. Por lo general, tienes derecho a llevar un bolso personal " +
                "y una maleta en la cabina. El equipaje facturado varía según tu clase de vuelo.",
                "equipaje, maleta, bolsa, limite, permitido");

            createFAQ("BAGGAGE", "¿Qué ocurre si mi equipaje excede el límite?",
                "Si tu equipaje excede el límite permitido, deberás pagar una tarifa adicional. " +
                "Las tarifas varían según el peso excedente y la aerolínea.",
                "equipaje extra, tarifa, sobrepeso, exceso");

            createFAQ("FLIGHT", "¿Qué hora debo llegar al aeropuerto?",
                "Se recomienda llegar con al menos 2 horas de anticipación para vuelos nacionales " +
                "y 3 horas para vuelos internacionales.",
                "hora, llegada, aeropuerto, anticipacion, vuelo");

            createFAQ("FLIGHT", "¿Puedo cambiar mi vuelo después del check-in?",
                "Los cambios después del check-in dependen de tu boleto y la aerolínea. " +
                "Comunícate con el mostrador de la aerolínea o el personal de asistencia.",
                "cambiar vuelo, reprogramar, modificar, alter");

            createFAQ("SYSTEM", "¿Olvidé mi contraseña, qué hago?",
                "Puedes recuperar tu contraseña utilizando la opción '¿Olvidaste tu contraseña?' en la pantalla de login. " +
                "Recibirás un enlace de recuperación en tu correo electrónico.",
                "contraseña, olvide, recuperar, password, reset");

            createFAQ("SYSTEM", "¿Cómo contacto con soporte técnico?",
                "Puedes contactar con nuestro equipo de soporte a través del panel de ayuda en la aplicación, " +
                "o enviando un correo a soporte@aerocheck.com",
                "soporte, ayuda, contacto, tecnico, problema");

            logger.info("FAQs de demostración inicializadas");
        }
    }
}
