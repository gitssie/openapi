package com.gitssie.openapi.oauth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.utils.Json;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @see OAuth2Exception
 */
public class OauthExceptionHandler extends AbstractHandlerExceptionResolver {

    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex instanceof OAuth2Exception) {
            try {
                OAuth2Exception oAuth2Exception = (OAuth2Exception) ex;
                ObjectNode node = Json.newObject();
                Code code = new Code(Code.PERMISSION_DENIED.code, oAuth2Exception.getOAuth2ErrorCode(), ex.getMessage());
                node.put("code", code.code);
                node.put("message", code.getMessage());
                node.put("errCode", code.getErrCode());
                
                Map<String, String> additionalInformation = oAuth2Exception.getAdditionalInformation();
                if (additionalInformation != null) {
                    additionalInformation.forEach(node::put);
                }
                response.setStatus(oAuth2Exception.getHttpErrorCode());
                response.setCharacterEncoding("UTF-8");
                response.setContentType(MediaType.APPLICATION_JSON.toString());
                response.getWriter().write(node.toString());
                return new ModelAndView();
            } catch (Exception handlerEx) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", handlerEx);
                }
            }
        }
        return null;
    }
}
