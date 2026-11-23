package com.swiftroute.service;

import com.swiftroute.dto.cache.CachedUrl;
import com.swiftroute.dto.request.CreateUrlRequest;
import com.swiftroute.dto.response.UrlAnalyticsResponse;
import com.swiftroute.dto.response.UrlClickResponse;
import com.swiftroute.dto.response.UrlResponse;
import com.swiftroute.entity.Url;
import com.swiftroute.entity.UrlClick;
import com.swiftroute.entity.User;
import com.swiftroute.exception.AliasAlreadyTakenException;
import com.swiftroute.exception.UrlExpiredException;
import com.swiftroute.exception.UrlNotFoundException;
import com.swiftroute.mapper.UrlClickMapper;
import com.swiftroute.mapper.UrlMapper;
import com.swiftroute.repository.UrlClickRepository;
import com.swiftroute.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UrlServiceTest {

    @Mock private UrlRepository urlRepository;

    @Mock private UrlClickService urlClickService;

    @Mock private UrlMapper urlMapper;

    @Mock private UrlClickMapper urlClickMapper;

    @Mock private UrlClickRepository urlClickRepository;

    @Mock private UrlCacheService urlCacheService;

    @InjectMocks private UrlService urlService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Akshat");
        testUser.setEmail("akshat@email.com");
        testUser.setPasswordHash("hashedPassword");
        ReflectionTestUtils.setField(testUser, "id", 7021L);
    }

    // -- Create Url Method Tests --

    @Test
    void shouldCreateShortUrlSuccessfullyWhenAliasIsGiven() {
        Instant expiry = Instant.now().plusSeconds(3600);
        CreateUrlRequest request = new CreateUrlRequest("https://google.com", "search", expiry);

        Url savedUrl = new Url("https://google.com", "search", expiry, testUser);
        ReflectionTestUtils.setField(savedUrl, "id", 100L);

        UrlResponse expectedResponse = new UrlResponse("search", "https://google.com", expiry, Instant.now());

        when(urlRepository.existsByShortCode("search")).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);
        when(urlMapper.toResponse(savedUrl)).thenReturn(expectedResponse);

        UrlResponse actualResponse = urlService.createShortUrl(request, testUser);

        assertEquals(expectedResponse, actualResponse);
        verify(urlCacheService).put(eq("search"), any(CachedUrl.class));
        verify(urlRepository).save(argThat(url ->
                url.getOriginalUrl().equals("https://google.com") &&
                url.getShortCode().equals("search") &&
                url.getUser().equals(testUser)
                ));
    }

    @Test
    void shouldThrowAliasAlreadyTakenExceptionInCreateShortUrlWhenAliasAlreadyExists() {
        CreateUrlRequest request = new CreateUrlRequest("https://google.com", "search", null);
        when(urlRepository.existsByShortCode("search")).thenReturn(true);

        assertThrows(AliasAlreadyTakenException.class, () ->
                urlService.createShortUrl(request, testUser)
        );

        verify(urlRepository, never()).save(any());
        verify(urlCacheService, never()).put(any(), any());
    }

    @Test
    void shouldCreateShortUrlSuccessfullyWhenAliasIsNotGiven() {
        CreateUrlRequest request = new CreateUrlRequest("https://google.com", null, null);
        Url savedUrl = new Url("https://google.com", "search-it", null, testUser);
        ReflectionTestUtils.setField(savedUrl, "id", 1L);

        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);
        when(urlMapper.toResponse(savedUrl)).thenReturn(new UrlResponse("search-it", "https://google.com", null, Instant.now()));

        urlService.createShortUrl(request, testUser);

        verify(urlRepository).save(argThat(url ->
                url.getShortCode() != null && !url.getShortCode().isBlank()
        ));
        verify(urlCacheService).put(eq("search-it"), any(CachedUrl.class));
    }

    // -- Get All Url Method Tests --

    @Test
    void shouldGetAllTheUrls_ReturnsListOfUrls() {
        Url url1 = new Url("https://example.com/1", "code1", null, testUser);
        Url url2 = new Url("https://example.com/2", "code2", null, testUser);
        Url url3 = new Url("https://example.com/3", "code3", null, testUser);
        List<Url> userUrls = List.of(url1, url2, url3);

        UrlResponse response1 = new UrlResponse("code1", "https://example.com/1", null, Instant.now());
        UrlResponse response2 = new UrlResponse("code2", "https://example.com/2", null, Instant.now());
        UrlResponse response3 = new UrlResponse("code3", "https://example.com/3", null, Instant.now());
        List<UrlResponse> expectedResponses = List.of(response1, response2, response3);

        when(urlRepository.findByUserId(testUser.getId())).thenReturn(userUrls);
        when(urlMapper.toResponseList(userUrls)).thenReturn(expectedResponses);

        List<UrlResponse> actualResponses = urlService.getMyUrls(testUser);

        assertEquals(3, actualResponses.size());
        assertEquals(expectedResponses, actualResponses);
        verify(urlRepository).findByUserId(testUser.getId());
        verify(urlMapper).toResponseList(userUrls);
    }

    @Test
    void getMyUrls_WhenNoUrlsExist_ReturnsEmptyList() {
        when(urlRepository.findByUserId(testUser.getId())).thenReturn(List.of());
        when(urlMapper.toResponseList(List.of())).thenReturn(List.of());

        List<UrlResponse> actualResponses = urlService.getMyUrls(testUser);

        assertTrue(actualResponses.isEmpty());
        verify(urlRepository).findByUserId(testUser.getId());
    }

    // -- Delete Url Method Tests --

    @Test
    void shouldDeleteUrlSuccessfully() {
        String shortCode = "short";
        Url url = new Url("https://example.com", shortCode, null, testUser);
        ReflectionTestUtils.setField(url, "id", 100L);

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        urlService.deleteUrl(shortCode, testUser);

        verify(urlClickRepository).deleteByUrlId(100L);
        verify(urlRepository).delete(url);
        verify(urlCacheService).evict(shortCode);
    }

    @Test
    void deleteUrl_NonExistingUrl_ThrowUrlNotFoundException() {
        String shortCode = "missing";
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () ->
            urlService.deleteUrl(shortCode, testUser)
        );

        verify(urlClickRepository, never()).deleteByUrlId(any());
        verify(urlRepository, never()).delete(any());
        verify(urlCacheService, never()).evict(any());
    }

    @Test
    void deleteUrl_UrlNotOfUser_AccessDeniedException() {
        String shortCode = "short";
        User anotherUser = new User();
        ReflectionTestUtils.setField(anotherUser, "id", 1L);

        Url url = new Url("https://example.com", shortCode, null, testUser);
        ReflectionTestUtils.setField(url, "id", 100L);

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        assertThrows(AccessDeniedException.class, () ->
                urlService.deleteUrl(shortCode, anotherUser)
        );

        verify(urlClickRepository, never()).deleteByUrlId(any());
        verify(urlRepository, never()).delete(any());
        verify(urlCacheService, never()).evict(any());
    }

    // -- Original Url & Log Click Method Tests --

    @Test
    void getOriginalUrl_CacheHit_ReturnsUrlAndLogsClick() {
        String shortCode = "short";
        CachedUrl cachedUrl = new CachedUrl(10L, "https://example.com", Instant.now().plusSeconds(3600));
        Url proxyUrl = new Url();
        ReflectionTestUtils.setField(proxyUrl, "id", 10L);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("Referer")).thenReturn("https://google.com");

        when(urlCacheService.get(shortCode)).thenReturn(Optional.of(cachedUrl));
        when(urlRepository.getReferenceById(10L)).thenReturn(proxyUrl);

        String result = urlService.getOriginalUrlAndLogClick(shortCode, request);

        assertEquals("https://example.com", result);
        verify(urlRepository, never()).findByShortCode(any());
        verify(urlCacheService, never()).put(any(), any());
        verify(urlClickService).recordClick(argThat(meta ->
                meta.getUrl().equals(proxyUrl) &&
                        meta.getIpAddress().equals("203.0.113.5") &&
                        meta.getUserAgent().equals("Mozilla/5.0") &&
                        meta.getReferrer().equals("https://google.com")
        ));
    }

    @Test
    void getOriginalUrl_CacheHitButExpired_ThrowsUrlExpiredException() {
        String shortCode = "expired1";
        CachedUrl expiredCached = new CachedUrl(10L, "https://example.com", Instant.now().minusSeconds(60));
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(urlCacheService.get(shortCode)).thenReturn(Optional.of(expiredCached));

        assertThrows(UrlExpiredException.class, () ->
                urlService.getOriginalUrlAndLogClick(shortCode, request)
        );

        verifyNoInteractions(urlClickService);
        verify(urlRepository, never()).getReferenceById(any());
    }

    @Test
    void getOriginalUrl_CacheMiss_FetchesFromDbCachesAndLogsClick() {
        String shortCode = "short";
        Url dbUrl = new Url("https://example.com", shortCode, null, testUser);
        ReflectionTestUtils.setField(dbUrl, "id", 10L);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");

        when(urlCacheService.get(shortCode)).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(dbUrl));

        String result = urlService.getOriginalUrlAndLogClick(shortCode, request);

        assertEquals("https://example.com", result);
        verify(urlCacheService).put(eq(shortCode), argThat(cached ->
                cached.id().equals(10L) && cached.originalUrl().equals("https://example.com")
        ));
        verify(urlClickService).recordClick(argThat(meta ->
                meta.getIpAddress().equals("192.168.1.50")
        ));
    }

    @Test
    void getOriginalUrl_WhenShortCodeNotExist_ThrowsUrlNotFoundException() {
        String shortCode = "unknown";
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(urlCacheService.get(shortCode)).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () ->
                urlService.getOriginalUrlAndLogClick(shortCode, request)
        );

        verify(urlCacheService, never()).put(any(), any());
        verifyNoInteractions(urlClickService);
    }

    @Test
    void getOriginalUrl_CacheMissAndDbExpired_ThrowsUrlExpiredException() {
        String shortCode = "dbExpired";
        Url expiredUrl = new Url("https://example.com", shortCode, Instant.now().minusSeconds(10), testUser);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(urlCacheService.get(shortCode)).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(expiredUrl));

        assertThrows(UrlExpiredException.class, () ->
                urlService.getOriginalUrlAndLogClick(shortCode, request)
        );

        verify(urlCacheService, never()).put(any(), any());
        verifyNoInteractions(urlClickService);
    }

    // -- Url Analytics Method Tests --

    @Test
    void getUrlAnalytics_Success_ReturnsAnalyticsResponse() {
        String shortCode = "short";
        Url url = new Url("https://example.com", shortCode, null, testUser);
        ReflectionTestUtils.setField(url, "id", 1L);

        UrlClick click1 = new UrlClick();
        UrlClick click2 = new UrlClick();
        List<UrlClick> recentClicks = List.of(click1, click2);
        Page<UrlClick> clickPage = new PageImpl<>(recentClicks);

        List<UrlClickResponse> clickResponses = List.of(
                new UrlClickResponse(Instant.now().minusSeconds(3600), "https://google.com", "Mozilla/5.0", "192.168.1.50"),
                new UrlClickResponse(Instant.now().minusSeconds(4800), "https://wikipedia.org", "Mozilla/5.0", "192.168.1.52")
        );

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));
        when(urlClickRepository.countByUrlId(1L)).thenReturn(42L);
        when(urlClickRepository.findByUrlId(eq(1L), any(Pageable.class))).thenReturn(clickPage);
        when(urlClickMapper.toResponseList(recentClicks)).thenReturn(clickResponses);

        UrlAnalyticsResponse response = urlService.getUrlAnalytics(shortCode, testUser);

        assertEquals(shortCode, response.shortCode());
        assertEquals("https://example.com", response.originalUrl());
        assertEquals(42L, response.totalClicks());
        assertEquals(clickResponses, response.recentClicks());

        verify(urlClickRepository).findByUrlId(eq(1L), argThat(pageable ->
                pageable.getPageNumber() == 0 &&
                        pageable.getPageSize() == 10 &&
                        Sort.Order.desc("clickedAt").equals(pageable.getSort().getOrderFor("clickedAt"))
        ));
    }

    @Test
    void getUrlAnalytics_WhenUrlNotFound_ThrowsUrlNotFoundException() {
        String shortCode = "missing";
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () ->
                urlService.getUrlAnalytics(shortCode, testUser)
        );

        verifyNoInteractions(urlClickRepository, urlClickMapper);
    }

    @Test
    void getUrlAnalytics_WhenUserDoesNotOwnUrl_ThrowsAccessDeniedException() {
        User anotherUser = new User();
        ReflectionTestUtils.setField(anotherUser, "id" , 9L);

        String shortCode = "short";
        Url url = new Url("https://example.com", shortCode, null, testUser);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        assertThrows(AccessDeniedException.class, () ->
                urlService.getUrlAnalytics(shortCode, anotherUser)
        );

        verifyNoInteractions(urlClickRepository, urlClickMapper);
    }

}
