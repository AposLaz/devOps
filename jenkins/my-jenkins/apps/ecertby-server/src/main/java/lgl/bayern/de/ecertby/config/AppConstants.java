package lgl.bayern.de.ecertby.config;

import lombok.Getter;

import java.util.*;

@SuppressWarnings("squid:S1118")
public class AppConstants {

  public static final Locale LOCALE = Locale.GERMAN;

  public static final String AUTHORIZATION = "Authorization";

  public static final String LOG_PREFIX = "ecertBY log: ";

  public static class Audit {

    public static class Event {

      public static final String AUTHENTICATION = "Authentication";
    }

  }

  public static class Jwt {

    // The unique JWT id.

    public static final String OPERATIONS = "operations";

    public static final String USER_DETAIL_DTO = "userDetailDTO";
    public static final String ACTIVE_SELECTION_FROM_DD = "activeSelectionFromDD";

    public static final String JWT = "jwt";

    public static final String JWT_STATUS = "jwtStatus";

    //only to be used on first login
    public static final String FIRST_JWT = "first-jwt";

    //to be used on renew due to inactive authority/company/user
    public static final String UPDATED_JWT = "updated-jwt";

    //to be used on refresh token
    public static final String REFRESH_JWT = "refresh-jwt";
  }

  public static class Email {
    public static final String CERTIFICATE_FORWARDED_GENERAL_SUBJECT = "certificate_forwarded_subject";
    public static final String CREATE_USER = "new_user.ftl";
    public static final String LINK_USER_WITH_TEAM = "user_linked_with_team.ftl";
    public static final String FILE_HAS_VIRUS = "file_has_virus.ftl";
    public static final String THREAD_REJECTED_WITH_REASON = "thread_rejected_with_reason.ftl";
    public static final String THREAD_PUBLISHED = "thread_published.ftl";
    public static final String FILE_UPLOAD_FAILED = "file_upload_failed.ftl";
    public static final String CREATE_COMPANY_EXISTING_USER = "new_company_existing_user.ftl";
    public static final String CREATE_AUTHORITY_EXISTING_USER = "new_authority_existing_user.ftl";
    public static final String EXISTING_COMPANY_EXISTING_USER = "existing_company_existing_user.ftl";
    public static final String EXISTING_AUTHORITY_EXISTING_USER = "existing_authority_existing_user.ftl";
    public static final String LOCK_USER = "lock_user.ftl";
    public static final String CERTIFICATE_USER_ASSIGNED = "certificate_assigned_to_user.ftl";
    public static final String CERTIFICATE_TEAM_ASSIGNED = "certificate_assigned_to_team.ftl";
    public static final String PRECERTIFICATE_USER_ASSIGNED = "precertificate_assigned_to_user.ftl";
    public static final String PRECERTIFICATE_TEAM_ASSIGNED = "precertificate_assigned_to_team.ftl";
    public static final String CERTIFICATE_RELEASED = "certificate_released.ftl";
    public static final String CERTIFICATE_REJECTED = "certificate_rejected.ftl";
    public static final String PRECERTIFICATE_VOTED_POSITIVE = "precertificate_voted_positive.ftl";
    public static final String PRECERTIFICATE_REJECTED = "precertificate_rejected.ftl";
    public static final String CERTIFICATE_FORWARDED = "certificate_forwarded.ftl";
    public static final String CERTIFICATE_FORWARDED_END = "certificate_forwarded_end.ftl";
    public static final String PRECERTIFICATE_FORWARDED = "precertificate_forwarded.ftl";
    public static final String PRECERTIFICATION_STARTED = "certificate_forwarded_with_precertificates.ftl";
    public static final String FEATUREBOARD_ENTRY_ADDED = "thread_published_notification.ftl";
    @Getter
    private static final Map<String,String> SUBJECTS = new HashMap<>();

