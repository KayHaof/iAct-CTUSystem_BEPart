package com.example.feature.users.service;

import com.example.feature.users.dto.CreateProfileDto;
import com.example.feature.users.dto.ImportResultDto;
import com.example.feature.users.model.Users;
import com.example.feature.users.repository.UserRepository;
import com.example.feignClient.ProfileServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserImportService {

    private final UserRepository userRepository;
    private final ProfileServiceClient profileServiceClient;
    private final Keycloak keycloak;
    private final String realm = "myRealm";

    @Transactional(rollbackFor = Exception.class)
    public ImportResultDto importUsers(MultipartFile file, Integer roleType) {
        ImportResultDto result = new ImportResultDto();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream());
             Workbook errorWorkbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.getSheetAt(0);
            Sheet errorSheet = errorWorkbook.createSheet("Lỗi Import");
            int errorRowIdx = 0;

            // Header cho file lỗi
            Row headerRow = sheet.getRow(0);
            Row errorHeader = errorSheet.createRow(errorRowIdx++);
            copyRow(headerRow, errorHeader);
            errorHeader.createCell(headerRow.getLastCellNum()).setCellValue("LÝ DO LỖI");

            // 1. GOM DATA ĐỂ VALIDATE 1 LẦN
            List<Row> dataRows = new ArrayList<>();
            Set<String> excelUsernames = new HashSet<>();
            Set<String> excelEmails = new HashSet<>();
            Set<String> excelMssvs = new HashSet<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                excelUsernames.add(getCellValue(row, 0));
                excelEmails.add(getCellValue(row, 1));
                excelMssvs.add(getCellValue(row, 2));

                dataRows.add(row);
            }

            // Gọi DB 1 lần duy nhất
            Set<String> existingUsernames = userRepository.findExistingUsernames(excelUsernames);
            Set<String> existingEmails = userRepository.findExistingEmails(excelEmails);

            Set<String> existingMssvs = new HashSet<>();
            try {
                if (!excelMssvs.isEmpty()) {
                    existingMssvs = profileServiceClient.checkExistingStudentCodes(excelMssvs).getResult();
                }
            } catch (Exception e) {
                log.warn("Không thể gọi Profile-Service để check MSSV: {}", e.getMessage());
            }

            List<Users> usersToSave = new ArrayList<>();
            List<CreateProfileDto> profilesToSync = new ArrayList<>();
            List<String> createdKeycloakIds = new ArrayList<>();

            // 2. XỬ LÝ TỪNG DÒNG
            for (Row row : dataRows) {
                String username = getCellValue(row, 0);
                String email = getCellValue(row, 1);
                String mssv = getCellValue(row, 2);
                String fullName = getCellValue(row, 3);
                String classCode = getCellValue(row, 4);
                String password = getCellValue(row, 5);

                StringBuilder error = new StringBuilder();

                if (existingUsernames.contains(username)) error.append("Username đã tồn tại. ");
                if (existingEmails.contains(email)) error.append("Email đã tồn tại. ");
                if (existingMssvs.contains(mssv)) error.append("MSSV đã tồn tại trong hồ sơ Profile. ");
                if (username.isEmpty() || email.isEmpty() || password.isEmpty() || mssv.isEmpty()) error.append("Thiếu dữ liệu bắt buộc. ");

                if (!error.isEmpty()) {
                    writeError(errorSheet, row, errorRowIdx++, error.toString());
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }

                UserRepresentation userRep = new UserRepresentation();
                userRep.setUsername(username);
                userRep.setEmail(email);

                // Cắt First Name / Last Name từ Full Name (Sơ bộ)
                String[] nameParts = fullName.trim().split(" ", 2);
                userRep.setFirstName(nameParts[0]);
                if (nameParts.length > 1) userRep.setLastName(nameParts[1]);

                userRep.setEnabled(true);
                userRep.setEmailVerified(true);
                userRep.setGroups(Collections.singletonList("/students"));

                CredentialRepresentation cred = new CredentialRepresentation();
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue(password);
                cred.setTemporary(false);
                userRep.setCredentials(Collections.singletonList(cred));

                try {
                    Response response = keycloak.realm(realm).users().create(userRep);
                    if (response.getStatus() == 201) {
                        String path = response.getLocation().getPath();
                        String keycloakId = path.substring(path.lastIndexOf("/") + 1);

                        // Gán Role
                        assignRoleToUser(keycloakId, "student");
                        createdKeycloakIds.add(keycloakId); // Đưa vào ds để canh chừng

                        Users user = new Users();
                        user.setKeycloakId(keycloakId);
                        user.setUsername(username);
                        user.setEmail(email);
                        user.setRoleType(1);
                        user.setStatus(1);
                        usersToSave.add(user);

                        CreateProfileDto profile = CreateProfileDto.builder()
                                .fullName(fullName)
                                .roleType(1)
                                .studentCode(mssv)
                                .build();
                        profilesToSync.add(profile);

                        existingUsernames.add(username);
                        existingEmails.add(email);
                        existingMssvs.add(mssv);
                        result.setSuccessCount(result.getSuccessCount() + 1);

                    } else {
                        writeError(errorSheet, row, errorRowIdx++, "Lỗi Keycloak: " + response.getStatus());
                        result.setFailCount(result.getFailCount() + 1);
                    }
                    response.close();
                } catch (Exception e) {
                    writeError(errorSheet, row, errorRowIdx++, "Keycloak Exception: " + e.getMessage());
                    result.setFailCount(result.getFailCount() + 1);
                }
            }

            // 3. BATCH INSERT XUỐNG DB & GỌI PROFILE SERVICE
            try {
                if (!usersToSave.isEmpty()) {
                    usersToSave = userRepository.saveAll(usersToSave);

                    // Gán lại ID thực tế từ DB cho list Profile
                    for (int i = 0; i < usersToSave.size(); i++) {
                        profilesToSync.get(i).setUserId(usersToSave.get(i).getId());
                    }

                    profileServiceClient.createProfilesBatch(profilesToSync);
                }
            } catch (Exception dbException) {
                log.error("Lỗi khi lưu DB/Profile, tiến hành rollback Keycloak...", dbException);
                for (String kid : createdKeycloakIds) {
                    try { keycloak.realm(realm).users().get(kid).remove(); } catch (Exception ignored) {}
                }
                throw new RuntimeException("Lỗi hệ thống khi lưu dữ liệu, đã hoàn tác.");
            }

            // 4. MÃ HÓA FILE LỖI
            if (result.getFailCount() > 0) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                errorWorkbook.write(bos);
                result.setErrorFileBase64(Base64.getEncoder().encodeToString(bos.toByteArray()));
            }

        } catch (Exception e) {
            log.error("Lỗi khi đọc file Excel: ", e);
            throw new RuntimeException("Không thể xử lý file. Vui lòng kiểm tra lại định dạng file Excel!");
        }

        return result;
    }

    private void assignRoleToUser(String userId, String roleName) {
        var roleRep = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(roleRep));
    }

    private void writeError(Sheet sheet, Row source, int rowIndex, String errorMsg) {
        Row errorRow = sheet.createRow(rowIndex);
        copyRow(source, errorRow);
        errorRow.createCell(source.getLastCellNum()).setCellValue(errorMsg);
    }

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private void copyRow(Row source, Row dest) {
        for (int i = 0; i < source.getLastCellNum(); i++) {
            Cell oldCell = source.getCell(i);
            if (oldCell != null) {
                DataFormatter formatter = new DataFormatter();
                dest.createCell(i).setCellValue(formatter.formatCellValue(oldCell));
            }
        }
    }
}