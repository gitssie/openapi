package com.gitssie.openapi.file;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: Awesome
 * @create: 2024-03-21 16:14
 */
@Data
@ConfigurationProperties(prefix = "file.cos")
public class COSProperties {
    private String secretId;
    private String secretKey;
    private String sessionToken = "TOKEN";
    private String region;
    private String bucket;
    private String imageBucket;
    private String fileBucket;
    private String imageFolder;
    private String fileFolder;
    private String imageAccept = ".jpg,png,.jpeg,image/*";
    private String imageUrl;
    private String fileAccept;
    private String fileUrl;


    public String getImageBucket() {
        return StringUtils.defaultString(imageBucket, bucket);
    }

    public String getFileBucket() {
        return StringUtils.defaultString(fileBucket, bucket);
    }
}
