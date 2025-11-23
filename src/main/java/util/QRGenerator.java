/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 *
 * @author USER
 */
public class QRGenerator {
    /**
     * Genera un código QR con el texto indicado y lo guarda como archivo PNG.
     * @param text Texto o URL que contendrá el código QR
     * @param fileName Nombre del archivo (ejemplo: "boarding_ABC123.png")
     * @return Archivo generado (ruta absoluta)
     */
    public static File generateQRCode(String text, String fileName) {
        int width = 250;
        int height = 250;
        String filePath = "temp/" + fileName;

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            Path path = FileSystems.getDefault().getPath(filePath);
            File dir = new File("temp");
            if (!dir.exists()) dir.mkdirs();

            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            return new File(filePath);

        } catch (WriterException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