    static {
      SUBJECTS.put(CREATE_USER, "create_user_subject");
      SUBJECTS.put(LINK_USER_WITH_TEAM,"link_user_with_team_subject");
      SUBJECTS.put(THREAD_REJECTED_WITH_REASON, "thread_rejected_subject");
      SUBJECTS.put(THREAD_PUBLISHED,"thread_published_subject");
      SUBJECTS.put(CREATE_COMPANY_EXISTING_USER,"create_company_subject");
      SUBJECTS.put(FILE_UPLOAD_FAILED,"file_upload_failed_subject");
      SUBJECTS.put(FILE_HAS_VIRUS,"file_has_virus_subject");
      SUBJECTS.put(CREATE_AUTHORITY_EXISTING_USER,"create_authority_subject");
      SUBJECTS.put(EXISTING_COMPANY_EXISTING_USER,"create_company_subject");
      SUBJECTS.put(EXISTING_AUTHORITY_EXISTING_USER,"create_authority_subject");
      SUBJECTS.put(LOCK_USER,"lock_user_subject");
      SUBJECTS.put(CERTIFICATE_USER_ASSIGNED,"certificate_assigned_subject");
      SUBJECTS.put(CERTIFICATE_TEAM_ASSIGNED,"certificate_assigned_subject");
      SUBJECTS.put(PRECERTIFICATE_USER_ASSIGNED,"precertificate_assigned_subject");
      SUBJECTS.put(PRECERTIFICATE_TEAM_ASSIGNED,"precertificate_assigned_subject");
      SUBJECTS.put(CERTIFICATE_RELEASED,"company_certificate_released_subject");
      SUBJECTS.put(CERTIFICATE_REJECTED,"company_certificate_rejected_subject");
      SUBJECTS.put(PRECERTIFICATE_VOTED_POSITIVE,"company_precertificate_voted_positive_subject");
      SUBJECTS.put(PRECERTIFICATE_REJECTED,"company_precertificate_rejected_subject");
      SUBJECTS.put(CERTIFICATE_FORWARDED, CERTIFICATE_FORWARDED_GENERAL_SUBJECT);
      SUBJECTS.put(CERTIFICATE_FORWARDED_END, CERTIFICATE_FORWARDED_GENERAL_SUBJECT);
      SUBJECTS.put(PRECERTIFICATE_FORWARDED, "precertificate_forwarded_subject");
      SUBJECTS.put(PRECERTIFICATION_STARTED, CERTIFICATE_FORWARDED_GENERAL_SUBJECT);
      SUBJECTS.put(FEATUREBOARD_ENTRY_ADDED, "featureboard_entry_added_subject");
    }
  }

  public static class EnumTranslations {

    public static final String ADMIN_USER = "Systemadministrator";

    public static final String AUTHORITY_USER = "Behördebenutzer";

    public static final String COMPANY_USER = "Betriebsbenutzer";

    public static final String CREATE = "Anlegen";

    public static final String UPDATE = "Bearbeiten";

    public static final String ACTIVATE = "Aktivieren";

    public static final String DEACTIVATE = "Deaktivieren";

    public static final String DELETE = "Löschen";

    public static final String ACCOUNT_UPDATE = "Konto bearbeiten";

    public static final String AUTHORITY = "Behörde";

    public static final String COMPANY = "Betrieb";

    public static final String USER = "Benutzer";

    public static final String TEAM = "Team";

    public static final String CERTIFICATE = "Zertifikat";

    public static final String TEMPLATE = "Vorlage";

    public static final String REJECTED  = "Abgelehnt";

    public static final String PUBLISHED  = "Veröffentlicht";

    public static final String REQUESTED  = "Angefordert";

    // CERTIFICATE STATUSES
    public static final String DRAFT = "Entwurf";
    public static final String FORWARDED = "Antrag";
    public static final String FORWARDED_PRE_CERTIFICATE_REJECTED = "Antrag [Vorzertifikat(e) abgelehnt]";
    public static final String RELEASED = "Freigegeben";
    public static final String REJECTED_CERTIFICATE = "Antrag abgelehnt";
    public static final String REVOKED = "Zurückgerufen/gesperrt";
    public static final String LOST = "Zurückgegeben/verloren";
    public static final String BLOCKED = "Antrag blockiert";
    public static final String DELETED = "Entwurf gelöscht";
    public static final String PRE_CERTIFICATE_DRAFT = "Vorzertifikat Entwurf";
    public static final String PRE_CERTIFICATE_FORWARDED = "Vorzertifikat Antrag";
    public static final String PRE_CERTIFICATE_REJECTED = "Vorzertifikat Antrag abgelehnt";
    public static final String PRE_CERTIFICATE_EXCLUDED = "Vorzertifikat Ausgeschlossen";
    public static final String PRE_CERTIFICATE_VOTE_POSITIVE = "Vorzertifikat Positiv bewertet";
    public static final String PRE_CERTIFICATE_DELETED = "Vorzertifikat Entwurf gelöscht";
  }

