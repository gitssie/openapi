package com.gitssie.openapi.oauth;

import com.gitssie.openapi.data.Code;
import io.vavr.control.Option;

import javax.servlet.http.HttpServletRequest;

public interface CaptchaStore {

    <T> Option<Code> storeCaptcha(HttpServletRequest request, T captcha);

    Option<Code> verifyCaptcha(HttpServletRequest request);
}
