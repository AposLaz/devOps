package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.dto.UserDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.SortedSet;
import lgl.bayern.de.ecertby.annotation.AuditIdentifier;
import lgl.bayern.de.ecertby.annotation.AuditTranslationKey;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class UserDetailProfileDTO extends BaseDTO {
  private String salutation;

  @NotNull
  @AuditTranslationKey(key = "first_name")
  private String firstName;

  @NotNull
  @AuditTranslationKey(key = "last_name")
  private String lastName;

  private String fullName;

  @AuditIdentifier
  private String username;

  @NotNull
  private String email;

  @AuditTranslationKey(key = "email_extern")
  private String emailExtern;

  private String telephone;

  @AuditTranslationKey(key = "mobile_number")
  private String mobileNumber;

  @AuditTranslationKey(key = "mobile_dienstnummer")
  private String mobileDienstnummer;

  @AuditTranslationKey(key = "additional_contact_info")
  private String additionalContactInfo;

  @Valid
  @NotEmpty
  private SortedSet<OptionDTO> department;

  private UserDTO user;

  @AuditTranslationKey(key = "company")
  private CompanyDTO  primaryCompany;

  @AuditTranslationKey(key = "authority")
  private AuthorityDTO primaryAuthority;
}