  public static class Operations {

    //User operations
    public static final String VIEW_USERS_LIST = "VIEW_USERS_LIST";
    public static final String NEW_USER = "NEW_USER";
    public static final String EDIT_USER = "EDIT_USER";
    public static final String ACTIVATE_USER = "ACTIVATE_USER";
    public static final String VIEW_USER = "VIEW_USER";

    // Authority operations
    public static final String VIEW_AUTHORITIES_LIST = "VIEW_AUTHORITIES_LIST";
    public static final String NEW_AUTHORITY = "NEW_AUTHORITY";
    public static final String EDIT_AUTHORITY = "EDIT_AUTHORITY";
    public static final String ACTIVATE_AUTHORITY = "ACTIVATE_AUTHORITY";
    public static final String VIEW_AUTHORITY = "VIEW_AUTHORITY";

    // Team operations
    public static final String VIEW_TEAMS_LIST = "VIEW_TEAMS_LIST";
    public static final String NEW_TEAM = "NEW_TEAM";
    public static final String EDIT_TEAM = "EDIT_TEAM";
    public static final String DELETE_TEAM = "DELETE_TEAM";

    public static final String VIEW_TEAM = "VIEW_TEAM";

    // Company operations
    public static final String VIEW_COMPANY = "VIEW_COMPANY";
    public static final String VIEW_COMPANIES_LIST = "VIEW_COMPANIES_LIST";
    public static final String NEW_COMPANY = "NEW_COMPANY";
    public static final String EDIT_COMPANY = "EDIT_COMPANY";
    public static final String DELETE_COMPANY = "DELETE_COMPANY";
    public static final String ACTIVATE_COMPANY = "ACTIVATE_COMPANY";

    //Profile operations
    public static final String NEW_PROFILE = "NEW_PROFILE";
    public static final String EDIT_PROFILE = "EDIT_PROFILE";
    public static final String DELETE_PROFILE = "DELETE_PROFILE";
    public static final String VIEW_PROFILE = "VIEW_PROFILE";
    public static final String ACTIVATE_PROFILE = "ACTIVATE_PROFILE";

    // Protocol operation
    public static final String VIEW_PROTOCOL = "VIEW_PROTOCOL";

    public static final String   VIEW_LAST_MODIFIED_BY = "VIEW_LAST_MODIFIED_BY";

    // Feature board operation
    public static final String VIEW_FEATURE_BOARD_LIST = "VIEW_FEATURE_BOARD_LIST";
    public static final String CREATE_THREAD = "CREATE_THREAD";
    public static final String VIEW_THREAD = "VIEW_THREAD";
    public static final String REJECT_THREAD = "REJECT_THREAD";
    public static final String PUBLISH_THREAD = "PUBLISH_THREAD";
    public static final String VOTE = "VOTE";
    public static final String COMMENT = "COMMENT";
    // Certificates operation
    public static final String NEW_CERTIFICATE = "NEW_CERTIFICATE";
    public static final String VIEW_CERTIFICATES_LIST = "VIEW_CERTIFICATES_LIST";
    public static final String VIEW_CERTIFICATE = "VIEW_CERTIFICATE";
    public static final String EDIT_CERTIFICATE = "EDIT_CERTIFICATE";
    public static final String REJECT_CERTIFICATE = "REJECT_CERTIFICATE";
    public static final String REVOKE_CERTIFICATE = "REVOKE_CERTIFICATE";
    public static final String MARK_CERTIFICATE_AS_LOST = "MARK_CERTIFICATE_AS_LOST";
    public static final String BLOCK_CERTIFICATE = "BLOCK_CERTIFICATE";
    public static final String DELETE_CERTIFICATE = "DELETE_CERTIFICATE";
    public static final String RELEASE_CERTIFICATE = "RELEASE_CERTIFICATE";
    public static final String FORWARD_CERTIFICATE = "FORWARD_CERTIFICATE";
    public static final String AUTHORITY_FORWARD_CERTIFICATE = "AUTHORITY_FORWARD_CERTIFICATE";
    public static final String VIEW_RECYCLE_BIN = "VIEW_RECYCLE_BIN";
    public static final String VOTE_POSITIVE = "VOTE_POSITIVE";
    public static final String EXCLUDE_CERTIFICATE = "EXCLUDE_CERTIFICATE";

