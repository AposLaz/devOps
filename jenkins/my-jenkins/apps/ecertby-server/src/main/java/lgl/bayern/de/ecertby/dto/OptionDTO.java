package lgl.bayern.de.ecertby.dto;

import lgl.bayern.de.ecertby.annotation.AuditIdentifier;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class OptionDTO implements Comparable<OptionDTO> {
    private String id;
    @AuditIdentifier
    private String name;
    private String filterId;
    private boolean active;
    public OptionDTO(String id) {
        setId(id);
    }

    @Override
    public int compareTo(OptionDTO optionDTO) {
        if (name != null && optionDTO.getName() != null) {
            return name.compareTo(optionDTO.getName());
        }
        if (name != null) {
            return -1;
        }
        return 1;
    }

}
