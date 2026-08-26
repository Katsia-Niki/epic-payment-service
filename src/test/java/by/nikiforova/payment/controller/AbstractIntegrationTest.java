package by.nikiforova.payment.controller;

import by.nikiforova.payment.repository.PaymentRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {
    protected static final String JWT_SECRET = "test-secret-key-test-secret-key-test-secret-key";

    static final MongoDBContainer MONGO =
            new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    protected static final WireMockServer WIRE_MOCK =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        MONGO.start();
        WIRE_MOCK.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add("random.api.url",
                () -> "http://localhost:" + WIRE_MOCK.port() + "/integers");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected JsonMapper objectMapper;
    @Autowired
    protected PaymentRepository paymentRepository;
}
