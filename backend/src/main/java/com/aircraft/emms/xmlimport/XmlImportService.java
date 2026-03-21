package com.aircraft.emms.xmlimport;

import com.aircraft.emms.entity.*;
import com.aircraft.emms.repository.*;
import com.aircraft.emms.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlImportService {

    private final MeterDefinitionRepository meterDefRepository;
    private final XmlImportLogRepository importLogRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${app.xml-import-dir:./data/uploads/xml}")
    private String xmlImportDir;

    @Transactional
    public XmlImportResult importZip(MultipartFile file, String importedBy) throws Exception {
        XmlImportResult result = XmlImportResult.builder()
                .fileName(file.getOriginalFilename())
                .build();

        User importer = userRepository.findByServiceId(importedBy)
                .orElseThrow(() -> new IllegalArgumentException("Importer not found"));

        // Create upload directory
        Path uploadPath = Paths.get(xmlImportDir);
        Files.createDirectories(uploadPath);

        // Save and extract ZIP
        Path zipPath = uploadPath.resolve(
                System.currentTimeMillis() + "_" + sanitizeFileName(file.getOriginalFilename()));
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String entryName = entry.getName();
                if (!entryName.toLowerCase().endsWith(".xml")) continue;

                // Parse XML
                byte[] xmlBytes = zis.readAllBytes();
                processXmlFile(entryName, xmlBytes, result, importer);

                zis.closeEntry();
            }
        }

        // Log import
        XmlImportLog importLog = XmlImportLog.builder()
                .fileName(file.getOriginalFilename())
                .xmlVersion(result.getXmlVersion())
                .importedBy(importer)
                .recordsImported(result.getSuccessCount())
                .recordsFailed(result.getFailureCount())
                .status(result.getFailureCount() == 0 ? "SUCCESS" : "PARTIAL")
                .errorMessage(result.getErrors().isEmpty() ? null :
                        String.join("\n", result.getErrors().subList(0, Math.min(5, result.getErrors().size()))))
                .build();
        importLogRepository.save(importLog);

        result.setSuccess(result.getFailureCount() == 0);

        auditService.log(importedBy, "XML_IMPORT", "XmlImportLog", importLog.getId(),
                "Imported " + result.getSuccessCount() + " records from " + file.getOriginalFilename());

        return result;
    }

    private void processXmlFile(String fileName, byte[] xmlBytes, XmlImportResult result, User importer) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
            doc.getDocumentElement().normalize();

            String rootTag = doc.getDocumentElement().getTagName();

            // Extract version from root element attribute
            String version = doc.getDocumentElement().getAttribute("version");
            if (version != null && !version.isBlank()) {
                result.setXmlVersion(version);
            }

            // Route to appropriate handler based on root element
            switch (rootTag.toLowerCase()) {
                case "meterdefinitions", "meter-definitions" -> processMeterDefinitions(doc, result);
                case "users" -> processUsers(doc, result);
                default -> {
                    result.addError("Unknown XML root element: " + rootTag + " in file: " + fileName);
                    result.setFailureCount(result.getFailureCount() + 1);
                }
            }
        } catch (Exception e) {
            log.error("Error processing XML file: {}", fileName, e);
            result.addError("Error in file " + fileName + ": " + e.getMessage());
            result.setFailureCount(result.getFailureCount() + 1);
        }
    }

    private void processMeterDefinitions(Document doc, XmlImportResult result) {
        NodeList nodes = doc.getElementsByTagName("meter");
        result.setTotalRecords(result.getTotalRecords() + nodes.getLength());

        for (int i = 0; i < nodes.getLength(); i++) {
            try {
                Element el = (Element) nodes.item(i);
                String meterName = getTextContent(el, "name");
                String aircraftType = getTextContent(el, "aircraftType");

                if (meterName == null || meterName.isBlank()) {
                    result.addError("Meter at index " + i + ": name is required");
                    result.setFailureCount(result.getFailureCount() + 1);
                    continue;
                }

                MeterDefinition def = MeterDefinition.builder()
                        .meterName(meterName)
                        .aircraftType(aircraftType)
                        .mandatory("true".equalsIgnoreCase(getTextContent(el, "mandatory")))
                        .displayOrder(parseIntOrDefault(getTextContent(el, "displayOrder"), i))
                        .unitOfMeasure(getTextContent(el, "unit"))
                        .active(true)
                        .build();

                meterDefRepository.save(def);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.addError("Meter at index " + i + ": " + e.getMessage());
                result.setFailureCount(result.getFailureCount() + 1);
            }
        }
    }

    private void processUsers(Document doc, XmlImportResult result) {
        NodeList nodes = doc.getElementsByTagName("user");
        result.setTotalRecords(result.getTotalRecords() + nodes.getLength());

        for (int i = 0; i < nodes.getLength(); i++) {
            try {
                Element el = (Element) nodes.item(i);
                String serviceId = getTextContent(el, "serviceId");

                if (serviceId == null || serviceId.isBlank()) {
                    result.addError("User at index " + i + ": serviceId is required");
                    result.setFailureCount(result.getFailureCount() + 1);
                    continue;
                }

                if (userRepository.existsByServiceId(serviceId)) {
                    result.addError("User at index " + i + ": serviceId already exists: " + serviceId);
                    result.setFailureCount(result.getFailureCount() + 1);
                    continue;
                }

                // Note: password import requires pre-hashed passwords or a default
                // For security, imported users get a default password that must be changed
                User user = User.builder()
                        .serviceId(serviceId)
                        .name(getTextContent(el, "name"))
                        .role(Role.valueOf(getTextContent(el, "role").toUpperCase()))
                        .password("$2a$12$IMPORT_PLACEHOLDER") // Must be reset
                        .active(true)
                        .build();

                userRepository.save(user);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.addError("User at index " + i + ": " + e.getMessage());
                result.setFailureCount(result.getFailureCount() + 1);
            }
        }
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) return null;
        return list.item(0).getTextContent().trim();
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unknown.zip";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Transactional(readOnly = true)
    public List<XmlImportLog> getImportHistory() {
        return importLogRepository.findAllByOrderByImportedAtDesc();
    }
}
