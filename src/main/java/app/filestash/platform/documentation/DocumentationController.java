package app.filestash.platform.documentation;

import app.filestash.platform.documentation.domain.Commit;
import app.filestash.platform.documentation.domain.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class DocumentationController {

    @Autowired
    ReleaseNoteService releaseNoteService;

    @Autowired
    CustomerPackage customerPackage;

    @GetMapping("/user/docker/")
    public String listImages(Model model, Principal principal) throws Exception {
        ArrayList<Package> images = new ArrayList<Package>();
        images.add(Package.builder().name("AGPL").cmd("docker run -p 8334:8334 --name filestash --rm machines/filestash").build());

        boolean hasCustomBuild = false;
        if (principal != null) {
            images.add(Package.builder().name("enterprise").cmd("docker run -p 8334:8334 --name filestash --rm platform.filestash.app/enterprise").build());
            images.addAll(customerPackage.listDockerImages(principal.getName()));
        }
        if (hasCustomBuild == false) model.addAttribute(
                "message",
                "Want a custom build? <a href=\"https://www.filestash.app/pricing/?modal=enterprise\">Chat with us</a>"
        );
        model.addAttribute("items", images);
        model.addAttribute("title", "Docker Images");
        return "documentation_package_list";
    }

    @GetMapping("/user/packages/")
    public String listPackages(Model model, Principal principal) throws Exception {
        ArrayList<Package> packages = new ArrayList<Package>();
        if (principal != null) {
            packages.addAll(customerPackage.listBuilds(principal.getName()));
        }
        model.addAttribute("items", packages);
        model.addAttribute("title", "Packages");
        if (packages.size() == 0) model.addAttribute(
                "message",
                "To get your own deployment package, <a href=\"https://www.filestash.app/pricing/?modal=enterprise\">contact us</a>"
        );
        return "documentation_package_list";
    }

    @GetMapping("/release/")
    public String listReleases(Model model) {
        model.addAttribute("title", "Releases");
        ArrayList<LocalDate> releases = new ArrayList<LocalDate>();
        int thisYear = Year.now().getValue();
        for (int year = thisYear; year >= 2017; year--) {
            int startMonth = 0;
            int endMonth = 12;
            if (year == 2017) {
                startMonth = 5;
            } else if (year == thisYear) {
                endMonth = Math.max(0, LocalDate.now().getMonthValue() - 1);
            }
            for (int month = endMonth; month > startMonth; month--) {
                releases.add(LocalDate.of(year, month, 1));
            }
        }
        model.addAttribute("items", releases);
        return "documentation_release_list";
    }

    @GetMapping("/release/{date}")
    public String getRelease(Model model, @PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) throws Exception {
        class ChangeNote {
            public final String title;
            public final String type;
            public final String hash;

            public ChangeNote(String type, String title, String hash) {
                this.title = title;
                this.type = type;
                this.hash = hash;
            }
        }
        model.addAttribute("title", "Changelist");

        ArrayList<ChangeNote> changes = new ArrayList<ChangeNote>();
        if (date.getYear() >= 2020 && date.isBefore(LocalDate.now())) {
            changes.add(new ChangeNote("chore", "maintain the build", ""));
        }
        List<Commit> commits = releaseNoteService.getCommits(date);
        for (int i=0; i<commits.size(); i++) {
            Commit commit = commits.get(i);
            Pattern pattern = Pattern.compile("^(?<type>[^\\s]+)\\s\\((?<target>[^\\)]+)\\):\\s(?<message>.+)$");
            Matcher matcher = pattern.matcher(commit.getMessage());
            if (matcher.find()) {
                String target =  matcher.group("target");
                String message = matcher.group("message");
                if (target.equalsIgnoreCase(message)) {
                    changes.add(new ChangeNote(matcher.group("type"), target, commit.getHash()));
                } else if (target.startsWith("[") && target.endsWith("]")) {
                    changes.add(new ChangeNote(matcher.group("type"), message, commit.getHash()));
                } else if (message.startsWith(target)) {
                    changes.add(new ChangeNote(matcher.group("type"), message, commit.getHash()));
                } else {
                    changes.add(new ChangeNote(matcher.group("type"), target + " - " + message, commit.getHash()));
                }
            } else {
                changes.add(new ChangeNote("chore", commit.getMessage(), commit.getHash()));
            }
        }
        changes.sort(new Comparator<ChangeNote>() {
            @Override
            public int compare(ChangeNote o1, ChangeNote o2) {
                return Integer.compare(getTypePriority(o1.type), getTypePriority(o2.type));
            }

            private int getTypePriority(String type) {
                return switch (type) {
                    case "feature" -> 1;
                    case "fix" -> 2;
                    case "chore" -> 3;
                    default -> 4;
                };
            }
        });
        model.addAttribute("items", changes);
        model.addAttribute("hasFeatures", changes.stream().anyMatch(item -> "feature".equals(item.type)));
        return "documentation_release";
    }
}
