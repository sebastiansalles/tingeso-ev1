package cl.ipt.siga.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

// Reads who is logged in from the Keycloak token of the current request.
// SIGA does not store users or passwords: a user is linked to a student or teacher by email.
public final class CurrentUser {

    private CurrentUser() { }

    public static String email() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            String email = token.getToken().getClaimAsString("email");
            return email == null ? null : email.trim().toLowerCase();
        }
        return null;
    }
}