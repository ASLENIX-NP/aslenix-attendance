package com.aslenix.attendance.config;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ImageKitConfig {

    private static final Logger log = LoggerFactory.getLogger(ImageKitConfig.class);

    @Value("${imagekit.public-key:${IMAGEKIT_PUBLIC_KEY:}}")
    private String publicKey;

    @Value("${imagekit.private-key:${IMAGEKIT_PRIVATE_KEY:}}")
    private String privateKey;

    @Value("${imagekit.url-endpoint:${IMAGEKIT_URL_ENDPOINT:}}")
    private String urlEndpoint;

    @Value("${imagekit.folder:${IMAGEKIT_FOLDER:/employee-photos}}")
    private String folder;

    private static final Map<String, String> DOTENV_MAP = loadDotenv();

    public String getPublicKey() {
        return resolveConfig(publicKey, "IMAGEKIT_PUBLIC_KEY", "IMAGE_KIT_PUBLIC_KEY");
    }

    public String getPrivateKey() {
        return resolveConfig(privateKey, "IMAGEKIT_PRIVATE_KEY", "IMAGE_KIT_PRIVATE_KEY");
    }

    public String getUrlEndpoint() {
        return resolveConfig(urlEndpoint, "IMAGEKIT_URL_ENDPOINT", "IMAGE_KIT_URL_ENDPOINT");
    }

    public String getFolder() {
        String val = resolveConfig(folder, "IMAGEKIT_FOLDER", "IMAGE_KIT_FOLDER");
        if (val.isBlank()) {
            val = "/employee-photos";
        }
        return val.startsWith("/") ? val : "/" + val;
    }

    public boolean isConfigured() {
        String priv = getPrivateKey();
        return priv != null && !priv.trim().isEmpty();
    }

    @Bean
    public ImageKitClient imageKitClient() {
        if (!isConfigured()) {
            log.info("ImageKit is not configured. Employee photos will use local disk storage.");
            return null;
        }

        try {
            log.info("Initializing ImageKit client with endpoint: {}", getUrlEndpoint());
            return ImageKitOkHttpClient.builder()
                    .privateKey(getPrivateKey())
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize ImageKit client: {}", e.getMessage(), e);
            return null;
        }
    }

    private String resolveConfig(String propValue, String... envNames) {
        if (propValue != null && !propValue.trim().isEmpty()) {
            return propValue.trim();
        }
        for (String envName : envNames) {
            String envVal = System.getenv(envName);
            if (envVal != null && !envVal.trim().isEmpty()) {
                return envVal.trim();
            }
            String sysProp = System.getProperty(envName);
            if (sysProp != null && !sysProp.trim().isEmpty()) {
                return sysProp.trim();
            }
            String dotVal = DOTENV_MAP.get(envName);
            if (dotVal != null && !dotVal.trim().isEmpty()) {
                return dotVal.trim();
            }
        }
        return "";
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> map = new HashMap<>();
        File[] candidates = new File[]{
                new File(".env"),
                new File("../.env"),
                new File("aslenix-attendance/.env")
        };
        for (File dotEnv : candidates) {
            if (dotEnv.exists() && dotEnv.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(dotEnv, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eq = line.indexOf('=');
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        map.putIfAbsent(key, val);
                    }
                } catch (Exception e) {
                    // Ignore if .env cannot be read
                }
            }
        }
        return map;
    }
}
