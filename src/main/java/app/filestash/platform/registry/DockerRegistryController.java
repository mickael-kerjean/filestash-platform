package app.filestash.platform.registry;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class DockerRegistryController {

    private static final Logger logger = LoggerFactory.getLogger(DockerRegistryController.class);

    HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${registry.docker.token}")
    private String DOCKER_HUB_TOKEN;

    @Value("${registry.docker.image}")
    private String DOCKER_ROOT_IMAGE;


    @GetMapping("/v2/")
    public ResponseEntity<String> authenticateRegistry() {
        return ResponseEntity.status(HttpStatus.OK).body("");
    }

    @GetMapping("/v2/{name}/manifests/{tag}")
    public ResponseEntity<String> pullImageManifest(@PathVariable(value="name") final String remoteImage, @PathVariable(value="tag") final String tag) {
        // STEP1: prepare everything
        logger.info("DOCKER pull image={} tag={}", remoteImage, tag);
        HttpResponse<String> resp;
        Builder req = this.buildHttpRequest(String.format("https://registry.hub.docker.com/v2/%s/manifests/%s", DOCKER_ROOT_IMAGE, remoteImage));
        req.setHeader("Accept", "application/vnd.docker.distribution.manifest.v2+json");

        // STEP2: make the request
        try {
            resp = this.httpClient.send(req.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            logger.warn("docker::pull::manifest err[client send issue] image={} tag={} message={}", remoteImage, tag, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Oops");
        }

        // STEP3: prepare the response
        HttpHeaders h = new HttpHeaders();
        resp.headers().map().forEach((key, values) -> {
            if (key.matches("^[a-z-A-Z0-9]+$") == false) {
                return;
            }
            for (String value : values) {
                h.add(key, value);
            }
        });
        return ResponseEntity
                .status(resp.statusCode())
                .headers(h)
                .body(resp.body());
    }

    @GetMapping("/v2/{name}/blobs/{hash}")
    public ResponseEntity<StreamingResponseBody> pullImageBlob(@PathVariable(value="hash") final String hash) {
        // STEP1: prepare everything
        HttpResponse<InputStream> resp = null;
        logger.info("DOCKER pull hash={}", hash);
        Builder req = this.buildHttpRequest(String.format("https://registry.hub.docker.com/v2/%s/blobs/%s", DOCKER_ROOT_IMAGE, hash));

        // STEP2: make the request
        try {
            resp =	this.httpClient.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            logger.warn("docker::pull::blob hash[%s] err[client send error] msg[%s]", hash, e.getMessage());
            return new ResponseEntity<StreamingResponseBody>(null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        logger.info("DOCKER blob status={} hash={}", String.valueOf(resp.statusCode()), hash);

        // STEP3: prepare the response
        InputStream inputStream = resp.body();
        HttpHeaders h = new HttpHeaders();
        resp.headers().map().forEach((key, values) -> {
            if (key.matches("^[a-z-A-Z0-9]+$") == false) {
                return;
            }
            for (String value : values) {
                h.add(key, value);
            }
        });
        StreamingResponseBody responseBody = (outputStream) -> {
            try {
                IOUtils.copy(inputStream, outputStream);
            } catch(Exception e) {
                logger.warn("docker::pull::blob hash[%s] err[blob copy error] msg[%s]", hash, e.getMessage());
            }
        };
        return new ResponseEntity<StreamingResponseBody>(responseBody, h, resp.statusCode());
    }

    private Builder buildHttpRequest(String url) {
        this.DockerControllerSetup();
        Builder req = HttpRequest
                .newBuilder()
                .uri(URI.create(url));
        req.setHeader("Authorization", String.format("Bearer %s", DOCKER_HUB_TOKEN));
        return req;
    }

    private void DockerControllerSetup() {
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(String.format("https://auth.docker.io/token?service=registry.docker.io&scope=repository:%s:pull", DOCKER_ROOT_IMAGE)))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> resp;
        try {
            resp = this.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            DOCKER_HUB_TOKEN = (String) new JSONObject(resp.body()).get("token");
        } catch (IOException | InterruptedException e) {
            logger.warn("docker::token err={}", e.getMessage());
            DOCKER_HUB_TOKEN = "";
        }
    }
}
