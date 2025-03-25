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
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class DockerRegistryController {

	private static final Logger logger = LoggerFactory.getLogger(DockerRegistryController.class);

	HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
			.cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();

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
			@PathVariable(value = "name") final String remoteImage, @PathVariable(value = "tag") final String tag,
			HttpServletRequest request) {
		Builder req;
		if (DOCKER_ROOT_IMAGE.isEmpty())
			req = this.buildHttpRequest(String.format(DOCKER_REGISTRY + "/v2/%s/manifests/%s", remoteImage, tag));
		else
			req = this.buildHttpRequest(
					String.format(DOCKER_REGISTRY + "/v2/%s/manifests/%s", DOCKER_ROOT_IMAGE, remoteImage));
		req.setHeader("Accept", "application/vnd.docker.distribution.manifest.v2+json");
		return this.proxy(req, request);
	}

	@GetMapping("/v2/{name}/blobs/{hash}")
	public ResponseEntity<StreamingResponseBody> pullImageBlob(@PathVariable(value = "name") final String remoteImage,
			@PathVariable(value = "hash") final String hash, HttpServletRequest request) {
		Builder req;
		if (DOCKER_ROOT_IMAGE.isEmpty())
			req = this.buildHttpRequest(String.format(DOCKER_REGISTRY + "/v2/%s/blobs/%s", remoteImage, hash));
		else
			req = this.buildHttpRequest(String.format(DOCKER_REGISTRY + "/v2/%s/blobs/%s", DOCKER_ROOT_IMAGE, hash));
		return this.proxy(req, request);
	}

	// -------------------------------------------------------
	// helpers -----------------------------------------------
	// -------------------------------------------------------
	private ResponseEntity<StreamingResponseBody> proxy(Builder req, HttpServletRequest request) {
		// proxy incoming header
		Enumeration<String> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				if ("host".equalsIgnoreCase(headerName) || "content-length".equalsIgnoreCase(headerName)
						|| "connection".equalsIgnoreCase(headerName)) {
					continue;
				}
				Enumeration<String> headerValues = request.getHeaders(headerName);
				while (headerValues.hasMoreElements()) {
					String headerValue = headerValues.nextElement();
					req.setHeader(headerName, headerValue);
				}
			}
		}
		req.setHeader("Accept", "application/vnd.docker.distribution.manifest.v2+json");
		req.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(DOCKER_CREDENTIALS.getBytes()));

		HttpResponse<InputStream> resp = null;
		try {
			resp = this.httpClient.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
		} catch (IOException | InterruptedException e) {
			logger.warn("proxy::exception msg={}", e.getMessage());
			return new ResponseEntity<StreamingResponseBody>(null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// prepare the response
		HttpHeaders hout = new HttpHeaders();
		resp.headers().map().forEach((key, values) -> {
			if (key == null || !key.matches("^[a-z-A-Z0-9]+$")) {
				return;
			}
			for (String value : values) {
				if ("location".equalsIgnoreCase(key)) {
					// value = value.replaceFirst("(?i)^https?://[^/]+", "http://localhost:8080");
					value = value.replaceFirst("(?i)^https?://[^/]+", "https://platform.filestash.app");
					logger.info("redirect={}", value);
				}
				hout.add(key, value);
			}
		});

		// handle errors
		int status = resp.statusCode();
		if (status >= 400 && status != 404) {
			try (InputStream is = resp.body()) {
				byte[] buffer = new byte[1024];
				int bytesRead = is.read(buffer);
				String errorSnippet = (bytesRead > 0) ? new String(buffer, 0, bytesRead) : "";
				logger.warn("proxy::error  status={} snippet={}", status, errorSnippet);
				StreamingResponseBody responseBody = outputStream -> outputStream
						.write(errorSnippet.getBytes(StandardCharsets.UTF_8));
				return new ResponseEntity<StreamingResponseBody>(responseBody, hout, status);
			} catch (IOException ioe) {
				logger.warn("proxy::error::throw msg={}", ioe.getMessage());
				return new ResponseEntity<StreamingResponseBody>(null, hout, status);
			}
		}

		// happy path
		InputStream inputStream = resp.body();
		StreamingResponseBody responseBody = (outputStream) -> {
			try {
				IOUtils.copy(inputStream, outputStream);
			} catch (IOException e) {
				logger.warn("docker::proxy::blob type=[ioexception] err[blob copy error] msg=[{}]", e.getMessage());
			}
		};
		return new ResponseEntity<StreamingResponseBody>(responseBody, hout, resp.statusCode());
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
