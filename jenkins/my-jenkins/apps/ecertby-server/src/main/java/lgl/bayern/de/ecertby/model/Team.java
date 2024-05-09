package lgl.bayern.de.ecertby.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Team extends BaseEntity {
    @NotNull
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_COMPANY", nullable = true)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_AUTHORITY", nullable = true)
    private Authority authority;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="FK_TEAM", nullable = false)
    private Set<TeamDepartment> department;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="FK_TEAM", nullable = false)
    private Set<UserTeam> userTeamSet;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "team", cascade = {CascadeType.ALL})
    private List<CertificateTeam> certificateTeamsList = new ArrayList<>(0);

    @Override
    public String toString() {
        return "Team{" +
                "name='" + name + '\'' +
                ", company=" + company +
                ", authority=" + authority +
                ", department=" + department +
                ", userTeamSet=" + userTeamSet +
                '}';
    }
}
