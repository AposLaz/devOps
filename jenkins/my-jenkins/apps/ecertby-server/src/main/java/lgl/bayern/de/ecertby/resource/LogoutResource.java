package lgl.bayern.de.ecertby.resource;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lgl.bayern.de.ecertby.service.LogoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("logout")
public class LogoutResource {

    private final LogoutService logoutService;

    @Value("${ecert.saml-success-redirect}")
    private String samlSuccessRedirect;

    @GetMapping("saml2/slo")
    public void logout(HttpServletResponse response, HttpServletRequest request, @RequestParam(value = "withRedirect") boolean withRedirect) throws IOException, ServletException {
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            DefaultSaml2AuthenticatedPrincipal principal =
                    (DefaultSaml2AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String userName = principal.getName();
            logoutService.logout(userName, request);
            if (withRedirect) {
                response.sendRedirect(samlSuccessRedirect);
            }
        }
    }

}
