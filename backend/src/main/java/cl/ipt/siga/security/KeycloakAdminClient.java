package cl.ipt.siga.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

// Creates user accounts in Keycloak through its Admin REST API, so that every student or teacher
// registered by the administrator can log in. The backend authenticates as the confidential client
// "siga-backend" (client credentials), whose service account is allowed to manage users.
@Component
public class KeycloakAdminClient {

    private final RestClient restClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAdminClient(@Value("${keycloak.admin.server-url}") String serverUrl,
                               @Value("${keycloak.admin.realm}") String realm,
                               @Value("${keycloak.admin.client-id}") String clientId,
                               @Value("${keycloak.admin.client-secret}") String clientSecret) {
        this.restClient = RestClient.create(serverUrl);
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    // Creates the account with a temporary password (Keycloak asks to change it on first login)
    // and assigns the realm role. If an account with that email already exists, it is reused.
    public void createUser(String email, String fullName, String initialPassword, String role) {
        String token = getAccessToken();

        // Keycloak keeps first and last name separately: "Ana Pérez Soto" -> "Ana" + "Pérez Soto"
        int space = fullName.indexOf(' ');
        String firstName = space > 0 ? fullName.substring(0, space) : fullName;
        String lastName = space > 0 ? fullName.substring(space + 1) : fullName;

        Map<String, Object> user = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of("type", "password", "value", initialPassword, "temporary", true)));

        URI location;
        try {
            location = restClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders().getLocation();
        } catch (HttpClientErrorException.Conflict e) {
            return;   // 409: an account with that email already exists. It is linked by email, nothing to create
        }

        // Keycloak answers 201 with the new user's URL: .../users/{id}
        String path = location.getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);

        Map<?, ?> roleRepresentation = restClient.get()
                .uri("/admin/realms/{realm}/roles/{role}", realm, role)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(Map.class);

        restClient.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(roleRepresentation))
                .retrieve()
                .toBodilessEntity();
    }

    private String getAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<?, ?> response = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        return (String) response.get("access_token");
    }
}