package app.filestash.platform.documentation;

import app.filestash.platform.documentation.domain.Commit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DocumentationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testDeploymentDocker() throws Exception {
        // given
        Document document = Jsoup.parse(mockMvc.perform(get("/user/docker/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        // then
        assertThat(document.select("h2").text()).contains("Docker Images");
    }

    @Test
    void testDeploymentPackages() throws Exception {
        // given
        Document document = Jsoup.parse(mockMvc.perform(get("/user/packages/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        // then
        assertThat(document.select("h2").text()).contains("Packages");
    }

    @Test
    void testReleaseList() throws Exception {
        // given
        Document document = Jsoup.parse(mockMvc.perform(get("/release/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        // then
        assertThat(document.select("h2").text()).contains("Releases");
        assertThat(document.select("a.card").size()).isGreaterThan(5);
    }

    @MockBean
    private ReleaseNoteService releaseNoteService;

    @Test
    void testReleaseNote() throws Exception {
        // given
        when(releaseNoteService.getCommits(Mockito.any())).thenReturn(List.of(
                Commit.builder().message("feature (test): hello world").build()
        ));

        // when
        Document document = Jsoup.parse(mockMvc.perform(get("/release/2024-08-01"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        // then
        verify(releaseNoteService).getCommits(LocalDate.of(2024, 8, 1));
        assertThat(document.select("h2").text()).contains("Changelist");
        assertThat(document.select(".card").size()).isEqualTo(2);
        assertThat(document.select(".card").get(0).text()).contains("test - hello world");
    }
}