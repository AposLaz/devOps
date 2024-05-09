package lgl.bayern.de.ecertby.config.filters;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.service.SecurityService;
import lgl.bayern.de.ecertby.service.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfterAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityUtils securityUtils;

    private final SecurityService securityService;
    /**
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String username = "";
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().getPrincipal() != "anonymousUser" &&
                request.getHeader(AppConstants.AUTHORIZATION) == null && !request.getRequestURI().contains("logout")) {
            DefaultSaml2AuthenticatedPrincipal principal = (DefaultSaml2AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            username = principal.getName();
            if(securityService.getLoggedInUserUsername() != null) {
                securityUtils.generateJWTAndRefreshTokens(response, null, null, null, AppConstants.Jwt.FIRST_JWT);
            }
        }
        if (request.getHeader(AppConstants.AUTHORIZATION) != null) {
            try {
             DefaultSaml2AuthenticatedPrincipal principal = (DefaultSaml2AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                username = principal.getName();
                String jwt = request.getHeader(AppConstants.AUTHORIZATION);
                securityUtils.validateJWT(jwt, username);
                //do not check for active user/authority/company on those requests
                if (requestNotIn(request.getServletPath())) {
                    boolean active = securityUtils.checkIfUserIsActiveAndUpdateJwt(response, username, jwt);
                    if (!active) {
                        response.reset();
                        return;
                    }
                }
            } catch (Exception e) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                log.warn("The user with username {} has an invalid JWT token.", username);
                QExceptionWrapper qlackException = new QExceptionWrapper("Invalid JWT");
                ObjectMapper mapper = new ObjectMapper();
                response.getWriter().write(mapper.writeValueAsString((qlackException)));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean requestNotIn(String servletPath) {
        return !servletPath.equals("/security/checkIfActive") && !servletPath.startsWith("/objectlock/delete")
                && !servletPath.startsWith("/authority/isAuthorityActive")
                && !servletPath.startsWith("/company/isCompanyActive")
                && !servletPath.equals("/security/refresh-token");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getRequestURI().startsWith("/api/public");
    }
}
