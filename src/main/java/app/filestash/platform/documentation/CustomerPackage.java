package app.filestash.platform.documentation;

import app.filestash.platform.documentation.domain.Package;

import java.util.List;

public interface CustomerPackage {
    List<Package> listDockerImages(String email);
    List<Package> listBuilds(String email);
}
