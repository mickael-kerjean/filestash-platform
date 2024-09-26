package app.filestash.platform.session;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SessionControllerTest {

	@Autowired
    private MockMvc mockMvc;

	@Test
	void testLoginPageMain() throws Exception {
		// then, when
		mockMvc.perform(get("/login"))
	         .andExpect(status().isOk())
	         .andExpect(content().string(containsString("<form method=\"POST\" action=\"/login/sendcode\"")))
	         .andExpect(content().string(containsString("<input name=\"username\" type=\"text\" placeholder=\"Your email\" required autofocus autocomplete=\"username\" />")))
	         .andExpect(content().string(containsString("<button type=\"submit\">CREATE CODE</button>")));
	}

	@Test
	void testLoginPageForThirdPartyApps() throws Exception {
		// given
        MockHttpSession session = new MockHttpSession();

        // when
        mockMvc.perform(get("/login/anonymous?origin=aws").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // then
        SecurityContext contextFromHolder = SecurityContextHolder.getContext();
        assertThat(contextFromHolder.getAuthentication()).isNotNull();
        assertThat(contextFromHolder.getAuthentication().getName()).isEqualTo("aws");
        assertThat(contextFromHolder.getAuthentication().getAuthorities()).extracting("authority").contains("ROLE_PARTNER");
	}

	@Test
	void testLoginPageForThirdPartyAppsErrorPath() throws Exception {
		// given
        MockHttpSession session = new MockHttpSession();

        // when
        mockMvc.perform(get("/login/anonymous?origin=unknown").session(session))
                .andExpect(status().is4xxClientError());
	}

    @Test
    void testLogoutPage() throws Exception {
    	// given
        MockHttpSession session = new MockHttpSession();
        assertThat(session.isInvalid()).isFalse();

        // when
        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // then
        assertThat(session.isInvalid()).isTrue();
    }
}
