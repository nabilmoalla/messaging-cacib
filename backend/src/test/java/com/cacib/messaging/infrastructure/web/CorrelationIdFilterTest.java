package com.cacib.messaging.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesAndEchoesBackAnIdWhenNoneProvided() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(CorrelationIdFilter.HEADER_NAME)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setHeader(org.mockito.ArgumentMatchers.eq(CorrelationIdFilter.HEADER_NAME), any(String.class));
        verify(chain).doFilter(request, response);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void reusesAnIncomingCorrelationId() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(CorrelationIdFilter.HEADER_NAME)).thenReturn("incoming-id-123");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(CorrelationIdFilter.HEADER_NAME, "incoming-id-123");
    }

    @Test
    void clearsMdcEvenWhenTheChainThrows() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(CorrelationIdFilter.HEADER_NAME)).thenReturn("incoming-id-123");

        try {
            org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);
            filter.doFilter(request, response, chain);
        } catch (Exception ignored) {
            // expected — we only care that MDC was cleaned up regardless
        }

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
