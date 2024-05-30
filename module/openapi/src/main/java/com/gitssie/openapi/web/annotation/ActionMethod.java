package com.gitssie.openapi.web.annotation;

public enum ActionMethod {
    QUERY, VIEW, PUT, CREATE, POST, EDIT, PATCH, DELETE;

    public static final String API_KAY = "apiKey";
    public static final String FUNC_NAME = "funcName";

    public static String getFuncName(ActionMethod method) {
        if (method == ActionMethod.QUERY) {
            return "List";
        } else if (method == ActionMethod.VIEW) {
            return "View";
        } else if (method == ActionMethod.PUT || method == ActionMethod.CREATE) {
            return "Create";
        } else if (method == ActionMethod.PATCH || method == ActionMethod.EDIT) {
            return "Edit";
        } else if (method == ActionMethod.DELETE) {
            return "Delete";
        }
        return null;
    }
}
