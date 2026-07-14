package com.tramites.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

/**
 * Servicio local de conversión de documentos a formato SFDT de Syncfusion.
 * Evita la dependencia de servicios externos de Syncfusion.
 */
@Slf4j
@RestController
@RequestMapping("/api/document-editor")
public class DocumentEditorServiceController {

    @PostMapping(value = "/Import", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> importDocument(@RequestParam("files") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("{\"error\":\"No se recibio ningun archivo\"}");
            }

            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";

            if (filename.toLowerCase().endsWith(".docx")) {
                String sfdt = convertDocxToSfdt(file.getInputStream());
                return ResponseEntity.ok(sfdt);
            } else {
                String text = new String(file.getBytes());
                String sfdt = buildSfdtFromText(text);
                return ResponseEntity.ok(sfdt);
            }

        } catch (Exception e) {
            log.error("Error al convertir documento a SFDT: {}", e.getMessage(), e);
            String safeMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'").replace("\\", "/") : "unknown";
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + safeMsg + "\"}");
        }
    }

    private String convertDocxToSfdt(InputStream is) throws Exception {
        try (XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder blocksJson = new StringBuilder();
            boolean firstBlock = true;

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (!firstBlock) blocksJson.append(",");
                firstBlock = false;

                String alignStr = "Left";
                if (paragraph.getAlignment() != null) {
                    String aName = paragraph.getAlignment().name();
                    switch (aName) {
                        case "CENTER" -> alignStr = "Center";
                        case "RIGHT" -> alignStr = "Right";
                        case "BOTH" -> alignStr = "Justify";
                        default -> alignStr = "Left";
                    }
                }

                StringBuilder inlinesJson = new StringBuilder();
                boolean firstInline = true;
                boolean hasContent = false;

                for (XWPFRun run : paragraph.getRuns()) {
                    String text = run.getText(0);
                    if (text != null && !text.isEmpty()) {
                        hasContent = true;
                        if (!firstInline) inlinesJson.append(",");
                        firstInline = false;

                        StringBuilder charFmt = new StringBuilder("{");
                        boolean firstFmt = true;
                        if (run.isBold()) { charFmt.append("\"bold\":true"); firstFmt = false; }
                        if (run.isItalic()) { if (!firstFmt) charFmt.append(","); charFmt.append("\"italic\":true"); firstFmt = false; }
                        if (run.getUnderline() != null && run.getUnderline() != UnderlinePatterns.NONE) {
                            if (!firstFmt) charFmt.append(",");
                            charFmt.append("\"underline\":\"Single\"");
                            firstFmt = false;
                        }
                        if (run.getFontSize() > 0) {
                            if (!firstFmt) charFmt.append(",");
                            charFmt.append("\"fontSize\":").append((double) run.getFontSize());
                            firstFmt = false;
                        }
                        if (run.getFontFamily() != null) {
                            if (!firstFmt) charFmt.append(",");
                            charFmt.append("\"fontFamily\":\"").append(escapeJson(run.getFontFamily())).append("\"");
                        }
                        charFmt.append("}");

                        inlinesJson.append("{\"characterFormat\":").append(charFmt)
                                   .append(",\"text\":\"").append(escapeJson(text)).append("\"}");
                    }
                }

                if (!hasContent) {
                    inlinesJson.append("{\"characterFormat\":{},\"text\":\"\"}");
                }

                blocksJson.append("{")
                          .append("\"paragraphFormat\":{\"textAlignment\":\"").append(alignStr).append("\"},")
                          .append("\"characterFormat\":{},")
                          .append("\"inlines\":[").append(inlinesJson).append("]")
                          .append("}");
            }

            if (blocksJson.length() == 0) {
                blocksJson.append("{\"paragraphFormat\":{},\"characterFormat\":{},\"inlines\":[{\"characterFormat\":{},\"text\":\"\"}]}");
            }

            return buildSfdtEnvelope(blocksJson.toString());
        }
    }

    private String buildSfdtFromText(String text) {
        StringBuilder blocksJson = new StringBuilder();
        String[] lines = text.isEmpty() ? new String[]{""} : text.split("\n", -1);
        boolean firstBlock = true;

        for (String line : lines) {
            if (!firstBlock) blocksJson.append(",");
            firstBlock = false;
            blocksJson.append("{\"paragraphFormat\":{},\"characterFormat\":{},")
                      .append("\"inlines\":[{\"characterFormat\":{},\"text\":\"")
                      .append(escapeJson(line)).append("\"}]}");
        }

        return buildSfdtEnvelope(blocksJson.toString());
    }

    private String buildSfdtEnvelope(String blocksJson) {
        return "{\"sfdt\":\"1.2\",\"sections\":[{" +
               "\"sectionFormat\":{\"pageWidth\":612,\"pageHeight\":792,\"leftMargin\":72,\"rightMargin\":72,\"topMargin\":72,\"bottomMargin\":72}," +
               "\"blocks\":[" + blocksJson + "]}]}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}


