package lgl.bayern.de.ecertby.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserTeam extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_USER_DETAIL", nullable = false)
    private UserDetail userDetail;

    @Override
    public boolean equals(final Object obj) {
        boolean result = true;
        if (this == obj) {
            result = true;
        } else if (obj == null) {
            result = false;
        } else if (getClass() != obj.getClass()) {
            result = false;
        } else {
            final UserTeam other = (UserTeam) obj;

            if (getId() == null) {
                if (other.getId() != null) {
                    result = false;
                }
            } else if (!getId().equals(other.getId())) {
                result = false;
            }
            if (stringFieldsEqual(this.userDetail.getId(), other.getUserDetail().getId())) {
                return false;
            }
            if (stringFieldsEqual(this.userDetail.getUsername(), other.getUserDetail().getUsername())) {
                return false;
            }
        }
        return result;
    }

    protected boolean stringFieldsEqual(String newValue, String oldValue) {
        return ((newValue != null && !newValue.equals(oldValue))
                || (newValue == null && oldValue != null));

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userDetail);
    }
}
