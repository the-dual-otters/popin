package com.snow.popin.global.error;

import com.snow.popin.global.config.SecurityConfig;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.jwt.JwtUtil;
import com.snow.popin.global.jwt.JwtFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("View 컨트롤러 - 에러")
@WebMvcTest(
        controllers = BaseErrorController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class)
        }
)
class BaseErrorControllerTest {

    private final MockMvc mvc;

    public BaseErrorControllerTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @DisplayName("[View] 잘못된 URI 요청 - 404 에러 페이지")
    @Test
    void givenWrongURI_whenRequestingPage_thenReturns404ErrorPage() throws Exception {
        // Given

        // When & Then
        mvc.perform(get("/wrong-uri"))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @DisplayName("[View] /error 엔드포인트 - HTML 에러 페이지 렌더링")
    @Test
    void givenErrorEndpoint_whenRequestingHtml_thenRendersErrorView() throws Exception {
        // When & Then
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden())                  // BaseErrorController: OK 방어 -> FORBIDDEN
                .andExpect(view().name("error"))
                .andExpect(model().attribute("statusCode", 403))
                .andExpect(model().attribute("errorCode", ErrorCode.ACCESS_DENIED))
                // 메시지는 withMessage(ReasonPhrase) 적용
                .andExpect(model().attribute("message", ErrorCode.ACCESS_DENIED.withMessage("Forbidden")))
                .andDo(print());
    }
}