    // Template operation
    public static final String VIEW_TEMPLATE_LIST = "VIEW_TEMPLATE_LIST";
    public static final String NEW_TEMPLATE = "NEW_TEMPLATE";
    public static final String EDIT_TEMPLATE = "EDIT_TEMPLATE";
    public static final String VIEW_TEMPLATE = "VIEW_TEMPLATE";
    public static final String ACTIVATE_TEMPLATE = "ACTIVATE_TEMPLATE";
    public static final String RELEASE_TEMPLATE = "RELEASE_TEMPLATE";
    // Catalog operation
    public static final String VIEW_CATALOG_LIST = "VIEW_CATALOG_LIST";
    public static final String VIEW_CATALOG = "VIEW_CATALOG";
    public static final String NEW_CATALOG = "NEW_CATALOG";
    public static final String EDIT_CATALOG = "EDIT_CATALOG";
    public static final String DELETE_CATALOG = "DELETE_CATALOG";
    // Notification operation
    public static final String VIEW_NOTIFICATION_LIST = "VIEW_NOTIFICATION_LIST";
    public static final String NEW_NOTIFICATION= "NEW_NOTIFICATION";
    public static final String EDIT_NOTIFICATION = "EDIT_NOTIFICATION";
    public static final String VIEW_NOTIFICATION = "VIEW_NOTIFICATION";
    public static final String ACTIVATE_NOTIFICATION = "ACTIVATE_NOTIFICATION";
    public static final String PUBLISH_NOTIFICATION = "PUBLISH_NOTIFICATION";
    public static final String DELETE_NOTIFICATION = "DELETE_NOTIFICATION";

    // Email Notification operation
    public static final String VIEW_EMAIL_NOTIFICATION = "VIEW_EMAIL_NOTIFICATION";

    // Task operation
    public static final String VIEW_TASKS_LIST = "VIEW_TASKS_LIST";
    public static final String COMPLETE_TASK = "COMPLETE_TASK";

    //Search Criteria Operations
    public static final String VIEW_SEARCH_CRITERIA_LIST = "VIEW_SEARCH_CRITERIA_LIST";
    public static final String VIEW_SEARCH_CRITERIA = "VIEW_SEARCH_CRITERIA";
    public static final String NEW_SEARCH_CRITERIA= "NEW_SEARCH_CRITERIA";
    public static final String EDIT_SEARCH_CRITERIA= "EDIT_SEARCH_CRITERIA";
    public static final String DELETE_SEARCH_CRITERIA = "DELETE_SEARCH_CRITERIA";

    // Attribute operations
    public static final String  VIEW_ATTRIBUTE_LIST = "VIEW_ATTRIBUTE_LIST";
    public static final String  VIEW_ATTRIBUTE =  "VIEW_ATTRIBUTE";
    public static final String  NEW_ATTRIBUTE =  "NEW_ATTRIBUTE";
    public static final String  EDIT_ATTRIBUTE = "EDIT_ATTRIBUTE";
    public static final String  DELETE_ATTRIBUTE = "DELETE_ATTRIBUTE";


    public static final List<String> EXCLUDE_OPS = List.of(VIEW_AUTHORITIES_LIST, VIEW_PROTOCOL, NEW_AUTHORITY, ACTIVATE_AUTHORITY,
            VIEW_TEMPLATE_LIST, NEW_TEMPLATE, EDIT_TEMPLATE, VIEW_TEMPLATE, ACTIVATE_TEMPLATE, RELEASE_TEMPLATE);

