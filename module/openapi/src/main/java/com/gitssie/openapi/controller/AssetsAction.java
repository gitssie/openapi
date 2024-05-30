package com.gitssie.openapi.controller;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.file.COSAssets;
import com.gitssie.openapi.utils.Json;
import com.gitssie.openapi.utils.Libs;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.vavr.control.Either;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

@Controller
public class AssetsAction {
    private final ApplicationTemp temp = new ApplicationTemp();

    private final ObjectProvider<COSAssets> assetsProvider;

    public AssetsAction(ObjectProvider<COSAssets> assetsProvider) {
        this.assetsProvider = assetsProvider;
    }

    @PostMapping("/api/image/upload")
    public Either<Code, Map<String, String>> imageUpload(@RequestParam(value = "image", required = false) MultipartFile image,
                                                         @RequestParam(value = "file", required = false) MultipartFile file) {
        image = image == null ? file : image;
        if (image == null) {
            return Either.left(Code.INVALID_ARGUMENT.withMessage("请上传正确的文件"));
        }
        return assetsProvider.getIfAvailable().uploadImage(image);
    }

    @GetMapping("/api/assets/**")
    public ResponseEntity<?> getAssets(HttpServletRequest request) {
        String path = request.getRequestURI().substring("/api/assets/".length());

        Either<Code, ResponseEntity<Resource>> result = assetsProvider.getIfAvailable().getAssets(path);
        if (result.isLeft()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<?> httpEntity = ResponseEntity.status(400).headers(headers).body(result.getLeft());
            return httpEntity;
        } else {
            return result.get();
        }
    }

    @GetMapping("/api/assets/qrcode")
    public ResponseEntity<StreamingResponseBody> getQRCode(@RequestParam(required = false) String code) throws Exception {
        int width = 300; // 二维码宽度
        int height = 300; // 二维码高度
        String format = "png"; // 二维码图像格式
        // 创建QRCodeWriter对象
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        // 设置二维码参数
        BitMatrix bitMatrix = qrCodeWriter.encode(code, BarcodeFormat.QR_CODE, width, height);
        // 创建BufferedImage对象
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // 将二维码像素点设置为黑白颜色
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                bufferedImage.setRGB(x, y, rgb);
            }
        }
        // 将BufferedImage对象保存为图像文件
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>((stream) -> {
            ImageIO.write(bufferedImage, format, stream);
        }, headers, HttpStatus.OK);
    }

    @GetMapping("/api/static/{path}")
    public ResponseEntity<Resource> getStatic(@PathVariable String path) throws FileNotFoundException {
        File dir = temp.getDir();
        File file = new File(dir, path);
        FileSystemResource resource = new FileSystemResource(file);// 2.
        MediaType mediaType = MediaTypeFactory
                .getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        // 3
        ContentDisposition disposition = ContentDisposition
                // 3.2
                .inline() // or .attachment()
                // 3.1
                .filename(resource.getFilename())
                .build();
        headers.setContentDisposition(disposition);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
