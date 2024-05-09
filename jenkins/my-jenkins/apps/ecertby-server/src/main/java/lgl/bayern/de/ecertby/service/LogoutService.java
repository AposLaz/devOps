package lgl.bayern.de.ecertby.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.model.Company;
import lgl.bayern.de.ecertby.model.QCompany;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class LogoutService extends BaseService<CompanyDTO, Company, QCompany> {

    private final KeycloakService keycloakService;

    private final ObjectLockService objectLockService;

    public void logout(String username, HttpServletRequest request) throws IOException, ServletException {
        objectLockService.deleteAllUserLocks(username);
        keycloakService.logoutUserFromKeycloak(username);
        request.getSession().invalidate();
        request.logout();
    }
}
