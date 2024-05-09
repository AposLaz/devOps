package lgl.bayern.de.ecertby.service;



import com.eurodyn.qlack.fuse.mailing.dto.EmailDTO;
import com.eurodyn.qlack.fuse.mailing.dto.EmailDTO.EMAIL_TYPE;
import com.eurodyn.qlack.fuse.mailing.service.MailService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import lgl.bayern.de.ecertby.config.MessageConfig;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.model.Certificate;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static lgl.bayern.de.ecertby.config.AppConstants.Email.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final MailService mailService;

  @Value("${spring.mail.properties.email_sender}")
  private String emailSender;
  @Value("${ecert.domain-url}")
  private String domainUrl;
  private Map<String,Object> parameters;
  private static final String USERNAME_FIELD = "username";
  private static final String CERTIFICATE_URL = "/certificate/%s/view";
  private static final String CERTIFICATE_ID_FIELD = "certificateId";
  private static final String LINK_FIELD = "link";

  public void sendCreateUserEmail(String username, String password, String siteUrl, String contact) {
    parameters = new HashMap<>();
    parameters.put(USERNAME_FIELD,username);
    parameters.put("password",password);
    parameters.put("siteUrl",siteUrl);
    sendEmail(CREATE_USER, parameters,contact);
  }

  public void sendFileHasVirusEmail(String certificateId , String filename ,String contact) {
    parameters = new HashMap<>();
    parameters.put(CERTIFICATE_ID_FIELD,certificateId);
    parameters.put("filename",filename);
    sendEmail(FILE_HAS_VIRUS, parameters, contact);
  }
  public void sendTeamMembersEmail(String teamName, String contact) {
    parameters = new HashMap<>();
    parameters.put("teamname",teamName);
    sendEmail(LINK_USER_WITH_TEAM, parameters, contact);
  }

  public void sendCompanyAdditionExistentUserEmail(String companyName, String contact) {
    parameters = new HashMap<>();
    parameters.put("companyname", companyName);
    sendEmail(CREATE_COMPANY_EXISTING_USER, parameters, contact);
  }

  public void sendAuthorityAdditionExistentUserEmail(String authorityName, String contact) {
    parameters = new HashMap<>();
    parameters.put("authorityName", authorityName);
    sendEmail(CREATE_AUTHORITY_EXISTING_USER, parameters, contact);
  }

  public void sendExistingCompanyExistingUserEmail(String companyName, String contact) {
    parameters = new HashMap<>();
    parameters.put("companyname", companyName);
    sendEmail(EXISTING_COMPANY_EXISTING_USER, parameters, contact);
  }

  public void sendExistingAuthorityExistingUserEmail(String authorityName, String contact) {
    parameters = new HashMap<>();
    parameters.put("authorityName", authorityName);
    sendEmail(EXISTING_AUTHORITY_EXISTING_USER, parameters, contact);
  }

  public void sendThreadRejectedEMail(String reason, String title, String contact) {
    parameters = new HashMap<>();
    parameters.put("title" , title);
    parameters.put("reason", reason);
    sendEmail(THREAD_REJECTED_WITH_REASON, parameters, contact);
  }
  public void sendThreadPublishedEMail(String title, String contact) {
    parameters = new HashMap<>();
    parameters.put("title" , title);
    sendEmail(THREAD_PUBLISHED, parameters, contact);
  }

  public void sendFileUploadFailedEmail(String certificateId, String contact) {
    parameters = new HashMap<>();
    parameters.put(CERTIFICATE_ID_FIELD , certificateId);
    sendEmail(FILE_UPLOAD_FAILED, parameters, contact);
  }

  public void sendFeatureboardEntryAddedEmail(String entryId, String contact) {
    parameters = new HashMap<>();
    parameters.put("link", domainUrl + "/feature-board/" + entryId);
    sendEmail(FEATUREBOARD_ENTRY_ADDED, parameters, contact);
  }

  public void sendCertificateReleaseEmail(String certificateId, String contact) {
    sendGeneralCertificateNotificationEmail(certificateId, contact, CERTIFICATE_RELEASED);
  }

  public void sendCertificateRejectEmail(String certificateId, String contact) {
    sendGeneralCertificateNotificationEmail(certificateId, contact, CERTIFICATE_REJECTED);
  }

  public void sendPrecertificateVotePositiveEmail(String certificateId, String parentCertificateId, String contact) {
    sendGeneralPreCertificateNotificationEmail(certificateId, contact, parentCertificateId,  PRECERTIFICATE_VOTED_POSITIVE);
  }

  public void sendPrecertificateRejectEmail(String certificateId, String parentCertificateId, String contact) {
    sendGeneralPreCertificateNotificationEmail(certificateId, contact, parentCertificateId, PRECERTIFICATE_REJECTED);
  }

  public void sendCertificateUserAssignedEmail(Certificate certificate, String contact) {
    sendGeneralCertificateNotificationEmail(certificate.getId(), contact, CERTIFICATE_USER_ASSIGNED);
  }

  public void sendPreCertificateUserAssignedEmail(Certificate certificate, String contact) {
    sendGeneralCertificateNotificationEmail(certificate.getId(), contact, PRECERTIFICATE_USER_ASSIGNED);
  }

  public void sendCertificateTeamAssignedEmail(Certificate certificate, String teamName, String contact) {
    sendGeneralTeamAssignmentNotificationEmail(certificate.getId(), teamName, contact, CERTIFICATE_TEAM_ASSIGNED);
  }

  public void sendPreCertificateTeamAssignedEmail(Certificate certificate, String teamName, String contact) {
    sendGeneralTeamAssignmentNotificationEmail(certificate.getId(), teamName, contact, PRECERTIFICATE_TEAM_ASSIGNED);
  }

  public void sendCertificateForwardedEmail(CertificateDTO certificateDTO, String contact) {
    sendGeneralForwardNotificationEmail(certificateDTO.getId(), certificateDTO.getCompany().getName(), contact, CERTIFICATE_FORWARDED);
  }

  public void sendCertificateForwardedEndEmail(CertificateDTO certificateDTO, String contact) {
    sendGeneralForwardNotificationEmail(certificateDTO.getId(), certificateDTO.getCompany().getName(), contact, CERTIFICATE_FORWARDED_END);
  }

  public void sendPreCertificateForwardedEmail(CertificateDTO certificateDTO, String contact) {
    sendGeneralForwardNotificationEmail(certificateDTO.getId(), certificateDTO.getParentCertificate().getCompany().getName(), contact, PRECERTIFICATE_FORWARDED);
  }

  public void sendPrecertificationStartedEmail(CertificateDTO certificateDTO, String contact) {
    sendGeneralForwardNotificationEmail(certificateDTO.getId(), certificateDTO.getCompany().getName(), contact, PRECERTIFICATION_STARTED);
  }

  private void sendGeneralCertificateNotificationEmail(String certificateId, String contact, String templateFileName) {
    parameters = new HashMap<>();
    parameters.put(CERTIFICATE_ID_FIELD, certificateId);
    parameters.put(LINK_FIELD, domainUrl + String.format(CERTIFICATE_URL, certificateId));
    sendEmail(templateFileName, parameters, contact);
  }

  private void sendGeneralPreCertificateNotificationEmail(String certificateId, String contact, String parentCertificateId, String templateFileName) {
    parameters = new HashMap<>();
    parameters.put(CERTIFICATE_ID_FIELD, certificateId);
    parameters.put("parentCertificateId", parentCertificateId);
    parameters.put(LINK_FIELD, domainUrl + String.format(CERTIFICATE_URL, parentCertificateId));
    sendEmail(templateFileName, parameters, contact);
  }

  private void sendGeneralTeamAssignmentNotificationEmail(String certificateId, String teamName, String contact, String templateFileName) {
    parameters = new HashMap<>();
    parameters.put(CERTIFICATE_ID_FIELD, certificateId);
    parameters.put("teamName", teamName);
    parameters.put(LINK_FIELD, domainUrl + String.format(CERTIFICATE_URL, certificateId));
    sendEmail(templateFileName, parameters, contact);
  }

  private void sendGeneralForwardNotificationEmail(String certificateId, String companyName, String contact, String templateFileName) {
    parameters = new HashMap<>();
    parameters.put(CERTIFICATE_ID_FIELD, certificateId);
    parameters.put("companyName", companyName);
    parameters.put(LINK_FIELD, domainUrl + String.format(CERTIFICATE_URL, certificateId));
    sendEmail(templateFileName, parameters, contact);
  }

  /**
   * Adds email entry to the DB, sends an email to provided contact address and
   * keep audit for it.
   *
   * @param templateFile
   *            the template file that is used
   * @param data
   *            the data
   * @param contact
   *            the reciepter's email address
   */
  private void sendEmail( String templateFile, Map<String, Object> data, String contact) {
    String emailBody = generateEmailBody(templateFile, data);

    EmailDTO emailDTO = new EmailDTO();
    emailDTO.setEmailType(EMAIL_TYPE.HTML);
    emailDTO.setSubject(MessageConfig.getValue(getSUBJECTS().get(templateFile)));
    emailDTO.setFromEmail(emailSender);
    emailDTO.setToContact(contact);
    emailDTO.setBody(emailBody);

    mailService.queueEmail(emailDTO);
  }

  private String generateEmailBody(String templateFile, Map<String, Object> data) {
    try (Writer out = new StringWriter()) {

      Configuration fTemplate = new freemarker.template.Configuration(Configuration.VERSION_2_3_27);
      fTemplate.setClassForTemplateLoading(getClass(), "/email_templates");
      fTemplate.setDefaultEncoding("UTF-8");
      fTemplate.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      fTemplate.setLogTemplateExceptions(Boolean.FALSE);
      fTemplate.setWrapUncheckedExceptions(Boolean.TRUE);

      Template temp = fTemplate.getTemplate(templateFile);

      temp.process(data, out);

      return out.toString();
    } catch (IOException | TemplateException ex) {
      log.error("Could not create email body for template file: "  + templateFile);
    }
    return "";
  }
}
