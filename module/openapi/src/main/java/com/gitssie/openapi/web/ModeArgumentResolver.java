package com.gitssie.openapi.web;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.service.PageService;
import com.gitssie.openapi.web.annotation.Action;
import com.gitssie.openapi.web.annotation.ActionMethod;
import io.vavr.control.Either;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * @author: Awesome
 * @create: 2024-05-20 17:15
 */
public class ModeArgumentResolver {
    public final static String MODEL_ARGUMENT_RESOLVER = ModeArgumentResolver.class.getName() + ".model";
    private PageService pageService;

    public ModeArgumentResolver(PageService pageService) {
        this.pageService = pageService;
    }

    protected String getApiKey(NativeWebRequest webRequest) {
        Map<String, String> uriTemplateVars = (Map<String, String>) webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        String apiKey = uriTemplateVars.get(ActionMethod.API_KAY);
        return apiKey;
    }

    protected Either<Code, Model> getQuery(MethodParameter parameter, NativeWebRequest webRequest) {
        Action action = parameter.getMethodAnnotation(Action.class);
        if (action == null) {
            return Either.left(Code.NOT_FOUND);
        }
        String apiKey = getApiKey(webRequest);
        String[] funcNames = webRequest.getParameterValues(ActionMethod.FUNC_NAME);
        String funcName = null;
        if (funcNames != null && funcNames.length == 1) {
            funcName = funcNames[0];
        }
        ActionMethod method = action.method();
        if (StringUtils.isEmpty(funcName)) {
            funcName = ActionMethod.getFuncName(method);
        }
        return pageService.getQuery(apiKey, funcName);
    }


    protected Either<Code, Model> getModel(MethodParameter parameter, NativeWebRequest webRequest) {
        Action action = parameter.getMethodAnnotation(Action.class);
        if (action == null) {
            return Either.left(Code.NOT_FOUND);
        }
        Model model = (Model) webRequest.getAttribute(MODEL_ARGUMENT_RESOLVER, RequestAttributes.SCOPE_REQUEST);
        if (model != null) {
            return Either.right(model);
        }
        String apiKey = getApiKey(webRequest);
        String[] funcNames = webRequest.getParameterValues(ActionMethod.FUNC_NAME);
        String funcName = null;
        if (funcNames != null && funcNames.length == 1) {
            funcName = funcNames[0];
        }

        ActionMethod method = action.method();
        if (StringUtils.isEmpty(funcName)) {
            funcName = ActionMethod.getFuncName(method);
        }
        Either<Code, Model> result;
        if (method == ActionMethod.QUERY) {
            result = pageService.getTable(apiKey, funcName);
        } else if (method == ActionMethod.VIEW) {
            result = pageService.getView(apiKey, funcName);
        } else {
            result = pageService.getForm(apiKey, funcName);
        }

        result.forEach(e -> {
            webRequest.setAttribute(MODEL_ARGUMENT_RESOLVER, e, RequestAttributes.SCOPE_REQUEST);
        });
        return result;
    }
}
