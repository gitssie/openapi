package com.gitssie.openapi.ebean.repository;

import com.google.common.collect.Maps;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.AstRoot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

public class EbeanJsEngine {
    private Map<Class, Scriptable> repositoryScriptableCache;

    public EbeanJsEngine() {
        this.repositoryScriptableCache = Maps.newConcurrentMap();
    }

    public Scriptable compileAndCached(Class<?> clazz, String resource) throws IOException {
        Scriptable scope = compile(resource);
        repositoryScriptableCache.put(clazz, scope);
        return scope;
    }

    public Scriptable compile(String resource) throws IOException {
        resource = getFilePath(resource);
        URL fileURL = ResourceUtils.getURL(resource);
        String jsCode = StreamUtils.copyToString(fileURL.openStream(), Charset.forName("utf-8"));
        // 定义自定义的解析器
        Parser parser = new Parser();
        // 解析脚本
        AstRoot astRoot = parser.parse(jsCode, null, 1);
        astRoot.visitAll(new SQLPlaceHolder());
        jsCode = astRoot.toSource();
        Context context = Context.enter();
        try {
            Script script = context.compileString(jsCode, resource, 1, null);
            ScriptableObject scope = context.initSafeStandardObjects(null, true);
            script.exec(context, scope);
            scope.sealObject();
            return scope;
        } finally {
            context.close();
        }
    }

    public String getFilePath(String resource) {
        return String.format("classpath:templates/sql/%s", resource);
    }

    public Option<java.util.function.Function<Object[], Tuple2<String, Object[]>>> getHandler(Class<?> clazz, String name) {
        Scriptable scope = repositoryScriptableCache.get(clazz);
        if (scope == null) {
            return Option.none();
        }
        Object code = scope.get(name, scope);
        if (code == null || !(code instanceof Function)) {
            return Option.none();
        }
        Function func = (Function) code;
        return Option.of((args) -> toSQL(func, args));
    }

    private NativeObject mapToNativeObject(NativeObject scope, Map<String, Object> mp) {
        if (mp == null) {
            return scope;
        }
        Object value;
        for (Map.Entry<String, Object> entry : mp.entrySet()) {
            value = entry.getValue();
            if (value != null && value instanceof Map) {
                if (!(value instanceof Scriptable)) {
                    value = mapToNativeObject(new NativeObject(), (Map<String, Object>) value);
                }
            }
            scope.put(entry.getKey(), scope, value);
        }
        return scope;
    }

    private Tuple2<String, Object[]> toSQL(Function func, Object[] args) {
        Object[] parameters = new Object[args.length - 1];
        NativeObject scope = new NativeObject();
        scope.put("page", scope, Pageable.unpaged());
        Pageable page = null;
        if (args.length > 0) {
            if (args[0] != null && args[0] instanceof Map) {
                Map<String, Object> mp = (Map) args[0];
                mapToNativeObject(scope, mp);
            }
            if (args.length > 1 && args[1] != null && args[1] instanceof Pageable) {
                page = (Pageable) args[1];
                scope.put("page", scope, page);
            }
            if (args.length > 2 && args[2] != null && args[2] instanceof Sort) {
                scope.put("sort", scope, sortToFunction((Sort) args[1]));
            } else if (page != null && page.getSort() != null) {
                scope.put("sort", scope, sortToFunction(page.getSort()));
            }
        } else {
            scope = new NativeObject();
        }
        return toSQL(func, scope, parameters);
    }

    public Tuple2<String, Object[]> toSQL(Function func, NativeObject scope, Object[] parameters) {
        Context context = Context.enter();
        try {
            SQLParameterFunction pf = new SQLParameterFunction();
            scope.put(pf.getParameterFuncName(), scope, pf);
            //call function
            Object result = func.call(context, scope, scope, parameters);
            String sql = context.toString(result);
            return new Tuple2<>(sql, pf.getArgs().toArray());
        } finally {
            context.close();
        }
    }

    private Function sortToFunction(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return null;
        }
        return new SortParameterFunction(sort);
    }

    public static class SortParameterFunction extends BaseFunction {
        private Sort sort;
        private StringBuilder buf;

        public SortParameterFunction(Sort sort) {
            this.sort = sort;
        }

        @Override
        public Object call(Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (sort.isUnsorted()) {
                return "";
            }
            if (buf == null) {
                buf = new StringBuilder();
            }
            buf.setLength(0);
            int len = 0;
            String tmp;
            String split = "";
            if (args.length > 0) {
                if (args[0] != null && !(args[0] instanceof Undefined)) {
                    tmp = args[0].toString();
                    if (tmp.length() > 0) {
                        buf.append(tmp);
                        buf.append(" "); //order by
                    }
                }
            }
            if (args.length > 1 && args[1] != null && !(args[1] instanceof Undefined)) {
                tmp = args[1].toString();
                if (tmp.length() > 0) {
                    split = args[1] + ".";
                }
            }
            len = buf.length();
            if (args.length > 2) {
                for (int i = 2; i < args.length; i++) {
                    tmp = args[i].toString();
                    for (Sort.Order order : this.sort) {
                        if (tmp.equals(order.getProperty())) {
                            buf.append(split);
                            buf.append(order.getProperty()).append(" ").append(order.getDirection().name());
                            buf.append(",");
                        }
                    }
                }
            } else {
                for (Sort.Order order : this.sort) {
                    buf.append(split);
                    buf.append(order.getProperty()).append(" ").append(order.getDirection().name());
                    buf.append(",");
                }
            }

            if (buf.length() > len) {
                buf.setLength(buf.length() - 1);
            }
            return buf.toString();
        }
    }
}
