package com.jhssong.errorping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ErrorpingService {

    private final String channelId;
    private final WebClient webClient;

    public ErrorpingService(ErrorpingProperties props) {
        this.channelId = props.getChannelId();
        this.webClient = WebClient.builder()
                .baseUrl("https://errorping.jhssong.com")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-api-key", props.getApiKey())
                .build();
    }

    private void post(MessageType messageType, Map<String, Object> body) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("channelId", this.channelId);
        requestBody.put("messageType", messageType.name());
        requestBody.put("body", body);

        this.webClient.post()
                .uri("")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.debug("Errorping request sent successfully"),
                        throwable -> log.warn("Failed to send error to Errorping: {}", throwable.getMessage())
                );
    }

    public void sendError(ProblemDetail res) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", res.getTitle());
        body.put("status", res.getStatus());
        body.put("detail", res.getDetail());
        body.put("instance", res.getInstance());
        if (res.getProperties() != null) {
            body.put("method", res.getProperties().get("method"));
            body.put("timestamp", res.getProperties().get("timestamp"));
        }

        post(MessageType.ERROR, body);
    }

    public void sendInfo(String title, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("message", message);

        post(MessageType.INFO, body);
    }

}
