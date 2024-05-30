package com.gitssie.openapi.auth;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.oauth.CaptchaStore;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.Date;


/**
 * 生成图片验证码
 */
@Component
public class ImageCaptchaStore implements CaptchaStore {
    private final String CAPTCHA_SESSION_KEY = "CAPTCHA_SESSION_KEY";
    private final String CAPTCHA_EXPIRE_TIME = "CAPTCHA_EXPIRE_TIME";
    private FastDateFormat df = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    @Override
    public <T> Option<Code> storeCaptcha(HttpServletRequest request, T captcha) {
        HttpSession session = request.getSession(true);
        session.setAttribute(CAPTCHA_SESSION_KEY, captcha);
        session.setAttribute(CAPTCHA_EXPIRE_TIME, df.format(DateUtils.addMinutes(new Date(), 10)));
        return Option.none();
    }

    @Override
    public Option<Code> verifyCaptcha(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Option<Code> errCode = Option.of(Code.INVALID_ARGUMENT.withMessage("验证码不匹配"));
        Option<Code> expireCode = errCode.map(e -> e.withMessage("验证码过期")); //验证码过期
        String code = request.getParameter("code");
        if (session == null || StringUtils.isBlank(code)) {
            return errCode;
        }
        String verifyCode = (String) session.getAttribute(CAPTCHA_SESSION_KEY);
        String expireTime = (String) session.getAttribute(CAPTCHA_EXPIRE_TIME);
        if (StringUtils.isBlank(verifyCode) || StringUtils.isBlank(expireTime)) {
            return expireCode;
        }
        try {
            Date expire = df.parse(expireTime);
            if (new Date().after(expire)) {
                return expireCode;
            }
            if (!code.equalsIgnoreCase(verifyCode)) {
                return errCode;
            }
            session.removeAttribute(CAPTCHA_EXPIRE_TIME);
            return Option.none();
        } catch (ParseException e) {
            return errCode;
        }
    }
}
