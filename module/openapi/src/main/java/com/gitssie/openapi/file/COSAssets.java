package com.gitssie.openapi.file;

import com.gitssie.openapi.data.Code;
import com.google.common.collect.Maps;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import io.vavr.control.Either;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * @author: Awesome
 * @create: 2024-03-21 16:05
 */
public class COSAssets {
    private static final Logger LOGGER = LoggerFactory.getLogger(COSAssets.class);
    private final COSClient cosClient;
    private final COSProperties properties;

    public COSAssets(COSProperties properties) {
        BasicSessionCredentials cred = new BasicSessionCredentials(properties.getSecretId(), properties.getSecretKey(), properties.getSessionToken());
        ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
        this.properties = properties;
        this.cosClient = new COSClient(cred, clientConfig);
    }

    public String getImageURL(String path) {
        String url = properties.getImageUrl();
        return url + "/" + path;
    }

    public Either<Code, Map<String, String>> uploadImage(MultipartFile file) {
        MediaType imageType = MediaType.parseMediaType(file.getContentType());
        String fileName = UUID.randomUUID().toString();
        String path = properties.getImageFolder() + "/" + fileName + "." + StringUtils.defaultString(imageType.getSubtype(), "png");
        String bucket = properties.getImageBucket();
        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            meta.setContentType(file.getContentType());
            //meta.setExpirationTime(DateUtils.addSeconds(new Date(), 10));
            PutObjectRequest request = new PutObjectRequest(bucket, path, file.getInputStream(), meta);
            PutObjectResult result = cosClient.putObject(request);
            Map<String, String> map = Maps.newLinkedHashMap();
            map.put("requestId", result.getRequestId());
            map.put("md5", result.getContentMd5());
            map.put("eTag", result.getETag());
            map.put("versionId", result.getVersionId());
            map.put("path", path);
            map.put("url", getImageURL(path));
            return Either.right(map);
        } catch (Exception e) {
            LOGGER.error("upload file:{} bucket:{},region:{},error", path, bucket, properties.getRegion(), e);
            return Either.left(Code.INTERNAL);
        }
    }

    public Either<Code, ResponseEntity<Resource>> getAssets(String path) {
        boolean image = StringUtils.isNotEmpty(properties.getImageFolder()) && path.startsWith(properties.getImageFolder());
        boolean file = StringUtils.isNotEmpty(properties.getFileFolder()) && path.startsWith(properties.getFileFolder());
        if (!(image || file)) {
            return Either.left(Code.NOT_FOUND);
        }
        String bucket = image ? properties.getImageBucket() : properties.getFileBucket();
        COSObject result;
        try {
            GetObjectRequest request = new GetObjectRequest(bucket, path);
            result = cosClient.getObject(request);
        } catch (Exception e) {
            LOGGER.error("get file:{} bucket:{},region:{},error", path, bucket, properties.getRegion(), e);
            return Either.left(Code.INTERNAL);
        }
        ObjectMetadata meta = result.getObjectMetadata();
        HttpHeaders headers = new HttpHeaders();
        if (meta != null) {
            headers.setContentType(MediaType.valueOf(meta.getContentType()));
            headers.setContentLength(meta.getContentLength());

            if (StringUtils.isNotEmpty(meta.getETag())) {
                headers.setETag("W/" + meta.getETag() + "\"");
            }
            if (meta.getLastModified() != null) {
                headers.setLastModified(meta.getLastModified().getTime());
            }
            Map<String, String> userMetadata = meta.getUserMetadata();
            if (ObjectUtils.isNotEmpty(userMetadata)) {
                String attachment = userMetadata.get("attachment");
                String inline = userMetadata.get("inline");
                if (StringUtils.isNotEmpty(attachment)) {
                    headers.setContentDisposition(ContentDisposition.attachment().filename(attachment).build());
                } else if (StringUtils.isNotEmpty(inline)) {
                    headers.setContentDisposition(ContentDisposition.inline().filename(inline).build());
                }
            }
            headers.setCacheControl(meta.getCacheControl());
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        ResponseEntity<Resource> entity = ResponseEntity.ok().headers(headers).body(new InputStreamResource(result.getObjectContent()));
        return Either.right(entity);
    }
}
