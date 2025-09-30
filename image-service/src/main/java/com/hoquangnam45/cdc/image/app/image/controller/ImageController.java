package com.hoquangnam45.cdc.image.app.image.controller;

import com.hoquangnam45.cdc.image.app.common.model.JwtUser;
import com.hoquangnam45.cdc.image.app.common.model.ServiceResponse;
import com.hoquangnam45.cdc.image.app.common.model.ProcessJobConfigurationMdl;
import com.hoquangnam45.cdc.image.app.image.model.ThumbnailImageResponse;
import com.hoquangnam45.cdc.image.app.image.model.UploadImageResponse;
import com.hoquangnam45.cdc.image.app.image.model.UserUploadedImageResponse;
import com.hoquangnam45.cdc.image.app.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/image")
public class ImageController {
    private final ImageService imageService;

    @PostMapping("/upload")
    public Mono<ResponseEntity<ServiceResponse<List<UploadImageResponse>>>> uploadImage(@AuthenticationPrincipal JwtUser jwtUser, @RequestBody List<String> fileNames) {
        return imageService.createUploadPresignedUrls(jwtUser.id(), fileNames)
                .map(ServiceResponse::success)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/list")
    public Mono<ResponseEntity<ServiceResponse<List<UserUploadedImageResponse>>>> listImages(@AuthenticationPrincipal JwtUser jwtUser) {
        return imageService.listUploadedUserImages(jwtUser.id())
                .map(ServiceResponse::success)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/thumbnail/configuration/list")
    public Mono<RequestEntity<ServiceResponse<List<ProcessJobConfigurationMdl>>>> getThumbnailConfigurations() {
        return Mono.empty();
    }

    @GetMapping("/{id}/thumbnail")
    public Mono<RequestEntity<ServiceResponse<ThumbnailImageResponse>>> getImageThumbnail(@RequestParam("configurationId") String configurationId) {
        return Mono.empty();
    }
}
