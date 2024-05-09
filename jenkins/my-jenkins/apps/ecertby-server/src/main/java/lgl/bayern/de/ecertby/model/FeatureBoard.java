package lgl.bayern.de.ecertby.model;

import com.eurodyn.qlack.fuse.fd.model.ThreadMessage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "fd_thread_message")
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FeatureBoard  extends ThreadMessage {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_authority")
    private Authority authority;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_company")
    private Company company;


    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "EMAIL")
    private String email;
}
