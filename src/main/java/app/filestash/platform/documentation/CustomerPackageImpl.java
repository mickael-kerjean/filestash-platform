package app.filestash.platform.documentation;

import app.filestash.platform.documentation.domain.Package;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.param.CustomerSearchParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CustomerPackageImpl implements CustomerPackage {

    private static final Logger logger = LoggerFactory.getLogger(CustomerPackageImpl.class);

    @Value("${github.repo.enterprise}")
    private String GITHUB_TOKEN;

    @Value("${stripe.token}")
    private String STRIPE_API_TOKEN;

    private final GithubRepo githubService;
    private final StripeMetadataService stripeService;

    public CustomerPackageImpl() {
        githubService = new GithubRepo();
        stripeService = new StripeMetadataService();
    }

    @Override
    public List<Package> listBuilds(String email) {
        ArrayList<Package> ps = new ArrayList<Package>();
        for(String customer : stripeService.getMetadata(email, STRIPE_API_TOKEN)) {
            String response = githubService.extractFile("/mickael-kerjean/filestash-enterprise/master/customers/" + customer + "/Jenkinsfile", GITHUB_TOKEN);
            Pattern.compile("IMAGE\\s=\\s'?([^']*)'?").matcher(response).results().map(mr -> mr.group(1)).forEach(imageName -> {
                String cmd = "";
                switch (imageName) {
                    case "deb":
                        cmd = "curl -O https://app-filestash-www.s3.amazonaws.com/pkg/"+customer+".deb\n";
                        cmd += "dpkg -i " + customer +".deb\n";
                        cmd += "systemctl start filestash";
                        break;
                    case "rpm":
                        cmd = "curl -O https://app-filestash-www.s3.amazonaws.com/pkg/"+customer+".rpm";
                        break;
                    case "raw":
                        cmd = "curl -O https://app-filestash-www.s3.amazonaws.com/pkg/"+customer+".tar.gz";
                        break;
                    default:
                        logger.warn("list builds customer={} image={}", customer, imageName);
                        throw new IllegalStateException("cannot find packages");
                }
                ps.add(Package.builder().cmd(cmd).name(customer + "::" + imageName).build());
            });
        }
        return ps;
    }

    @Override
    public List<Package> listDockerImages(String email) {
        ArrayList<Package> ps = new ArrayList<Package>();
        String response;
        for(String customer : stripeService.getMetadata(email, STRIPE_API_TOKEN)) {
            // STEP1: extract port where customer will be running the application
            ArrayList<Integer> ports = new ArrayList<Integer>();
            response = githubService.extractFile("/mickael-kerjean/filestash-enterprise/master/customers/" + customer+ "/plugin.go", GITHUB_TOKEN);
            Pattern.compile("(plg_starter_[^\"]*)").matcher(response).results().map(mr -> mr.group(1)).forEach(pluginName -> {
                switch (pluginName) {
                    case "plg_starter_http" -> ports.add(8334);
                    case "plg_starter_https" -> ports.add(8334);
                    case "plg_starter_web" -> {
                        ports.add(80);
                        ports.add(443);
                    }
                    case "plg_starter_httpsfs" -> ports.add(443);
                }
            });

            // STEP2: create the actual deployment instructions
            response = githubService.extractFile("/mickael-kerjean/filestash-enterprise/master/customers/" + customer + "/Jenkinsfile", GITHUB_TOKEN);
            Pattern.compile("IMAGE\\s=\\s'?([^']*)'?").matcher(response).results().map(mr -> mr.group(1)).forEach(imageName -> {
                if (List.of("raw", "rpm", "deb").contains(imageName)) {
                    return;
                }
                String port = "";
                for (int p : ports) {
                    port += String.format("-p %d:%d ", p, p);
                }
                if(imageName.startsWith("machines/stash:")) {
                    imageName = imageName.replaceFirst("machines/stash:", "platform.filestash.app/");
                }
                ps.add(Package.builder().name(customer).cmd("docker run --name filestash " + port + "--rm " + imageName).build());
            });
        }
        return ps;
    }

}

class GithubRepo {
    public String extractFile(String path, String token) {
        return this.httpGet("https://raw.githubusercontent.com" + path, token);
    }

    private String httpGet(String url, String token) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url)).GET()
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return "";
        }
        return response.body();
    }
}

class StripeMetadataService {
    public List<String> getMetadata(String email, String token) {
        Stripe.apiKey = token;
        ArrayList<String> metas = new ArrayList<String>();
        ArrayList<String> cids = null;
        try {
            cids = this.getCustomerId(email);
        } catch (StripeException e) {
            return metas;
        }
        for(String cid : cids) {
            Customer customer = null;
            try {
                customer = Customer.retrieve(cid);
            } catch (StripeException e) {
                return metas;
            }
            String val = customer.getMetadata().get(cid);
            if (val != null) metas.add(val);
        }
        return metas;
    }

    protected ArrayList<String> getCustomerId(String email) throws StripeException {
        ArrayList<String> ids = new ArrayList<String>();
        CustomerSearchParams params = CustomerSearchParams
                .builder()
                .setQuery(String.format("email:'%s'", email))
                .build();
        CustomerSearchResult result = Customer.search(params);
        for (int i=0; i<result.getData().size(); i++) {
            ids.add(result.getData().get(i).getId());
        }
        return ids;
    }

}