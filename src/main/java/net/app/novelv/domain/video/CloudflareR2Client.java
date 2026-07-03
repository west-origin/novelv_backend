package net.app.novelv.domain.video;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Component
public class CloudflareR2Client {

    private final CloudflareR2Properties properties;

    public CloudflareR2Client(CloudflareR2Properties properties) {
        this.properties = properties;
    }

    public R2DirectUpload createDirectUpload(DirectUploadRequest request, Long uploaderId) {
        validateConfig();
        String objectKey = createObjectKey(request.fileName(), uploaderId);
        Instant expiresAt = Instant.now().plusSeconds(properties.resolvedUploadUrlTtlSeconds());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(objectKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(properties.resolvedUploadUrlTtlSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();

        try (S3Presigner presigner = createPresigner()) {
            return new R2DirectUpload(
                    objectKey,
                    presigner.presignPutObject(presignRequest).url().toString(),
                    expiresAt.toString()
            );
        } catch (SdkException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudflare R2 upload URL creation failed: " + exception.getMessage(),
                    exception
            );
        }
    }

    public String createPlaybackUrl(String objectKey, String contentType) {
        validateConfig();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(objectKey)
                .responseContentType(resolveContentType(contentType))
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(properties.resolvedPlaybackUrlTtlSeconds()))
                .getObjectRequest(getObjectRequest)
                .build();

        try (S3Presigner presigner = createPresigner()) {
            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudflare R2 playback URL creation failed: " + exception.getMessage(),
                    exception
            );
        }
    }

    public void deleteObject(String objectKey) {
        validateConfig();
        if (!StringUtils.hasText(objectKey)) {
            return;
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(objectKey)
                .build();

        try (S3Client s3Client = createS3Client()) {
            s3Client.deleteObject(deleteObjectRequest);
        } catch (SdkException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudflare R2 object deletion failed: " + exception.getMessage(),
                    exception
            );
        }
    }

    private S3Presigner createPresigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.resolvedEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())
                ))
                .build();
    }

    private S3Client createS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.resolvedEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())
                ))
                .build();
    }

    private String createObjectKey(String fileName, Long uploaderId) {
        String safeName = Normalizer.normalize(fileName, Normalizer.Form.NFKD)
                .replaceAll("[^a-zA-Z0-9._-]", "-")
                .replaceAll("-+", "-")
                .toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(safeName)) {
            safeName = "video";
        }
        return "videos/" + uploaderId + "/" + UUID.randomUUID() + "/" + safeName;
    }

    private String resolveContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.accountId())) {
            throw new IllegalStateException("app.cloudflare.r2.account-id is required.");
        }
        if (!StringUtils.hasText(properties.accessKeyId())) {
            throw new IllegalStateException("app.cloudflare.r2.access-key-id is required.");
        }
        if (!StringUtils.hasText(properties.secretAccessKey())) {
            throw new IllegalStateException("app.cloudflare.r2.secret-access-key is required.");
        }
        if (!StringUtils.hasText(properties.bucketName())) {
            throw new IllegalStateException("app.cloudflare.r2.bucket-name is required.");
        }
    }
}