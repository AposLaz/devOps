package lgl.bayern.de.ecertby.service;

import lgl.bayern.de.ecertby.dto.ResetPasswordDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class KeycloakService {

    @Value("${ecert.keycloak-container-url}")
    private String keycloakContainerURL;

    @Value("${ecert.keycloak-ecertby-realm}")
    private String keycloakEcertbyRealm;

    @Value("${ecert.keycloak-admin-username}")
    private String keycloakAdminUsername;

    @Value("${ecert.keycloak-admin-password}")
    private String keycloakAdminPassword;

    @Value("${ecert.keycloak-master-realm}")
    private String keycloakMasterRealm;

    @Value("${ecert.keycloak-client-id}")
    private String keycloakClientId;

    @Value("${ecert.keycloak-client-id-for-verification}")
    private String keycloakClientIdForVerification;

    /**
     * Create user in keycloak.
     * @param userDetailDTO The object with the information for the new user.
     */
    public void createKeycloakUser(UserDetailDTO userDetailDTO) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(userDetailDTO.getNewPassword());
        credential.setTemporary(true);

        UserRepresentation user = new UserRepresentation();
        updateUserRepresentation(user, userDetailDTO);
        user.setCredentials(Arrays.asList(credential));

        getKeycloak().realm(keycloakEcertbyRealm).users().create(user);
    }

    private void updateUserRepresentation(UserRepresentation user, UserDetailDTO userDetailDTO) {
        user.setUsername(userDetailDTO.getUsername());
        user.setFirstName(userDetailDTO.getFirstName());
        user.setLastName(userDetailDTO.getLastName());
        user.setEmail(userDetailDTO.getEmail());
        user.setEnabled(true);

    }

    /**
     * Update user in keycloak.
     * @param userDetailDTO The object with the information for the user.
     * @param usernameDB The current username in keycloak.
     */
    public void updateKeycloakUser(UserDetailDTO userDetailDTO, String usernameDB) {
        List<UserRepresentation> users = getKeycloak().realm(keycloakEcertbyRealm).users().searchByUsername(usernameDB, true);
        if(users.size() == 0){
            return;
        }
        UserRepresentation user = users.get(0);
        updateUserRepresentation(user, userDetailDTO);

        getKeycloak().realm(keycloakEcertbyRealm).users().get(user.getId()).update(user);
    }

    /**
     * Call to update user's password
     * @param username The username of logged-in user.
     * @param resetPasswordDTO The object with the current and new password.
     */
    public void updatePassword(String username, ResetPasswordDTO resetPasswordDTO) throws EcertBYGeneralException {

        // Verify current password.
        verifyCurrentPassword(username, resetPasswordDTO);
        // Reset password.
        resetPassword(username, resetPasswordDTO.getNewPassword());
    }

    private void verifyCurrentPassword(String username, ResetPasswordDTO resetPasswordDTO) throws EcertBYGeneralException {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", keycloakClientIdForVerification);
        map.add("username", username);
        map.add("password", resetPasswordDTO.getCurrentPassword());
        map.add("totp", resetPasswordDTO.getOtp());
        map.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Object> keycloakResponse =
                    restTemplate.exchange(keycloakContainerURL+"/realms/"+keycloakEcertbyRealm
                                    +"/protocol/openid-connect/token",
                            HttpMethod.POST, entity, Object.class);
            LinkedHashMap<String, String> keycloakResponseBody = (LinkedHashMap<String, String>) keycloakResponse.getBody();
            keycloakResponseBody.get("access_token");
        } catch (Exception e) {
            EcertBYErrorException errorException = new EcertBYErrorException("invalid_password", "invalid_password", "currentPassword", "resetPasswordDTO", null, true);
            throw new EcertBYGeneralException(Collections.singletonList(errorException));
        }
    }

    private void resetPassword(String username, String password) {
        RealmResource realmResource = getKeycloak().realm(keycloakEcertbyRealm);
        UserRepresentation userRepresentation = realmResource.users().searchByUsername(username, true).get(0);
        UserResource userResource = realmResource.users().get(userRepresentation.getId());

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(false);
        userResource.resetPassword(cred);
    }

    private Keycloak getKeycloak() {
        return getKeycloak(keycloakMasterRealm, keycloakAdminUsername, keycloakAdminPassword, keycloakClientId);
    }

    private Keycloak getKeycloak(String realm, String username, String password, String clientId) {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakContainerURL)
                .grantType(OAuth2Constants.PASSWORD)
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password).build();
    }

    public void logoutUserFromKeycloak(String userName) {
        List<UserRepresentation> users = getKeycloak().realm(keycloakEcertbyRealm).users().searchByUsername(userName, true);
        if(users.size() == 0){
            return;
        }
        UserRepresentation user = users.get(0);
        getKeycloak().realm(keycloakEcertbyRealm).users().get(user.getId()).logout();
    }

    public void deleteUserFromKeycloak(String userName) {
        List<UserRepresentation> users = getKeycloak().realm(keycloakEcertbyRealm).users().searchByUsername(userName, true);
        if(users.size() == 0){
            return;
        }
        UserRepresentation user = users.get(0);
        getKeycloak().realm(keycloakEcertbyRealm).users().delete(user.getId());
    }

}
