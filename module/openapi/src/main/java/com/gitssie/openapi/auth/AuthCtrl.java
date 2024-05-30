package com.gitssie.openapi.auth;


import com.gitssie.openapi.oauth.AuthenticationResultHandler;
import com.gitssie.openapi.oauth.CaptchaStore;
import com.gitssie.openapi.web.EitherReturnValueHandler;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
public class AuthCtrl {
    private AuthenticationResultHandler authenticationResultHandler;
    private CaptchaStore captchaStore;

    public AuthCtrl(EitherReturnValueHandler returnValueHandler, CaptchaStore captchaStore) {
        this.authenticationResultHandler = new AuthenticationResultHandler(returnValueHandler);
        this.captchaStore = captchaStore;
    }

    @RequestMapping(value = "/api/auth/generateCode", method = {RequestMethod.POST, RequestMethod.GET})
    public void generate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Expires", "0");
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        //        response().setHeader("Cache-Control", "post-check=0, pre-recon=0");
        response.setHeader("Pragma", "no-cache");
        String captchaCode = VerifyCodeUtils.generateVerifyCode(4);
        captchaStore.storeCaptcha(request, captchaCode);
        int w = 124;
        int h = 50;
        VerifyCodeUtils.outputImage(w, h, response.getOutputStream(), captchaCode);
    }

    @GetMapping("/api/auth/state")
    public Map<String, Object> loginState(Authentication authentication) {
        return authenticationResultHandler.toResult(authentication);
    }
}

