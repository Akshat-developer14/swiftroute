package com.swiftroute.service;

import com.swiftroute.dto.helper.ClickMetadata;
import com.swiftroute.entity.UrlClick;
import com.swiftroute.repository.UrlClickRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class UrlClickService {
    private final UrlClickRepository urlClickRepository;

    public UrlClickService(UrlClickRepository urlClickRepository) {
        this.urlClickRepository = urlClickRepository;
    }

    @Async
    public void recordClick(ClickMetadata metadata) {
        UrlClick click = new UrlClick(metadata.getUrl(), metadata.getReferrer(), metadata.getUserAgent(), metadata.getIpAddress());
        urlClickRepository.save(click);
    }

}
