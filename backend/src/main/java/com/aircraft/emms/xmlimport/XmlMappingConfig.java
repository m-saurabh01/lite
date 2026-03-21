package com.aircraft.emms.xmlimport;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Configuration for XML-to-entity mapping.
 * Loaded from xml-mapping-config.json in the config directory.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class XmlMappingConfig {

    private String entityType;
    private String rootElement;
    private String recordElement;
    private String xmlVersion;
    private List<FieldMapping> fieldMappings;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class FieldMapping {
        private String xmlPath;
        private String entityField;
        private String dataType;  // STRING, INTEGER, DECIMAL, DATE, TIME, DATETIME
        private String dateFormat;
        private boolean required;
        private String defaultValue;
    }
}
