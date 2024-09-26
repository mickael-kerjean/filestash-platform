package app.filestash.platform.documentation;

import app.filestash.platform.documentation.domain.Commit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReleaseNoteServiceImpl implements ReleaseNoteService {

    @Override
    public List<Commit> getCommits(LocalDate origin) {
        String lastHash = "";
        List<Commit> commits = new ArrayList<Commit>();
        for (int page=0; page<75; page++) {
            List<Commit> commitCandidates = fetch(lastHash);
            Commit currCommit = null;
            for (int i=0; i<commitCandidates.size(); i++) {
                currCommit = commitCandidates.get(i);
                LocalDate d = currCommit.getDate();
                if (d.getYear() == origin.getYear() && d.getMonth() == origin.getMonth()) {
                    commits.add(currCommit);
                }
            }
            if (currCommit == null) return commits;
            if (origin.isAfter(currCommit.getDate())) {
                return commits;
            }
            lastHash = currCommit.getHash();
        }
        return commits;
    }

    private List<Commit> fetch(String first) {
        String baseUrl = "https://github.com/mickael-kerjean/filestash/commits/master/";
        String url = first.isEmpty() ? baseUrl : baseUrl + "?after=" + first + "+0";

        try {
            Document doc = Jsoup.connect(url).get();
            return doc.select("[data-testid=\"commit-row-item\"]")
                    .stream()
                    .map(element -> {
                        return Commit.builder()
                                .message(element.getElementsByAttribute("title").attr("title").split("\\R")[0])
                                .hash(element.getElementsByAttribute("href").attr("href").replaceAll(".*/commit/([a-f0-9]{40})$", "$1"))
                                .date(LocalDate.parse(
                                        element.select("relative-time").text(),
                                        DateTimeFormatter.ofPattern("MMM d, yyyy")
                                ))
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            // Log the error (use a logger or print stack trace for debugging)
            System.err.println("Error fetching commits: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
