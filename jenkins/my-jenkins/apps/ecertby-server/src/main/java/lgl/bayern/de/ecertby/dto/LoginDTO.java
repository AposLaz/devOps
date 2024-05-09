package lgl.bayern.de.ecertby.dto;


import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@SuppressWarnings("java:S1068")
public class LoginDTO {
  private String email;
  private String password;
}
