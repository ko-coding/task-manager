package io.github.kocoding.taskmanager.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_有効なトークン() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtProvider.parseUserId("valid-token"))
                .thenReturn(1L);
        jwtAuthenticationFilter.doFilter(request, response, chain);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(1L, authentication.getPrincipal());
        // チェーンが呼ばれた
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_ヘッダーなし() throws Exception {
        jwtAuthenticationFilter.doFilter(request, response, chain);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        assertNotNull(chain.getRequest());
        verify(jwtProvider, never()).parseUserId(any());
    }

    @Test
    void doFilter_不正なトークン() throws Exception {
        request.addHeader("Authorization", "Bearer invalid-token");
        when(jwtProvider.parseUserId("invalid-token"))
                .thenThrow(new JwtException("invalid"));
        jwtAuthenticationFilter.doFilter(request, response, chain);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_Bearer以外() throws Exception {
        request.addHeader("Authorization", "Basic xxx");
        jwtAuthenticationFilter.doFilter(request, response, chain);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        assertNotNull(chain.getRequest());
        verify(jwtProvider, never()).parseUserId(any());
    }

    @Test
    void doFilter_トークンが空() throws Exception {
        request.addHeader("Authorization", "Bearer ");
        when(jwtProvider.parseUserId(""))
                .thenThrow(new IllegalArgumentException());
        jwtAuthenticationFilter.doFilter(request, response, chain);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        assertNotNull(chain.getRequest());
    }

}
