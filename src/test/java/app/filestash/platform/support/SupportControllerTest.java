package app.filestash.platform.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SupportControllerTest {

	@Autowired
    private MockMvc mockMvc;

	@Test
	void testSupportPageTicketCreation() throws Exception {
		// given
		Document document = Jsoup.parse(mockMvc.perform(get("/support/ticket/new"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());

		// then
		assertThat(document.select("h2").text()).contains("Create a Web Ticket");
		assertThat(document.select("input[name=\"username\"]")).isNotEmpty();
		assertThat(document.select("textarea")).isNotEmpty();
		assertThat(document.select("button[type=\"submit\"]").text()).contains("SEND");
	}

	@Test
	void testSupportPageTicketCreationAfterRedirected() throws Exception {
		// given
		Document document = Jsoup.parse(mockMvc.perform(get("/support/ticket/new").param("ok", ""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());

		// then
		assertThat(document.text()).contains("DONE");
	}

	@Test
	void testSupportPageBooking() throws Exception {
		// given
		Document document = Jsoup.parse(mockMvc.perform(get("/support/book"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());

		// then
		assertThat(document.select("h2").text()).contains("Book a timeslot");
		assertThat(document.select("iframe")).isNotEmpty();
	}

	@Test
	void testSupportPageNonHappyPath() throws Exception {
		 mockMvc.perform(get("/support/")).andExpect(status().isNotFound());
		 mockMvc.perform(get("/support/dummy")).andExpect(status().isNotFound());
	}

}
