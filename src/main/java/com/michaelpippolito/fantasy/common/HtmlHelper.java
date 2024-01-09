package com.michaelpippolito.fantasy.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Component
@Slf4j
public class HtmlHelper {
    @Value("${throttle.amountMs}")
    private int throttleMs;

    @Retryable(retryFor = HttpStatusException.class, maxAttempts = 2)
    public Document getHtmlDocument(String url) throws IOException, InterruptedException {
        try {
            Thread.sleep(throttleMs);
            return Jsoup.connect(url).get();
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 429) {
                try {
                    new RestTemplate().getForEntity(url, String.class);
                } catch (HttpClientErrorException ex) {
                    if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                        HttpHeaders headers = ex.getResponseHeaders();
                        String retryAfterHeader = headers.getFirst("Retry-After");
                        int sleepTimeInSeconds = Integer.parseInt(retryAfterHeader);

                        log.error("Too many requests! Sleeping for {} seconds...", sleepTimeInSeconds);
                        Thread.sleep(sleepTimeInSeconds * 1000);
                        log.info("Resuming");
                    } else {
                        log.error(ExceptionUtils.getStackTrace(e));
                    }
                }
            } else {
                log.error(ExceptionUtils.getStackTrace(e));
            }
            throw e;
        } catch (IOException | InterruptedException e) {
            throw e;
        }
    }
}