    public static final List<String> ADMIN_RIGHTS = List.of(ACTIVATE_AUTHORITY, ACTIVATE_COMPANY, ACTIVATE_PROFILE, ACTIVATE_TEMPLATE, ACTIVATE_USER,
            COMMENT, DELETE_COMPANY, DELETE_PROFILE, EDIT_AUTHORITY, EDIT_COMPANY, EDIT_PROFILE, EDIT_TEMPLATE, EDIT_USER, NEW_AUTHORITY, NEW_COMPANY,
            NEW_PROFILE, NEW_TEMPLATE, NEW_USER, PUBLISH_THREAD, REJECT_THREAD, RELEASE_TEMPLATE,
            VIEW_AUTHORITIES_LIST, VIEW_AUTHORITY, VIEW_CERTIFICATE, VIEW_CERTIFICATES_LIST,
            VIEW_COMPANIES_LIST, VIEW_COMPANY, VIEW_FEATURE_BOARD_LIST, VIEW_PROFILE,
            VIEW_PROTOCOL, VIEW_TEMPLATE, VIEW_TEMPLATE_LIST, VIEW_THREAD, VIEW_USER,
            VIEW_USERS_LIST);

    public static final List<String> AUTHORITY_OPERATIONS = List.of(ACTIVATE_COMPANY, ACTIVATE_PROFILE, ACTIVATE_USER,
            BLOCK_CERTIFICATE, COMMENT, CREATE_THREAD, DELETE_COMPANY, DELETE_PROFILE, DELETE_TEAM,
            EDIT_AUTHORITY, EDIT_CERTIFICATE, EDIT_COMPANY, EDIT_PROFILE, EDIT_TEAM,
            EDIT_USER, MARK_CERTIFICATE_AS_LOST, NEW_COMPANY, NEW_PROFILE, NEW_TEAM, NEW_USER, REJECT_CERTIFICATE,
            RELEASE_CERTIFICATE, REVOKE_CERTIFICATE, VIEW_AUTHORITY, VIEW_CERTIFICATE, VIEW_CERTIFICATES_LIST,
            VIEW_COMPANIES_LIST, VIEW_COMPANY, VIEW_FEATURE_BOARD_LIST, VIEW_PROFILE, VIEW_TEAM,
            VIEW_TEAMS_LIST, VIEW_THREAD, VIEW_USER, VIEW_USERS_LIST, VOTE
    );

    public static final List<String> COMPANY_OPERATIONS = List.of(ACTIVATE_PROFILE, ACTIVATE_USER, COMMENT, CREATE_THREAD, DELETE_CERTIFICATE,
            DELETE_PROFILE, DELETE_TEAM, EDIT_CERTIFICATE, EDIT_COMPANY,
            EDIT_PROFILE, EDIT_TEAM, EDIT_USER, FORWARD_CERTIFICATE, NEW_CERTIFICATE, NEW_PROFILE,
            NEW_TEAM, NEW_USER, VIEW_CERTIFICATE, VIEW_CERTIFICATES_LIST, VIEW_COMPANY, VIEW_FEATURE_BOARD_LIST,
            VIEW_PROFILE, VIEW_TEAM, VIEW_TEAMS_LIST, VIEW_THREAD, VIEW_USER, VIEW_USERS_LIST, VOTE );
  }

  public static class Resource {
    public static final String ADMIN_RESOURCE = "ADMIN_RESOURCE";
    public static final String ADMIN_RESOURCE_ID = "aaaddd00-fb08-4fec-ae6e-8fcc85b258cf";
  }
  public static class SelectionUtils {
    public static final String ACTIVE = "isActive";
    public static final String DELETED = "isDeleted";
  }

  public static class CatalogNames {
    public static final String DEPARTMENT = "Fachbereich";
    public static final String KEYWORD = "Schlagwort";
    public static final String PRODUCT = "Produkt";
  }

  //uuid of first (root) user, he should not be deactivated
  public static final String ROOT_UUID = "eb38afa4-de72-4c1c-b2ae-cbdff9b042b0";
}
