package lgl.bayern.de.ecertby.databaseconfig;

import liquibase.change.DatabaseChange;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;

@Slf4j
@Configuration
@ConditionalOnClass({SpringLiquibase.class, DatabaseChange.class})
@ConditionalOnProperty(prefix = "spring.db", name = "drop-schema", havingValue = "true")
@AutoConfigureAfter({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@Import({DBSchemaInit.SpringLiquibaseDependsOnPostProcessor.class})
public class DBSchemaInit {

    @Component
    public static class SchemaInitBean implements InitializingBean {

        private final DataSource dataSource;

        @Autowired
        public SchemaInitBean(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void afterPropertiesSet() {

            String foundSchemaName = null;
            try (Connection conn = dataSource.getConnection(); Statement statement = conn.createStatement()) {
                PreparedStatement stmt = conn.prepareStatement("select schema_name from information_schema.schemata where schema_name = 'ecertby'");
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    foundSchemaName = rs.getString("schema_name");
                }

                // Check if ecertby schema exists on the unit test DB Instance. If yes, drop it and recreate it.
                // After the DBSchemaInit is executed, liquibase scripts will run.
                if (foundSchemaName != null) {
                    log.debug("Dropping and creating new DB schema");
                    statement.execute("DROP SCHEMA ecertby CASCADE");
                    statement.execute("CREATE SCHEMA ecertby");
                    log.debug("New DB Schema 'ecertby' created.");
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create schema.", e);
            }
        }
    }


    @ConditionalOnBean(SchemaInitBean.class)
    static class SpringLiquibaseDependsOnPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

        SpringLiquibaseDependsOnPostProcessor() {
            // Configure the 3rd party SpringLiquibase bean to depend on our SchemaInitBean
            super(SpringLiquibase.class, SchemaInitBean.class);
        }
    }
}
