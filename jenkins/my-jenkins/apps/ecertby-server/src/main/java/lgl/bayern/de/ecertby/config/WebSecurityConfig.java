package lgl.bayern.de.ecertby.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

@Configuration
public class WebSecurityConfig {

  private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

  @Value("${ecert.saml-success-redirect}")
  private String successRedirect;

  @Value("${ecert.keycloak-url}"+ "/realms/ecertby/protocol/saml/clients/ecertbyDevClient")
  private String keycloakLoginPage;

  @Value("${ecert.keycloak-url}" +"/realms/ecertby/protocol/saml")
  private String keycloakLogoutUrl;

  @Value("${ecert.saml-logout-endpoint}")
  private String samlLogoutEndpoint;

  public WebSecurityConfig(RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
    this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
  }

  /**
   * Handler to enforce the redirection URL after a successful SAML authentication
   * @return the success redirect handler
   */
  @Bean
  public SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
    SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler = new SavedRequestAwareAuthenticationSuccessHandler();
    successRedirectHandler.setDefaultTargetUrl(successRedirect);
    successRedirectHandler.setAlwaysUseDefaultTargetUrl(true);
    return successRedirectHandler;
  }

  /**
   * Handler that manages a failed SAML authentication
   * @return the SAML authentication failure handler
   */
  @Bean
  public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
    SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
    failureHandler.setUseForward(true);
    failureHandler.setDefaultFailureUrl(keycloakLoginPage);
    return failureHandler;
  }

  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {

    Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver =
            new DefaultRelyingPartyRegistrationResolver(this.relyingPartyRegistrationRepository);


    Saml2MetadataFilter filter = new Saml2MetadataFilter((RelyingPartyRegistrationResolver) relyingPartyRegistrationResolver, new OpenSamlMetadataResolver());
    http
            .saml2Login(saml -> saml.loginPage(keycloakLoginPage)
                    .successHandler(successRedirectHandler())
                    .failureHandler(authenticationFailureHandler()))
            .saml2Logout(logout -> logout
                    .logoutRequest(req -> req.logoutUrl(keycloakLogoutUrl))
                    .logoutResponse(response -> response.logoutUrl(samlLogoutEndpoint)))
            .addFilterBefore(filter, Saml2WebSsoAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz.requestMatchers("/actuator/health").permitAll())
            .authorizeHttpRequests(authz -> authz.requestMatchers("/**").authenticated())
            .csrf(AbstractHttpConfigurer::disable);

    return http.build();

  }
}
