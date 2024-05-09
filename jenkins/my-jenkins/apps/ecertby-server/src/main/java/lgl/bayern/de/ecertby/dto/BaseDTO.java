package lgl.bayern.de.ecertby.dto;

import com.opencsv.bean.CsvBindByPosition;
import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lombok.Data;


@Data
public class BaseDTO {
  @SuppressWarnings("java:S1068")
  @AuditIgnore
  @CsvBindByPosition(position = 0)
  private String id;
}
