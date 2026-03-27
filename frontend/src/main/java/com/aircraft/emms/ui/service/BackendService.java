package com.aircraft.emms.ui.service;

import com.aircraft.emms.ui.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BackendService {

    private static final BackendService INSTANCE = new BackendService();
    private final ApiClient api = ApiClient.getInstance();

    private BackendService() {}

    public static BackendService getInstance() {
        return INSTANCE;
    }

    // ---- Auth ----

    public LoginResponse login(String serviceId, String password) throws IOException, InterruptedException {
        Map<String, String> body = Map.of("serviceId", serviceId, "password", password);
        ApiResponse<LoginResponse> resp = api.post("/auth/login", body,
                new TypeReference<>() {});
        return resp.getData();
    }

    public void logout() throws IOException, InterruptedException {
        api.post("/auth/logout", new TypeReference<ApiResponse<Void>>() {});
    }

    public String getSecurityQuestion(String serviceId) throws IOException, InterruptedException {
        ApiResponse<String> resp = api.get("/auth/security-question?serviceId=" + serviceId,
                new TypeReference<>() {});
        return resp.getData();
    }

    public void resetPassword(String serviceId, String securityAnswer, String newPassword) throws IOException, InterruptedException {
        Map<String, String> body = Map.of(
                "serviceId", serviceId,
                "securityAnswer", securityAnswer,
                "newPassword", newPassword
        );
        api.post("/auth/reset-password", body, new TypeReference<ApiResponse<Void>>() {});
    }

    // ---- Users ----

    public UserDto createUser(UserDto dto) throws IOException, InterruptedException {
        ApiResponse<UserDto> resp = api.post("/users/manage/create", dto, new TypeReference<>() {});
        return resp.getData();
    }

    public UserDto updateUser(Long id, UserDto dto) throws IOException, InterruptedException {
        ApiResponse<UserDto> resp = api.put("/users/manage/" + id, dto, new TypeReference<>() {});
        return resp.getData();
    }

    public List<UserDto> getAllUsers() throws IOException, InterruptedException {
        ApiResponse<List<UserDto>> resp = api.get("/users", new TypeReference<>() {});
        return resp.getData();
    }

    public List<UserDto> getUsersByRole(Role role) throws IOException, InterruptedException {
        ApiResponse<List<UserDto>> resp = api.get("/users/role/" + role.name(), new TypeReference<>() {});
        return resp.getData();
    }

    public UserDto getCurrentUser() throws IOException, InterruptedException {
        ApiResponse<UserDto> resp = api.get("/users/me", new TypeReference<>() {});
        return resp.getData();
    }

    public void deactivateUser(Long id) throws IOException, InterruptedException {
        api.delete("/users/manage/" + id, new TypeReference<ApiResponse<Void>>() {});
    }

    // ---- Sorties ----

    public SortieDto createSortie(SortieDto dto) throws IOException, InterruptedException {
        ApiResponse<SortieDto> resp = api.post("/sorties/create", dto, new TypeReference<>() {});
        return resp.getData();
    }

    public SortieDto assignPilot(Long sortieId, Long pilotId) throws IOException, InterruptedException {
        ApiResponse<SortieDto> resp = api.post("/sorties/assign?sortieId=" + sortieId + "&pilotId=" + pilotId,
                new TypeReference<>() {});
        return resp.getData();
    }

    public SortieDto acceptSortie(Long id) throws IOException, InterruptedException {
        ApiResponse<SortieDto> resp = api.post("/sorties/" + id + "/accept", new TypeReference<>() {});
        return resp.getData();
    }

    public SortieDto rejectSortie(Long id, String remarks) throws IOException, InterruptedException {
        String url = "/sorties/" + id + "/reject";
        if (remarks != null && !remarks.isBlank()) {
            url += "?remarks=" + java.net.URLEncoder.encode(remarks, java.nio.charset.StandardCharsets.UTF_8);
        }
        ApiResponse<SortieDto> resp = api.post(url, new TypeReference<>() {});
        return resp.getData();
    }

    public SortieDto cancelSortie(Long id) throws IOException, InterruptedException {
        ApiResponse<SortieDto> resp = api.post("/sorties/" + id + "/cancel", new TypeReference<>() {});
        return resp.getData();
    }

    public SortieDto closeSortie(Long id) throws IOException, InterruptedException {
        ApiResponse<SortieDto> resp = api.post("/sorties/" + id + "/close", new TypeReference<>() {});
        return resp.getData();
    }

    public List<SortieDto> getAllSorties() throws IOException, InterruptedException {
        ApiResponse<List<SortieDto>> resp = api.get("/sorties", new TypeReference<>() {});
        return resp.getData();
    }

    public List<SortieDto> getMySorties() throws IOException, InterruptedException {
        ApiResponse<List<SortieDto>> resp = api.get("/sorties/my-sorties", new TypeReference<>() {});
        return resp.getData();
    }

    public List<SortieDto> getPendingSorties() throws IOException, InterruptedException {
        ApiResponse<List<SortieDto>> resp = api.get("/sorties/pending", new TypeReference<>() {});
        return resp.getData();
    }

    // ---- FLB ----

    public FlightLogBookDto createFlb(FlightLogBookDto dto) throws IOException, InterruptedException {
        ApiResponse<FlightLogBookDto> resp = api.post("/flb", dto, new TypeReference<>() {});
        return resp.getData();
    }

    public FlightLogBookDto updateFlb(Long id, FlightLogBookDto dto) throws IOException, InterruptedException {
        ApiResponse<FlightLogBookDto> resp = api.put("/flb/" + id, dto, new TypeReference<>() {});
        return resp.getData();
    }

    public FlightLogBookDto closeFlb(Long id) throws IOException, InterruptedException {
        ApiResponse<FlightLogBookDto> resp = api.post("/flb/" + id + "/close", new TypeReference<>() {});
        return resp.getData();
    }

    public FlightLogBookDto abortFlb(Long id) throws IOException, InterruptedException {
        ApiResponse<FlightLogBookDto> resp = api.post("/flb/" + id + "/abort", new TypeReference<>() {});
        return resp.getData();
    }

    public List<FlightLogBookDto> getAllFlbs() throws IOException, InterruptedException {
        ApiResponse<List<FlightLogBookDto>> resp = api.get("/flb", new TypeReference<>() {});
        return resp.getData();
    }

    public List<FlightLogBookDto> getMyFlbs() throws IOException, InterruptedException {
        ApiResponse<List<FlightLogBookDto>> resp = api.get("/flb/my-flbs", new TypeReference<>() {});
        return resp.getData();
    }

    public List<MeterEntryDto> getMeterDefinitions(String aircraftType) throws IOException, InterruptedException {
        ApiResponse<List<MeterEntryDto>> resp = api.get("/flb/meter-definitions/" + aircraftType,
                new TypeReference<>() {});
        return resp.getData();
    }

    public List<MeterEntryDto> getActiveAircraftMeterDefs() throws IOException, InterruptedException {
        ApiResponse<List<MeterEntryDto>> resp = api.get("/flb/meter-definitions",
                new TypeReference<>() {});
        return resp.getData();
    }

    // ---- XML Import ----

    public ApiResponse<Object> importXmlZip(java.nio.file.Path filePath) throws IOException, InterruptedException {
        return api.uploadFile("/xml-import/upload", filePath, new TypeReference<>() {});
    }

    public List<ImportLogDto> getImportHistory() throws IOException, InterruptedException {
        ApiResponse<List<ImportLogDto>> resp = api.get("/xml-import/history", new TypeReference<>() {});
        return resp.getData();
    }

    // ---- Admin: Aircraft Management ----

    public List<AircraftDataSetDto> listAircraft() throws IOException, InterruptedException {
        ApiResponse<List<AircraftDataSetDto>> resp = api.get("/admin/aircraft", new TypeReference<>() {});
        return resp.getData();
    }

    public AircraftDataSetDto getActiveAircraft() throws IOException, InterruptedException {
        ApiResponse<AircraftDataSetDto> resp = api.get("/aircraft/active", new TypeReference<>() {});
        return resp.getData();
    }

    public AircraftDataSetDto activateAircraft(Long id) throws IOException, InterruptedException {
        ApiResponse<AircraftDataSetDto> resp = api.post("/admin/aircraft/" + id + "/activate",
                new TypeReference<>() {});
        return resp.getData();
    }

    public void truncateAircraft(Long id) throws IOException, InterruptedException {
        api.delete("/admin/aircraft/" + id + "/truncate", new TypeReference<ApiResponse<Void>>() {});
    }

    // ---- Admin: Role Assignment ----

    public List<UserDto> getAircraftUsers() throws IOException, InterruptedException {
        ApiResponse<List<UserDto>> resp = api.get("/admin/aircraft-users", new TypeReference<>() {});
        return resp.getData();
    }

    public UserDto assignRole(Long userId, Role role) throws IOException, InterruptedException {
        Map<String, String> body = Map.of("userId", userId.toString(), "role", role.name());
        ApiResponse<UserDto> resp = api.post("/admin/assign-role", body, new TypeReference<>() {});
        return resp.getData();
    }

    public UserDto removeRole(Long userId, Role role) throws IOException, InterruptedException {
        Map<String, String> body = Map.of("userId", userId.toString(), "role", role.name());
        ApiResponse<UserDto> resp = api.post("/admin/remove-role", body, new TypeReference<>() {});
        return resp.getData();
    }
}
