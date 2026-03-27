package com.aircraft.emms.xmlimport;

import com.aircraft.emms.entity.*;
import com.aircraft.emms.repository.*;
import com.aircraft.emms.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlImportService {

    private final AircraftDataSetRepository datasetRepository;
    private final AssetRepository assetRepository;
    private final MeterDefinitionRepository meterDefRepository;
    private final XmlImportLogRepository importLogRepository;
    private final UserRepository userRepository;
    private final SnagRepository snagRepository;
    private final SortieRepository sortieRepository;
    private final FlightLogBookRepository flbRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.xml-import-dir:./data/uploads/xml}")
    private String xmlImportDir;

    private static final String DEFAULT_IMPORT_PASSWORD = "Change@123";

    @Transactional
    public XmlImportResult importZip(MultipartFile file, String importedBy) throws Exception {
        XmlImportResult result = XmlImportResult.builder()
                .fileName(file.getOriginalFilename())
                .build();

        User importer = userRepository.findByServiceId(importedBy)
                .orElseThrow(() -> new IllegalArgumentException("Importer not found"));

        Path uploadPath = Paths.get(xmlImportDir);
        Files.createDirectories(uploadPath);
        Path zipPath = uploadPath.resolve(
                System.currentTimeMillis() + "_" + sanitizeFileName(file.getOriginalFilename()));
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Extract all XML files from ZIP
        Map<String, byte[]> xmlFiles = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String entryName = entry.getName().toLowerCase();
                if (!entryName.endsWith(".xml")) continue;
                xmlFiles.put(entryName, zis.readAllBytes());
                zis.closeEntry();
            }
        }

        if (xmlFiles.isEmpty()) {
            result.addError("ZIP contains no XML files");
            result.setSuccess(false);
            logImport(result, importer);
            return result;
        }

        // Parse all XML documents
        Map<String, Document> parsedDocs = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> e : xmlFiles.entrySet()) {
            try {
                parsedDocs.put(e.getKey(), parseXml(e.getValue()));
            } catch (Exception ex) {
                result.addError("Failed to parse " + e.getKey() + ": " + ex.getMessage());
            }
        }
        if (!result.getErrors().isEmpty()) {
            result.setSuccess(false);
            logImport(result, importer);
            return result;
        }

        // Identify XML files by root element
        Document modelDoc = null, usersDoc = null, metersDoc = null, flbDoc = null, snagDoc = null;
        for (Map.Entry<String, Document> e : parsedDocs.entrySet()) {
            String rootTag = e.getValue().getDocumentElement().getTagName().toLowerCase();
            switch (rootTag) {
                case "assets" -> modelDoc = e.getValue();
                case "users" -> usersDoc = e.getValue();
                case "meters" -> metersDoc = e.getValue();
                case "flbs" -> flbDoc = e.getValue();
                case "snags" -> snagDoc = e.getValue();
                default -> result.addError("Unknown root element '" + rootTag + "' in " + e.getKey());
            }
        }

        if (modelDoc == null) {
            result.addError("Missing model.xml (root element <assets>). This is required.");
            result.setSuccess(false);
            logImport(result, importer);
            return result;
        }

        String version = modelDoc.getDocumentElement().getAttribute("version");
        if (version != null && !version.isBlank()) result.setXmlVersion(version);

        // Parse model → assets
        List<AssetData> assetDataList = parseAssets(modelDoc, result);
        if (assetDataList.isEmpty()) {
            result.addError("Model XML contains no assets");
            result.setSuccess(false);
            logImport(result, importer);
            return result;
        }

        AssetData rootAsset = assetDataList.stream()
                .filter(a -> a.parentAssetNum == null || a.parentAssetNum.isBlank())
                .findFirst().orElse(null);
        if (rootAsset == null) {
            result.addError("No root asset found (asset with no parent)");
            result.setSuccess(false);
            logImport(result, importer);
            return result;
        }

        String aircraftAssetNum = rootAsset.assetNum;
        validateAssetHierarchy(assetDataList, result);

        Set<String> validAssetNums = new HashSet<>();
        assetDataList.forEach(a -> validAssetNums.add(a.assetNum));

        List<UserData> userData = usersDoc != null ? parseUsers(usersDoc, result) : List.of();
        Set<String> validServiceNums = new HashSet<>();
        userData.forEach(u -> validServiceNums.add(u.serviceNum));

        List<MeterData> meterData = metersDoc != null ? parseMeterDefs(metersDoc, validAssetNums, result) : List.of();
        List<FlbData> flbData = flbDoc != null ? parseFlbs(flbDoc, validAssetNums, result) : List.of();
        List<SnagData> snagData = snagDoc != null ? parseSnags(snagDoc, validAssetNums, validServiceNums, result) : List.of();

        if (!result.getErrors().isEmpty()) {
            result.setSuccess(false);
            logImport(result, importer);
            return result;
        }

        if (datasetRepository.existsByAssetNum(aircraftAssetNum)) {
            result.addError("Aircraft '" + aircraftAssetNum + "' already exists. Truncate it first.");
            result.setSuccess(false);
            logImport(result, importer);
            return result;
        }

        // Persist
        AircraftDataSet dataset = AircraftDataSet.builder()
                .assetNum(aircraftAssetNum)
                .aircraftName(rootAsset.name)
                .aircraftType(rootAsset.name)
                .active(false)
                .build();
        dataset = datasetRepository.save(dataset);

        int total = 0;
        for (AssetData ad : assetDataList) {
            assetRepository.save(Asset.builder().assetNum(ad.assetNum).name(ad.name)
                    .parentAssetNum(ad.parentAssetNum).dataset(dataset).build());
            total++;
        }

        String hashedPw = passwordEncoder.encode(DEFAULT_IMPORT_PASSWORD);
        for (UserData ud : userData) {
            if (userRepository.existsByServiceId(ud.serviceNum)) {
                result.addError("User '" + ud.serviceNum + "' already exists globally, skipped");
                continue;
            }
            userRepository.save(User.builder().serviceId(ud.serviceNum).name(ud.name)
                    .role(Role.PILOT).password(hashedPw).active(true).dataset(dataset).build());
            total++;
        }

        for (MeterData md : meterData) {
            meterDefRepository.save(MeterDefinition.builder()
                    .meterNum(md.meterNum).meterName(md.meterName).meterType(md.meterType)
                    .unitOfMeasure(md.uom).initialValue(md.initialValue).assetNum(md.assetNum)
                    .mandatory(md.isMandatory).displayOrder(md.displayOrder)
                    .active(true).dataset(dataset).build());
            total++;
        }

        for (FlbData fd : flbData) {
            User pilot = userData.isEmpty() ? importer :
                    userRepository.findByServiceId(userData.get(0).serviceNum).orElse(importer);
            FlightLogBook flb = FlightLogBook.builder()
                    .aircraftType(dataset.getAircraftType()).aircraftNumber(dataset.getAssetNum())
                    .pilot(pilot).actualTakeoffTime(fd.startTime).actualLandingTime(fd.endTime)
                    .remarks("Imported: sortie " + fd.sortieNumber + ", date " + fd.date)
                    .status(FlbStatus.CLOSED).dataset(dataset).build();
            flb.calculateDuration();
            flbRepository.save(flb);
            total++;
        }

        for (SnagData sd : snagData) {
            snagRepository.save(Snag.builder().assetNum(sd.assetNum).description(sd.description)
                    .reportedBy(sd.reportedBy).reportedAt(sd.reportedAt)
                    .dataset(dataset).status(SnagStatus.OPEN).build());
            total++;
        }

        result.setTotalRecords(total);
        result.setSuccessCount(total);
        result.setSuccess(true);
        logImport(result, importer);

        auditService.log(importedBy, "XML_IMPORT", "AircraftDataSet", dataset.getId(),
                "Imported aircraft: " + aircraftAssetNum + " (" + total + " records)");
        return result;
    }

    @Transactional(readOnly = true)
    public List<com.aircraft.emms.dto.ImportLogDto> getImportHistory() {
        return importLogRepository.findAllByOrderByImportedAtDesc().stream()
                .map(log -> com.aircraft.emms.dto.ImportLogDto.builder()
                        .id(log.getId())
                        .fileName(log.getFileName())
                        .xmlVersion(log.getXmlVersion())
                        .recordsImported(log.getRecordsImported())
                        .recordsFailed(log.getRecordsFailed())
                        .status(log.getStatus())
                        .errorMessage(log.getErrorMessage())
                        .importedAt(log.getImportedAt())
                        .build())
                .toList();
    }

    // ========== XML Parsing ==========

    private Document parseXml(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlBytes));
    }

    private List<AssetData> parseAssets(Document doc, XmlImportResult result) {
        List<AssetData> list = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("asset");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String assetNum = txt(el, "assetNum");
            String name = txt(el, "name");
            String parent = txt(el, "parentAssetNum");
            if (assetNum == null || assetNum.isBlank()) { result.addError("Asset[" + i + "]: assetNum required"); continue; }
            if (name == null || name.isBlank()) { result.addError("Asset[" + i + "]: name required"); continue; }
            list.add(new AssetData(assetNum, name, (parent != null && !parent.isBlank()) ? parent : null));
        }
        return list;
    }

    private void validateAssetHierarchy(List<AssetData> assets, XmlImportResult result) {
        Set<String> nums = new HashSet<>();
        for (AssetData a : assets) if (!nums.add(a.assetNum)) result.addError("Duplicate assetNum: " + a.assetNum);
        for (AssetData a : assets) if (a.parentAssetNum != null && !nums.contains(a.parentAssetNum))
            result.addError("Asset '" + a.assetNum + "': parent '" + a.parentAssetNum + "' not found");
        Map<String, String> pm = new HashMap<>();
        assets.forEach(a -> pm.put(a.assetNum, a.parentAssetNum));
        for (AssetData a : assets) {
            Set<String> v = new HashSet<>(); String c = a.assetNum;
            while (c != null) { if (!v.add(c)) { result.addError("Cycle at asset: " + c); break; } c = pm.get(c); }
        }
    }

    private List<UserData> parseUsers(Document doc, XmlImportResult result) {
        List<UserData> list = new ArrayList<>(); Set<String> seen = new HashSet<>();
        NodeList nodes = doc.getElementsByTagName("user");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String sn = txt(el, "serviceNum"); String nm = txt(el, "name");
            if (sn == null || sn.isBlank()) { result.addError("User[" + i + "]: serviceNum required"); continue; }
            if (nm == null || nm.isBlank()) { result.addError("User[" + i + "]: name required"); continue; }
            if (!seen.add(sn)) { result.addError("Duplicate serviceNum: " + sn); continue; }
            list.add(new UserData(sn, nm));
        }
        return list;
    }

    private List<MeterData> parseMeterDefs(Document doc, Set<String> validAssets, XmlImportResult result) {
        List<MeterData> list = new ArrayList<>(); Set<String> seen = new HashSet<>();
        NodeList nodes = doc.getElementsByTagName("meter");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String mn = txt(el, "meterNum"), name = txt(el, "meterName"),
                    typeStr = txt(el, "meterType"), uom = txt(el, "uom"),
                    iv = txt(el, "initialValue"), an = txt(el, "assetNum"),
                    mand = txt(el, "isMandatory");
            if (mn == null || mn.isBlank()) { result.addError("Meter[" + i + "]: meterNum required"); continue; }
            if (!seen.add(mn)) { result.addError("Duplicate meterNum: " + mn); continue; }
            if (an != null && !an.isBlank() && !validAssets.contains(an)) {
                result.addError("Meter '" + mn + "': asset '" + an + "' not found"); continue; }
            MeterType mt = MeterType.CONTINUOUS;
            if (typeStr != null && !typeStr.isBlank()) {
                try { mt = MeterType.valueOf(typeStr.toUpperCase()); }
                catch (IllegalArgumentException e) { result.addError("Meter '" + mn + "': invalid meterType"); continue; }
            }
            if (iv != null && !iv.isBlank()) {
                try { if (new BigDecimal(iv).compareTo(BigDecimal.ZERO) < 0) { result.addError("Meter '" + mn + "': initialValue < 0"); continue; } }
                catch (NumberFormatException e) { result.addError("Meter '" + mn + "': invalid initialValue"); continue; }
            }
            list.add(new MeterData(mn, name != null ? name : mn, mt, uom != null ? uom : "",
                    iv != null ? iv : "0", an, "true".equalsIgnoreCase(mand), i));
        }
        return list;
    }

    private List<FlbData> parseFlbs(Document doc, Set<String> validAssets, XmlImportResult result) {
        List<FlbData> list = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("flb");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String sn = txt(el, "sortieNumber"), ds = txt(el, "date"),
                    ss = txt(el, "startTime"), es = txt(el, "endTime"), an = txt(el, "assetNum");
            if (an != null && !an.isBlank() && !validAssets.contains(an)) {
                result.addError("FLB[" + i + "]: asset '" + an + "' not found"); continue; }
            LocalDate d = null; LocalTime st = null, et = null;
            try { if (ds != null && !ds.isBlank()) d = LocalDate.parse(ds); }
            catch (DateTimeParseException e) { result.addError("FLB[" + i + "]: invalid date"); continue; }
            try { if (ss != null && !ss.isBlank()) st = LocalTime.parse(ss);
                   if (es != null && !es.isBlank()) et = LocalTime.parse(es); }
            catch (DateTimeParseException e) { result.addError("FLB[" + i + "]: invalid time"); continue; }
            if (st != null && et != null && !et.isAfter(st)) {
                result.addError("FLB[" + i + "]: endTime must be after startTime"); continue; }
            list.add(new FlbData(sn, d, st, et, an));
        }
        return list;
    }

    private List<SnagData> parseSnags(Document doc, Set<String> validAssets, Set<String> validUsers, XmlImportResult result) {
        List<SnagData> list = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("snag");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String an = txt(el, "assetNum"), desc = txt(el, "description"),
                    rb = txt(el, "reportedBy"), ra = txt(el, "reportedAt");
            if (an == null || an.isBlank() || !validAssets.contains(an)) {
                result.addError("Snag[" + i + "]: invalid assetNum"); continue; }
            if (desc == null || desc.isBlank()) { result.addError("Snag[" + i + "]: description required"); continue; }
            if (rb != null && !rb.isBlank() && !validUsers.isEmpty() && !validUsers.contains(rb)) {
                result.addError("Snag[" + i + "]: reportedBy '" + rb + "' not in users.xml"); continue; }
            LocalDateTime rat = LocalDateTime.now();
            if (ra != null && !ra.isBlank()) {
                try { rat = LocalDateTime.parse(ra); }
                catch (DateTimeParseException e) { result.addError("Snag[" + i + "]: invalid reportedAt"); continue; }
            }
            list.add(new SnagData(an, desc, rb != null ? rb : "", rat));
        }
        return list;
    }

    private String txt(Element parent, String tag) {
        NodeList l = parent.getElementsByTagName(tag);
        return l.getLength() == 0 ? null : l.item(0).getTextContent().trim();
    }

    private String sanitizeFileName(String fn) {
        return fn == null ? "unknown.zip" : fn.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void logImport(XmlImportResult result, User importer) {
        importLogRepository.save(XmlImportLog.builder()
                .fileName(result.getFileName()).xmlVersion(result.getXmlVersion())
                .importedBy(importer).recordsImported(result.getSuccessCount())
                .recordsFailed(result.getFailureCount())
                .status(result.isSuccess() ? "SUCCESS" : "FAILED")
                .errorMessage(result.getErrors().isEmpty() ? null :
                        String.join("\n", result.getErrors().subList(0, Math.min(10, result.getErrors().size()))))
                .build());
    }

    private record AssetData(String assetNum, String name, String parentAssetNum) {}
    private record UserData(String serviceNum, String name) {}
    private record MeterData(String meterNum, String meterName, MeterType meterType,
                             String uom, String initialValue, String assetNum,
                             boolean isMandatory, int displayOrder) {}
    private record FlbData(String sortieNumber, LocalDate date, LocalTime startTime,
                           LocalTime endTime, String assetNum) {}
    private record SnagData(String assetNum, String description, String reportedBy,
                            LocalDateTime reportedAt) {}
}
