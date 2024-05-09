package lgl.bayern.de.ecertby.service;

import jakarta.servlet.http.HttpServletResponse;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.dto.SelectionFromDDJWTDTO;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.mapper.CompanyMapper;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.Company;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class JwtUpdateService {
    private final SecurityUtils securityUtils;
    private final SecurityService securityService;

    private final AuthorityService authorityService;
    AuthorityMapper authorityMapper = Mappers.getMapper(AuthorityMapper.class);

    private final CompanyService companyService;
    CompanyMapper companyMapper = Mappers.getMapper(CompanyMapper.class);

    private final ObjectLockService objectLockService;


    public Map<String,Boolean> checkIfAuthorityIsActiveAndUpdateJwt(String id, String jwt,HttpServletResponse response) {
        SelectionFromDDJWTDTO newSelectionJWT = null;
        Authority newSelectedAuthority = authorityService.findEntityById(id);
        newSelectionJWT = authorityMapper.mapToSelectionFromDDJWTDTO(newSelectedAuthority);
        Map<String,Boolean> result = new HashMap<>();
        if (!newSelectedAuthority.isActive()) {
            result.put(AppConstants.SelectionUtils.ACTIVE,false);
            return result;
        } else {
            securityUtils.generateJWTAndRefreshTokens(response, null,newSelectionJWT, jwt, AppConstants.Jwt.UPDATED_JWT);
            result.put(AppConstants.SelectionUtils.ACTIVE,true);
            //delete user locks since he will be redirected
            objectLockService.deleteLoggedInUserLocks();
            return result;
        }
    }

    public Map<String,Boolean> checkIfCompanyIsActiveAndUpdateJwt(String id, String jwt, HttpServletResponse response) {
        SelectionFromDDJWTDTO newSelectionJWT = null;
        Company newSelectedCompany = companyService.findEntityById(id);
        newSelectionJWT = companyMapper.mapToSelectionFromDDJWTDTO(newSelectedCompany);
        Map<String,Boolean> result = new HashMap<>();
        if (!newSelectedCompany.isActive()) {
               result.put(AppConstants.SelectionUtils.ACTIVE,false);
            if (newSelectedCompany.isDeleted()) {
                result.put(AppConstants.SelectionUtils.DELETED,true);
            }
            return result;
        } else {
            securityUtils.generateJWTAndRefreshTokens(response,null, newSelectionJWT, jwt, AppConstants.Jwt.UPDATED_JWT);
            result.put(AppConstants.SelectionUtils.ACTIVE,true);
            //delete user locks since he will be redirected
            objectLockService.deleteLoggedInUserLocks();
            return result;
        }
    }
}
