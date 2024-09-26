package app.filestash.platform.pages;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SkeletonControllerTest {

	@Autowired
    private MockMvc mockMvc;
	
	@Test
	void testPageHealthCheck() throws Exception {
		 mockMvc.perform(get("/healthz"))
	         .andExpect(status().isOk())
	         .andExpect(content().string("ok"));
	}
	
	@Test
	void testPageRobotTxt() throws Exception {
		 mockMvc.perform(get("/robots.txt"))
	        .andExpect(status().isOk())
	        .andExpect(content().string(containsString("Disallow")));
	}

}
