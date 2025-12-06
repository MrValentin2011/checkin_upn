/*
 * Modelo para preguntas frecuentes (FAQ)
 */
package model;

import java.time.LocalDateTime;

/**
 * Representa una pregunta frecuente en la base de conocimiento del sistema.
 * Incluye búsqueda, categorización y gestión de versiones.
 */
public class FAQ {
    private int faqId;
    private String category;      // Categoría: CHECKIN, FLIGHT, BAGGAGE, PAYMENT, SYSTEM, etc.
    private String question;
    private String answer;
    private String keywords;      // Palabras clave separadas por comas para búsqueda
    private int helpfulYes;       // Contador de "fue útil"
    private int helpfulNo;        // Contador de "no fue útil"
    private int views;            // Contador de visualizaciones
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;       // Si está disponible para usuarios

    // Constructores
    public FAQ() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public FAQ(String category, String question, String answer, String keywords) {
        this();
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.keywords = keywords;
    }

    // Getters y Setters
    public int getFaqId() { return faqId; }
    public void setFaqId(int faqId) { this.faqId = faqId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public int getHelpfulYes() { return helpfulYes; }
    public void setHelpfulYes(int helpfulYes) { this.helpfulYes = helpfulYes; }

    public int getHelpfulNo() { return helpfulNo; }
    public void setHelpfulNo(int helpfulNo) { this.helpfulNo = helpfulNo; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * Calcula la utilidad (porcentaje de respuestas positivas)
     */
    public double getHelpfulnessRatio() {
        int total = helpfulYes + helpfulNo;
        if (total == 0) return 0;
        return ((double) helpfulYes / total) * 100;
    }

    @Override
    public String toString() {
        return String.format("%s | %s", category, question);
    }
}
