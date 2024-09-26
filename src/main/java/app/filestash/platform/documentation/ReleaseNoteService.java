package app.filestash.platform.documentation;

import app.filestash.platform.documentation.domain.Commit;

import java.time.LocalDate;
import java.util.List;

public interface ReleaseNoteService {
	List<Commit> getCommits(LocalDate origin);
}
