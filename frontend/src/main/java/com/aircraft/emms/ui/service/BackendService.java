package com.aircraft.emms.ui.service;

import com.aircraft.emms.ui.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * High-level API service methods used by controllers.
 * Wraps ApiClient with typed methods.
 */
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

    public SortieDto rejectSortie(Long id) throws IOException, InterruptedException {
        ApiResponse<SortieDto> resp = api.post("/sorties/" + id + "/reject", new TypeReference<>() {});
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

    public FlightLogBookDto submitFlb(Long id) throws IOException, InterruptedException {
        ApiResponse<FlightLogBookDto> resp = api.post("/flb/" + id + "/submit", new TypeReference<>() {});
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

    // ---- XML Import ----

    public ApiResponse<Object> importXmlZip(java.nio.file.Path filePath) throws IOException, InterruptedException {
        return api.uploadFile("/xml-import/upload", filePath, new TypeReference<>() {});
    }
}
