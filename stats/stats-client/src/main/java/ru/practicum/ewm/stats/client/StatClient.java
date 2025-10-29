package ru.practicum.ewm.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import ru.practicum.ewm.stats.client.exception.NotAvailableStatServerException;
import ru.practicum.ewm.stats.dto.EndpointHitNewRequest;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.net.URI;
import java.util.List;

@Service
@Slf4j
public class StatClient {
    private final RestClient rest;

    public StatClient(@Value("${stats-server.url}") String statsServerUrl) {
        this.rest = RestClient.builder().baseUrl(statsServerUrl).build();
    }

    public void createHit(EndpointHitNewRequest endpointHitNewRequest) {
        log.info("Вызов метода StatClient.createHit().");
        try {
            rest.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(endpointHitNewRequest)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Выброшено исключение RestClientException.");
            throw new NotAvailableStatServerException("Ошибка при получении статистики:" + e.getMessage());
        }
        log.info("Просмотр отправлен в stat-server.");
    }

    public List<ViewStats> getStats(String start, String end, List<String> uris, Boolean unique) {
        log.info("Вызов метода StatClient.getStats.");
        List<ViewStats> viewStats;
        try {
            viewStats = rest.get()
                    .uri(uriBuilder -> getUri(uriBuilder, start, end, uris, unique))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ViewStats>>() {
                    });

        } catch (RestClientException e) {
            log.warn("Выброшено  исключение RestClientException.");
            throw new NotAvailableStatServerException("Ошибка при получении статистики:" + e.getMessage());
        }
        log.info("Запрос на получение статистики отправлен в stat-server.");
        return viewStats;
    }

    private URI getUri(UriBuilder uriBuilder, String start, String end, List<String> uris, Boolean unique) {
        UriBuilder builder = uriBuilder
                .path("/stats")
                .queryParam("start", start)
                .queryParam("end", end);
        if (uris != null && !uris.isEmpty())
            builder.queryParam("uris", uris.toArray());
        if (unique != null)
            builder.queryParam("unique", unique);
        return builder.build();
    }
}
