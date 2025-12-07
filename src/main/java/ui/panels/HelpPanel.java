/*
 * Panel de ayuda y soporte
 */
package ui.panels;

import model.FAQ;
import service.impl.FAQService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

/**
 * Panel de ayuda e información.
 * Incluye navegador de FAQ, búsqueda, categorías y contacto de soporte.
 */
public class HelpPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(HelpPanel.class);

    private final FAQService faqService;
    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JList<FAQ> faqList;
    private JEditorPane answerPane;
    private JLabel helpfulLabel;
    private int currentFaqId = -1;

    public HelpPanel() {
        this.faqService = FAQService.getInstance();
        faqService.initializeDefaultFAQs(); // Inicializar FAQs si es necesario
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Panel superior: búsqueda y filtros
        add(createSearchPanel(), BorderLayout.NORTH);

        // Panel central: lista de FAQs y respuesta
        add(createMainPanel(), BorderLayout.CENTER);

        // Panel inferior: información de contacto y utilidad
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Búsqueda
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("🔍 Buscar:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        searchField = new JTextField();
        searchField.addActionListener(e -> performSearch());
        panel.add(searchField, gbc);

        // Categoría
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("Categoría:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.2;
        categoryCombo = new JComboBox<>();
        categoryCombo.addItem("Todas");
        for (String cat : faqService.getCategories()) {
            categoryCombo.addItem(cat);
        }
        categoryCombo.addActionListener(e -> updateFAQList());
        panel.add(categoryCombo, gbc);

        // Botón de búsqueda
        gbc.gridx = 4;
        gbc.weightx = 0;
        JButton btnSearch = new JButton("Buscar");
        btnSearch.addActionListener(e -> performSearch());
        panel.add(btnSearch, gbc);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Panel izquierdo: lista de FAQs
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createTitledBorder("Preguntas Frecuentes"));

        faqList = new JList<>();
        faqList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showSelectedFAQ();
            }
        });
        JScrollPane scrollList = new JScrollPane(faqList);
        scrollList.setPreferredSize(new Dimension(300, 400));
        listPanel.add(scrollList, BorderLayout.CENTER);

        panel.add(listPanel, BorderLayout.WEST);

        // Panel derecho: respuesta
        JPanel answerPanel = new JPanel(new BorderLayout());
        answerPanel.setBorder(BorderFactory.createTitledBorder("Respuesta"));

        answerPane = new JEditorPane();
        answerPane.setContentType("text/html");
        answerPane.setEditable(false);
        HTMLEditorKit kit = new HTMLEditorKit();
        answerPane.setEditorKit(kit);
        JScrollPane scrollAnswer = new JScrollPane(answerPane);
        answerPanel.add(scrollAnswer, BorderLayout.CENTER);

        panel.add(answerPanel, BorderLayout.CENTER);

        // Cargar FAQs iniciales
        updateFAQList();

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(""));

        // Panel izquierdo: utilidad de la respuesta
        JPanel utilityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        utilityPanel.add(new JLabel("¿Fue útil esta respuesta?"));

        JButton btnYes = new JButton("👍 Sí");
        btnYes.addActionListener(e -> markHelpful(true));
        utilityPanel.add(btnYes);

        JButton btnNo = new JButton("👎 No");
        btnNo.addActionListener(e -> markHelpful(false));
        utilityPanel.add(btnNo);

        helpfulLabel = new JLabel("Gracias por tu feedback");
        helpfulLabel.setVisible(false);
        utilityPanel.add(helpfulLabel);

        panel.add(utilityPanel, BorderLayout.WEST);

        // Panel derecho: contacto de soporte
        JPanel contactPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        contactPanel.add(new JLabel("¿Aún tienes dudas?"));

        JButton btnContact = new JButton("📧 Contactar Soporte");
        btnContact.addActionListener(e -> showContactSupport());
        contactPanel.add(btnContact);

        JButton btnManual = new JButton("📄 Descargar Manual");
        btnManual.addActionListener(e -> downloadManual());
        contactPanel.add(btnManual);

        panel.add(contactPanel, BorderLayout.EAST);

        return panel;
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            updateFAQList();
            return;
        }

        List<FAQ> results = faqService.searchFAQs(query);
        updateFAQListModel(results);
    }

    private void updateFAQList() {
        String selectedCategory = (String) categoryCombo.getSelectedItem();

        List<FAQ> faqs;
        if ("Todas".equals(selectedCategory)) {
            faqs = faqService.getAllFAQs();
        } else {
            faqs = faqService.getFAQsByCategory(selectedCategory);
        }

        updateFAQListModel(faqs);
    }

    private void updateFAQListModel(List<FAQ> faqs) {
        DefaultListModel<FAQ> model = new DefaultListModel<>();
        for (FAQ faq : faqs) {
            model.addElement(faq);
        }
        faqList.setModel(model);
    }

    private void showPopularFAQs() {
        List<FAQ> popular = faqService.getPopularFAQs(20);
        updateFAQListModel(popular);
        searchField.setText("");
        categoryCombo.setSelectedItem("Todas");
    }

    private void showSelectedFAQ() {
        FAQ selected = faqList.getSelectedValue();
        if (selected != null) {
            currentFaqId = selected.getFaqId();
            FAQ fullFAQ = faqService.getFAQById(currentFaqId);

            if (fullFAQ != null) {
                updateHtmlAnswer(fullFAQ);
                helpfulLabel.setVisible(false);
            }
        }
    }

    private void markHelpful(boolean helpful) {
        if (currentFaqId > 0) {

            if (helpful) {
                faqService.markFAQHelpful(currentFaqId);
                helpfulLabel.setText("✓ Gracias, tu feedback nos ayuda a mejorar");
            } else {
                faqService.markFAQNotHelpful(currentFaqId);
                helpfulLabel.setText("✓ Gracias por tu feedback, intentaremos mejorar");
            }

            helpfulLabel.setVisible(true);

            // Refrescar inmediatamente usando el registro actualizado
            FAQ refreshed = faqService.getFAQById(currentFaqId);
            if (refreshed != null) {
                updateHtmlAnswer(refreshed);
            }

            // Ocultar mensaje después de 2s
            new javax.swing.Timer(2000, e -> helpfulLabel.setVisible(false))
                    .start();
        }
    }

    private void updateHtmlAnswer(FAQ faq) {
        String html = String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 10px; }
                        h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }
                        .category { color: #7f8c8d; font-size: 12px; }
                        .answer { margin-top: 15px; line-height: 1.6; }
                        .stats { margin-top: 20px; padding: 10px; background: #ecf0f1; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <div class="category">Categoría: %s</div>
                    <h1>%s</h1>
                    <div class="answer">%s</div>
                    <div class="stats">
                        <strong>Vistas:</strong> %d |
                        <strong>Útil:</strong> %d |
                        <strong>No útil:</strong> %d
                    </div>
                </body>
                </html>
                """,
                faq.getCategory(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getViews(),
                faq.getHelpfulYes(),
                faq.getHelpfulNo());

        answerPane.setText(html);
    }

    private void showContactSupport() {
        String message = """
                Para contactar con nuestro equipo de soporte:

                📧 Email: soporte@aerocheck.com
                📞 Teléfono: +1-800-AERO-HELP (1-800-237-6435)
                💬 Chat en vivo: Disponible de Lunes a Viernes, 8 AM - 6 PM
                🌐 Portal de soporte: https://support.aerocheck.com

                Tiempo de respuesta promedio:
                • Email: 24 horas
                • Chat: 5 minutos
                • Teléfono: 15 minutos
                """;

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 250));

        JOptionPane.showMessageDialog(this, scrollPane, "Contactar Soporte", JOptionPane.INFORMATION_MESSAGE);
    }

    private void downloadManual() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Descargar manual del usuario");
        chooser.setSelectedFile(new File("MANUAL DE USO.pdf"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF", "pdf"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();

            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".pdf");
            }

            try {
                generatePDF(selectedFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Manual descargado correctamente",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                logger.error("Error creando PDF", e);
                JOptionPane.showMessageDialog(this, "Error al generar PDF",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void generatePDF(String path) throws Exception {
        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(path));

        document.open();

        // Título elegante
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 22,
                com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("AEROCHECK - MANUAL DE USUARIO", titleFont);
        title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        document.add(title);

        document.add(new com.itextpdf.text.Paragraph("\n"));

        // Contenido general
        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16,
                com.itextpdf.text.Font.BOLD);

        com.itextpdf.text.Font textFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12);

        addSection(document, "1. Bienvenida",
                "Bienvenido al sistema AeroCheck. Este manual explica las funciones del sistema,\n" +
                        "incluyendo check-in, gestión de equipaje, vuelos, soporte técnico y más.",
                headerFont, textFont);

        addSection(document, "2. Inicio de sesión",
                "• Ingresa tu usuario y contraseña.\n" +
                        "• Usa '¿Olvidaste tu contraseña?' si necesitas recuperar el acceso.\n",
                headerFont, textFont);

        addSection(document, "3. Realizar Check-In",
                "• Ingresa los datos del pasajero.\n" +
                        "• Verifica la información.\n" +
                        "• El sistema generará tu boarding pass.\n",
                headerFont, textFont);

        addSection(document, "4. Gestión de Equipaje",
                "• Registra equipaje de mano y equipaje facturado.\n" +
                        "• El sistema validará peso y restricciones.\n",
                headerFont, textFont);

        addSection(document, "5. Consulta de vuelos",
                "• Revisa puertas de embarque, retrasos y horario actualizado.\n",
                headerFont, textFont);

        addSection(document, "6. Centro de Ayuda y FAQ",
                "• Explora preguntas frecuentes agrupadas por categoría.\n" +
                        "• Usa el buscador inteligente.\n",
                headerFont, textFont);

        addSection(document, "7. Valoración de respuestas (👍 / 👎)",
                "Puedes marcar si una respuesta fue útil. Esto mejora las recomendaciones.\n",
                headerFont, textFont);

        addSection(document, "8. Contacto de soporte",
                "Email: soporte@aerocheck.com\n" +
                        "Teléfono: 1-800-AERO-HELP\n" +
                        "Horario: Lunes a Viernes - 8 AM a 6 PM\n",
                headerFont, textFont);

        document.close();
    }

    private void addSection(com.itextpdf.text.Document doc, String title, String text,
            com.itextpdf.text.Font titleFont, com.itextpdf.text.Font textFont)
            throws com.itextpdf.text.DocumentException {

        com.itextpdf.text.Paragraph t = new com.itextpdf.text.Paragraph(title, titleFont);
        t.setSpacingBefore(10);
        doc.add(t);

        com.itextpdf.text.Paragraph p = new com.itextpdf.text.Paragraph(text, textFont);
        p.setSpacingAfter(10);
        doc.add(p);
    }

}
