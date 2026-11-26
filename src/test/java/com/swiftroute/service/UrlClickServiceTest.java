package com.swiftroute.service;

import com.swiftroute.dto.helper.ClickMetadata;
import com.swiftroute.entity.Url;
import com.swiftroute.repository.UrlClickRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UrlClickServiceTest {

    @Mock private UrlClickRepository urlClickRepository;

    @InjectMocks private UrlClickService urlClickService;

    @Test
    void recordClick_MapsMetadataAndSavesClick() {
        Url url = new Url();
        ReflectionTestUtils.setField(url, "id", 1L);

        ClickMetadata metadata = ClickMetadata.builder()
                .url(url)
                .referrer("https://google.com")
                .userAgent("Mozilla/5.0")
                .ipAddress("203.0.113.5")
                .build();

        urlClickService.recordClick(metadata);

        verify(urlClickRepository).save(argThat(click ->
                click.getUrl().equals(url) &&
                        "https://google.com".equals(click.getReferrer()) &&
                        "Mozilla/5.0".equals(click.getUserAgent()) &&
                        "203.0.113.5".equals(click.getIpAddress())
        ));
    }
}
