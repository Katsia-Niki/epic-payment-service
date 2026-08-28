package by.nikiforova.payment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Component
public class RandomNumberClient {

    private final RestClient restClient = RestClient.builder().build();

    @Value("${randomorg.api.url}")
    private String randomApiUrl;

    public int getRandomNumber() {
        String body = restClient.get()
                .uri(URI.create(randomApiUrl))
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Random.org returned empty body");
        }

        return Integer.parseInt(body.trim());
    }
}
