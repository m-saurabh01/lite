package com.aircraft.emms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "xml_import_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class XmlImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "xml_version", length = 20)
    private String xmlVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by_id")
    private User importedBy;

    @Column(name = "records_imported")
    private int recordsImported;

    @Column(name = "records_failed")
    private int recordsFailed;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }
}
