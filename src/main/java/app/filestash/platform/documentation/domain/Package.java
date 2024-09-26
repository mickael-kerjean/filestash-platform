package app.filestash.platform.documentation.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Package {
    public String name;
    public String cmd;
}
