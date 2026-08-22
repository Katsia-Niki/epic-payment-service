package by.nikiforova.payment.config;


import jakarta.annotation.PostConstruct;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoLiquibaseConfig {

    @Value("${spring.mongodb.uri}")
    private String mongoUri;
    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    @PostConstruct
    public void migrate() throws Exception {
        String changeLogPath = changeLog.replace("classpath:", "");

        try (MongoLiquibaseDatabase database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, null);
             Liquibase liquibase = new Liquibase(changeLogPath, new ClassLoaderResourceAccessor(), database)) {

            liquibase.update(new Contexts());
        }
    }
}
