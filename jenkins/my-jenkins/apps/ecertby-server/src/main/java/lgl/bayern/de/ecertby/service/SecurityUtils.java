package lgl.bayern.de.ecertby.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.util.jwt.config.AppPropertiesUtilJwt;
import com.eurodyn.qlack.util.jwt.dto.JwtGenerateRequestDTO;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.dto.SelectionFromDDJWTDTO;
import lgl.bayern.de.ecertby.dto.UserDetailJWTDTO;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.mapper.CompanyMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.Company;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class SecurityUtils {

    @Value("${ecert.jwt.issuer}")
    private String issuer;

    @Value("${ecert.jwt.secret}")
    private String secret;

    @Value("${ecert.jwt.ttl}")
    private String timeToLive;

    @Value("${ecert.jwt.subject}")
    private String jwtSubject;

    @Value("${ecert.jwt.refresh-token-ttl}")
    private String refreshTokenTimeToLive;

    @Value("${ecert.jwt.refresh-times}")
    private int refreshTimes;

    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);
    AuthorityMapper authorityMapper = Mappers.getMapper(AuthorityMapper.class);
    CompanyMapper companyMapper = Mappers.getMapper(CompanyMapper.class);
    private final UserDetailRepository userDetailRepository;
    private final AuthorityService authorityService;
    private final CompanyService companyService;
    private final SecurityService securityService;

    private final ObjectLockService objectLockService;

    private final static String BEARER = "Bearer ";

    public String createSessionJwt(UserDetailJWTDTO dto, SelectionFromDDJWTDTO selection, String previousJwt, String jwtStatus) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(AppConstants.Jwt.USER_DETAIL_DTO, dto);
        claims.put(AppConstants.Jwt.ACTIVE_SELECTION_FROM_DD, selection);
        claims.put(AppConstants.Jwt.JWT_STATUS, jwtStatus);
        JwtGenerateRequestDTO.JwtGenerateRequestDTOBuilder builder = JwtGenerateRequestDTO.builder();
        builder.claims(claims);
        builder.subject(jwtSubject);

        JwtGenerateRequestDTO request = builder.build();

        AppPropertiesUtilJwt appPropertiesUtilJwt = new AppPropertiesUtilJwt();
        appPropertiesUtilJwt.setJwtIssuer(issuer);
        appPropertiesUtilJwt.setJwtSecret(secret);
        int timeInMinutes = Integer.parseInt(timeToLive);

        appPropertiesUtilJwt.setJwtTtlMinutes(timeInMinutes);

        return generateJwt(request, appPropertiesUtilJwt, previousJwt);
    }

    public String generateJwt(JwtGenerateRequestDTO request, AppPropertiesUtilJwt appProperties, String previousJwt) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        Instant now = Instant.now();
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(Base64.encodeBase64String(appProperties.getJwtSecret().getBytes()));
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
        Date expirationDate = null;
        if (previousJwt != null) {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).withIssuer(issuer).build();
            DecodedJWT decodedJWT = verifier.verify(previousJwt.replace(BEARER, ""));
            long expirationTimeSeconds = decodedJWT.getExpiresAt().toInstant().getEpochSecond();
            expirationDate = Date.from(Instant.ofEpochSecond(expirationTimeSeconds));
        } else {
            expirationDate = new Date(Instant.now().plus(appProperties.getJwtTtlMinutes(), ChronoUnit.MINUTES).toEpochMilli());
        }

        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(Date.from(now))
                .setSubject(request.getSubject())
                .setIssuer(appProperties.getJwtIssuer())
                .setExpiration(expirationDate)
                .signWith(signatureAlgorithm, signingKey);
        if (!request.getClaims().keySet().isEmpty()) {
            builder.addClaims(request.getClaims());
        }

        return builder.compact();
    }

    public String doGenerateRefreshToken() {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(Base64.encodeBase64String(secret.getBytes()));
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
        return Jwts.builder().setSubject(jwtSubject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setIssuer(issuer)
                .setExpiration(new Date(Instant.now().plus(Integer.parseInt(refreshTokenTimeToLive), ChronoUnit.MINUTES).toEpochMilli()))
                .signWith(signatureAlgorithm, signingKey).compact();
    }

    /**
     * Validates jwt against signature, expiration date + with logged in user's username
     *
     * @param jwt      The jwt to validate
     * @param username Logged in user's username
     * @return true if valid
     */
    public boolean validateJWT(String jwt, String username) {

        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).withIssuer(issuer).build();
        DecodedJWT decodedJWT = verifier.verify(jwt.replace(BEARER, ""));
        String jwtUsername = JsonParser.parseString(StringUtils.newStringUtf8(Base64.decodeBase64(decodedJWT.getPayload())))
                .getAsJsonObject().getAsJsonObject("userDetailDTO").get("username").getAsString();
        if (!username.equalsIgnoreCase(jwtUsername)) {
            throw new QExceptionWrapper("Invalid JWT");
        }
        return true;
    }

    /**
     * Method to refresh the jwt token
     *
     * @param refreshToken The current refresh token
     * @param jwtToken     The current jwt token
     * @param response     A response with the new jwt and refresh tokens in its' headers
     */
    public void refreshToken(String refreshToken, String jwtToken, HttpServletResponse response) {

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(Base64.encodeBase64String(secret.getBytes()));
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
        //First validate refresh token against signature and expiration date
        Jwts.parser().setSigningKey(signingKey).parse(refreshToken);

        Date expirationDate = Jwts.parser().setSigningKey(signingKey).parseClaimsJws(jwtToken.replace(BEARER, "")).getBody().getExpiration();

        Calendar currentDate = Calendar.getInstance();
        long after6Mins = currentDate.getTimeInMillis() + (6 * 60 * 1000);
        //Check if jwt token is about to expire in the next 6 minutes
        if (after6Mins >= expirationDate.getTime()) {
            UserDetail userDetail = userDetailRepository.getRefreshToken(securityService.getLoggedInUserDetailId());
            //If max refresh times has been reached, user should be logged out
            if (userDetail.getRefreshCounter() < refreshTimes) {
                //refresh token should be validated with the one saved in user's db
                if (!refreshToken.equals(userDetail.getRefreshToken())) {
                    throw new QExceptionWrapper("Invalid Refresh Token");
                }

                //everything is fine at this point, create a new jwt and a new refresh token and send them back
                UserDetailJWTDTO userDetailDTO = userMapperInstance.mapToUserDetailJWTDTO(userDetailRepository.findByUserUsernameIgnoreCase(securityService.getLoggedInUserName()));
                SelectionFromDDJWTDTO currentSelection = null;
                if (!userDetail.getUserType().equals(UserType.ADMIN_USER)) {
                    currentSelection = copyActiveSelection(jwtToken);
                }
                response.setHeader(AppConstants.Jwt.JWT, createSessionJwt(userDetailDTO, currentSelection, null, AppConstants.Jwt.REFRESH_JWT));

                String newRefreshToken = doGenerateRefreshToken();

                //update refresh token + counter in db
                userDetailRepository.updateRefreshTokenAndCounter(newRefreshToken, userDetail.getRefreshCounter() + 1, userDetailDTO.getId());
                response.setHeader("refresh-token", newRefreshToken);
            } else {
                //logout
                throw new QExceptionWrapper("Reached max refresh times");
            }
        }
    }

    private SelectionFromDDJWTDTO copyActiveSelection(String jwtToken) {
        JsonObject object = retrieveActiveSelection(jwtToken);
        SelectionFromDDJWTDTO currentSelection = new SelectionFromDDJWTDTO(
                object.get("active").getAsBoolean(),
                object.get("deleted").getAsBoolean(),
                null
        );
        currentSelection.setId(object.get("id").getAsString());
        return currentSelection;
    }

    public void generateJWTAndRefreshTokens(HttpServletResponse response, UserDetail userDetail,SelectionFromDDJWTDTO selectionFromDDJWTDTO, String previousJwt, String jwtStatus) {
        log.info("generating new jwt and refresh");
        UserDetailJWTDTO userDetailDTO = null;
        if(userDetail == null) {
            userDetail = userDetailRepository.findByUserUsernameIgnoreCase(securityService.getLoggedInUserUsername());
        }
        userDetailDTO = userMapperInstance.mapToUserDetailJWTDTO(userDetail);

        if (selectionFromDDJWTDTO != null) {
            response.setHeader(AppConstants.Jwt.JWT, createSessionJwt(userDetailDTO, selectionFromDDJWTDTO, previousJwt, jwtStatus));
        } else {
            response.setHeader(AppConstants.Jwt.JWT, createSessionJwt(userDetailDTO, getPrimarySelection(userDetailDTO), previousJwt, jwtStatus));
        }
        log.trace("jwt created");
        String refreshToken = doGenerateRefreshToken();

        int refreshCounter = 0;
        if (previousJwt != null) {
            refreshCounter = userDetail.getRefreshCounter();
        }
        userDetailRepository.updateRefreshTokenAndCounter(refreshToken, refreshCounter, userDetailDTO.getId());
        response.setHeader("refresh-token", refreshToken);
        log.trace("refresh token created");
    }

    private SelectionFromDDJWTDTO getPrimarySelection(UserDetailJWTDTO userDetailDTO) {
        SelectionFromDDJWTDTO firstSelection = null;
        if (userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
            firstSelection = authorityMapper.mapToSelectionFromDDJWTDTO(authorityService.findEntityById(userDetailDTO.getPrimaryAuthority().getId()));
        } else if (userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            firstSelection = companyMapper.mapToSelectionFromDDJWTDTO(companyService.findEntityById(userDetailDTO.getPrimaryCompany().getId()));
        }
        return firstSelection;
    }

    public boolean checkIfUserIsActiveAndUpdateJwt(HttpServletResponse response, String username, String jwt) {
        UserDetail userDetail = userDetailRepository.findByUserUsernameIgnoreCase(username);
        if(userDetail == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setHeader("deleted-user", "true");
            return false;
        }
        if (!userDetail.isActive()) {
            generateJWTAndRefreshTokens(response, userDetail, null, jwt, AppConstants.Jwt.UPDATED_JWT);
            return false;
        }
        if (!userDetail.getUserType().equals(UserType.ADMIN_USER)) {
            JsonObject object = retrieveActiveSelection(jwt);
            String currentSelection = object.get("id").getAsString();
            return checkIfCurrentSelectionIsActiveAndUpdateJWT(response, userDetail, currentSelection, jwt);
        }
        return true;
    }

    private JsonObject retrieveActiveSelection(String jwt) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).withIssuer(issuer).build();
        DecodedJWT decodedJWT = verifier.verify(jwt.replace(BEARER, ""));
        return JsonParser.parseString(StringUtils.newStringUtf8(Base64.decodeBase64(decodedJWT.getPayload())))
                .getAsJsonObject().getAsJsonObject(AppConstants.Jwt.ACTIVE_SELECTION_FROM_DD);
    }

    private  boolean  checkIfCurrentSelectionIsActiveAndUpdateJWT(HttpServletResponse response, UserDetail userDetail, String param, String jwt) {
        if (param != null) {
            if (userDetail.getUserType().equals(UserType.AUTHORITY_USER)) {
                Authority currentActiveAuthority = authorityService.findEntityById(param);
                if (!currentActiveAuthority.isActive()) {
                    SelectionFromDDJWTDTO newAuthoritySelection = authorityService.mapFirstActiveAuthorityToSelectionAndUpdateJWT(userDetail);
                    generateJWTAndRefreshTokens(response,userDetail,newAuthoritySelection, jwt, AppConstants.Jwt.UPDATED_JWT);
                    //delete user locks since he will be redirected
                    log.info("delete locks for user" + securityService.getLoggedInUserName());
                    objectLockService.deleteLoggedInUserLocks();
                    return false;
                }
            } else if (userDetail.getUserType().equals(UserType.COMPANY_USER)) {
                Company currentActiveCompany = companyService.findEntityById(param);
                if (!currentActiveCompany.isActive()) {
                    SelectionFromDDJWTDTO newCompanySelection = companyService.mapFirstActiveCompanyToSelectionAndUpdateJWT(userDetail, currentActiveCompany);
                    generateJWTAndRefreshTokens(response,userDetail,newCompanySelection, jwt, AppConstants.Jwt.UPDATED_JWT);
                    //delete user locks since he will be redirected
                    log.info("delete locks for user" + securityService.getLoggedInUserName());
                    objectLockService.deleteLoggedInUserLocks();
                    return false;
                }
            }
        }
        return true;
    }
}
