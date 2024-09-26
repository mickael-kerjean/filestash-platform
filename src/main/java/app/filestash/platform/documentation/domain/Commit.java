package app.filestash.platform.documentation.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
public class Commit {
    public String hash;
    public String message;
    public LocalDate date;
}
