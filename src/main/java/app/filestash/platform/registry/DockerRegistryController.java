package app.filestash.platform.registry;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class DockerRegistryController {

	private static final Logger logger = LoggerFactory.getLogger(DockerRegistryController.class);

	HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

	@Value("${registry.docker.url}")
	private String DOCKER_REGISTRY;

	@Value("${registry.docker.image}")
	private String DOCKER_ROOT_IMAGE;

	@Value("${registry.docker.token}")
	private String DOCKER_HUB_TOKEN;

	@Value("${registry.docker.credentials}")
	private String DOCKER_CREDENTIALS;

	@GetMapping("/{repository:.+}:{tag:.+}")
	public String ImagePage(@PathVariable String repository, @PathVariable String tag, Model model) {
		String img = "platform.filestash.app/" + repository + ":" + tag;
		model.addAttribute("title", "Docker Registry");
		model.addAttribute("name", img);
		model.addAttribute("cmd", "docker pull " + img);
		return "registry_public";
	}

	@GetMapping("/v2/")
	public ResponseEntity<String> authenticateRegistry() {
		return ResponseEntity.status(HttpStatus.OK).body("");
	}

	@GetMapping("/v2/{name}/manifests/{tag}")
	public ResponseEntity<StreamingResponseBody> pullImageManifest(
			@PathVariable(value = "name") final String remoteImage, @PathVariable(value = "tag") final String tag) {
		logger.info("DOCKER pull image={} tag={}", remoteImage, tag);
		Builder req;
		if (DOCKER_ROOT_IMAGE.isEmpty())
			req = this.buildHttpRequest(String.format(DOCKER_REGISTRY + "/v2/%s/manifests/%s", remoteImage, tag));
		else
			req = this.buildHttpRequest(
					String.format(DOCKER_REGISTRY + "/v2/%s/manifests/%s", DOCKER_ROOT_IMAGE, remoteImage));
		req.setHeader("Accept", "application/vnd.docker.distribution.manifest.v2+json");
		return this.proxy(req);
	}

	@GetMapping("/v2/{name}/blobs/{hash}")
	public ResponseEntity<StreamingResponseBody> pullImageBlob(@PathVariable(value = "name") final String remoteImage,
			@PathVariable(value = "hash") final String hash) {
		logger.info("DOCKER pull hash={}", hash);
		Builder req;
		if (DOCKER_ROOT_IMAGE.isEmpty())
			req = this.buildHttpRequest(String.format(DOCKER_REGISTRY + "/v2/%s/blobs/%s", remoteImage, hash));
		else
			req = this.buildHttpRequest(String.format(DOCKER_REGISTRY + "/v2/%s/blobs/%s", DOCKER_ROOT_IMAGE, hash));
		return this.proxy(req);
	}

	// -------------------------------------------------------
	// docker push -------------------------------------------
	// -------------------------------------------------------
	@RequestMapping(value = "/v2/{name}/blobs/{digest}", method = RequestMethod.HEAD)
	public ResponseEntity<StreamingResponseBody> headImage(@PathVariable("name") final String remoteImage,
			@PathVariable("digest") final String digest, HttpServletRequest request) {
		if (!this.isAuthenticated(request))
			return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED);
		logger.info("DOCKER blob::head blob image={} digest={}", remoteImage, digest);
		if (!DOCKER_ROOT_IMAGE.isEmpty())
			return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
		Builder req = this.buildHttpRequest(String.format(DOCKER_REGISTRY + "/v2/%s/blobs/%s", remoteImage, digest));
		req.method(RequestMethod.HEAD.toString(), HttpRequest.BodyPublishers.noBody());
		return this.proxy(req);
	}

	@RequestMapping(value = "/v2/{name}/manifests/{tag}", method = RequestMethod.PUT)
	public ResponseEntity<StreamingResponseBody> pushImageManifest(@PathVariable("name") final String remoteImage,
			@PathVariable("tag") final String tag, @RequestBody(required = false) String manifest,
			HttpServletRequest request) {
		if (!this.isAuthenticated(request))
			return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED);
		logger.info("DOCKER push::manifest image={} tag={}", remoteImage, tag);
		if (!DOCKER_ROOT_IMAGE.isEmpty())
			return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
		Builder req = this.buildHttpRequest(String.format(DOCKER_REGISTRY + "/v2/%s/manifests/%s", remoteImage, tag));
		req.setHeader("Content-Type", "application/vnd.docker.distribution.manifest.v2+json");
		req.method(RequestMethod.PUT.toString(), HttpRequest.BodyPublishers.ofString(manifest));
		return this.proxy(req);
	}

	@RequestMapping(value = "/v2/{name}/blobs/uploads/", method = RequestMethod.POST)
	public ResponseEntity<StreamingResponseBody> pushImageBlobPost(
			@PathVariable(value = "name") final String remoteImage, HttpServletRequest request) {
		if (!this.isAuthenticated(request))
			return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED);
		logger.info("DOCKER push::blob image={}", remoteImage);
		if (!DOCKER_ROOT_IMAGE.isEmpty())
			return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
		Builder req = this.buildHttpRequest(String.format(DOCKER_REGISTRY + "/v2/%s/blobs/uploads/", remoteImage));
		req.method(RequestMethod.POST.toString(), HttpRequest.BodyPublishers.noBody());
		return this.proxy(req);
	}

	// -------------------------------------------------------
	// helpers -----------------------------------------------
	// -------------------------------------------------------
	private boolean isAuthenticated(HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Basic ")) {
			return false;
		}
		String provided = authHeader.substring("Basic ".length()).trim();
		String expected = Base64.getEncoder().encodeToString(DOCKER_CREDENTIALS.getBytes(StandardCharsets.UTF_8));
		logger.info("docker provided={} expected={} res={} config={}", provided, expected, provided.equals(expected),
				DOCKER_CREDENTIALS);
		return provided.equals(expected);
	}

	private ResponseEntity<StreamingResponseBody> proxy(Builder req) {
		HttpResponse<InputStream> resp = null;
		try {
			resp = this.httpClient.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
		} catch (IOException | InterruptedException e) {
			return new ResponseEntity<StreamingResponseBody>(null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// STEP3: prepare the response
		InputStream inputStream = resp.body();
		HttpHeaders h = new HttpHeaders();
		resp.headers().map().forEach((key, values) -> {
			if (key == null || !key.matches("^[a-z-A-Z0-9]+$")) {
				return;
			}
			for (String value : values) {
				h.add(key, value);
			}
		});
		StreamingResponseBody responseBody = (outputStream) -> {
			try {
				IOUtils.copy(inputStream, outputStream);
			} catch (Exception e) {
				logger.warn("docker::pull::blob err[blob copy error] msg[%s]", e.getMessage());
			}
		};
		return new ResponseEntity<StreamingResponseBody>(responseBody, h, resp.statusCode());
	}

	private Builder buildHttpRequest(String url) {
		this.DockerControllerSetup();
		Builder req = HttpRequest.newBuilder().uri(URI.create(url));
		if (!DOCKER_HUB_TOKEN.isEmpty())
			req.setHeader("Authorization", String.format("Bearer %s", DOCKER_HUB_TOKEN));
		return req;
	}

	private void DockerControllerSetup() {
		if (DOCKER_HUB_TOKEN.isEmpty())
			return;
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(String.format(
						"https://auth.docker.io/token?service=registry.docker.io&scope=repository:%s:pull",
						DOCKER_ROOT_IMAGE)))
				.header("Content-Type", "application/json").build();

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
