package com.gitssie.openapi.page;

import com.gitssie.openapi.ebean.repository.SQLPlaceHolder;
import com.gitssie.openapi.models.layout.Component;
import com.gitssie.openapi.utils.TypeUtils;
import com.gitssie.openapi.rule.Rules;
import com.gitssie.openapi.validator.UtilsNativeObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.AstRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author: Awesome
 * @create: 2024-02-07 09:40
 */
public class PageView {
    private static final Logger LOGGER = LoggerFactory.getLogger(PageView.class);
    private static final String DEFAULT_TEMPLATE_PATH = "classpath:templates/view";
    private final Cache<String, Option<ScopeView>> scopeViewCache; //文件缓存
    private final Map<String, LazyValueResolver> valueResolvers;
    private final NativeObject console;
    private final String templatePath;

    public PageView() {
        this(Collections.EMPTY_MAP, null);
    }

    public PageView(Map<String, LazyValueResolver> valueResolvers) {
        this(valueResolvers, null);
    }

    public PageView(Map<String, LazyValueResolver> valueResolvers, String templatePath) {
        this.valueResolvers = valueResolvers;
        this.templatePath = StringUtils.defaultIfBlank(templatePath, DEFAULT_TEMPLATE_PATH);
        this.scopeViewCache = CacheBuilder
                .newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(Duration.ofHours(1))
                .build();
        this.console = createConsole();
    }

    public Option<AggreModel> getAggre(String apiKey, String funcName) {
        return getComponent(apiKey, funcName, "aggre").map(this::toAggreModel); //ORM 聚合函数查询
    }

    public Option<Model> getQuery(String apiKey, String funcName) {
        return getModel(apiKey, funcName, "query", this::parseQueryField);//ORM 查询表单
    }

    public Option<Model> getTable(String apiKey, String funcName) {
        return getModel(apiKey, funcName, "table", this::parseColumn);//ORM 列表返回数据
    }

    public Option<Model> getView(String apiKey, String funcName) {
        return getModel(apiKey, funcName, "view", this::parseColumn).orElse(() -> getForm(apiKey, funcName));//ORM 详情视图返回数据
    }

    public Option<Model> getCreate(String apiKey, String funcName) {
        return getModel(apiKey, funcName, "form", this::parseColumn);//ORM 新增创建表单
    }

    public Option<Model> getEdit(String apiKey, String funcName) {
        return getModel(apiKey, funcName, "form", this::parseColumn);//ORM 修改表单
    }

    public Option<Model> getForm(String apiKey, String funcName) {
        return getModel(apiKey, funcName, "form", this::parseColumn);//ORM 修改表单
    }

    public Option<Model> getModel(String apiKey, String funcName, String type, java.util.function.Function<Map<String, ?>, Field> parseColumn) {
        String key = String.format("%s.%s", apiKey, funcName);
        return getScope(apiKey).flatMap(e -> {
            return getFunction(e, funcName).flatMap(func -> {
                return runFunction(key, e, func);
            });
        }).flatMap(e -> toModel(e, apiKey, type, parseColumn));
    }

    public Option<Component> getComponent(String apiKey, String funcName, String type) {
        String key = String.format("%s.%s", apiKey, funcName);
        return getScope(apiKey).flatMap(e -> {
            return getFunction(e, funcName).flatMap(func -> {
                return runFunction(key, e, func);
            });
        }).flatMap(e -> toComponent(e, type));
    }

    public Option<Function> getFunction(String apiKey, String funcName) {
        return getScope(apiKey).flatMap(e -> {
            return getFunction(e, funcName);
        });
    }

    private Option<Function> getFunction(ScopeView view, String funcName) {
        Object function = view.scope.get(funcName, view.scope);
        if (function instanceof Function) {
            return Option.of((Function) function);
        } else {
            return Option.none();
        }
    }

    public Option<Model> getTable() {
        return Option.none();
    }

    public Option<Model> getForm() {
        return Option.none();
    }

    public String getFilePath(String apiKey) {
        String path = templatePath;
        if (path.startsWith("src/")) {
            String base = System.getProperty("java.class.path");
            if (StringUtils.isNotBlank(base)) {
                base = base.substring(0, base.indexOf(File.pathSeparator));
                base = base.substring(0, base.lastIndexOf(File.separator));
                base = base.substring(0, base.lastIndexOf(File.separator));
                path = base + File.separator + path;
            }
        }
        return String.format("%s/%s.js", path, apiKey);
    }

    public static Option<Model> toModel(Map result, String apiKey, String type, java.util.function.Function<Map<String, ?>, Field> parseColumn) {
        return toComponent(result, type).map(component -> {
            return ComponentRender.parse(apiKey, component, parseColumn); //component toModel
        });
    }

    public static Option<Model> toModel(Map result) {
        return toComponent(result, null).map(component -> {
            return ComponentRender.parse(null, component, null); //component toModel
        });
    }

    public static Option<Model> toModel(List columns) {
        return toModel(columns, null);
    }

    public static Option<Model> toModel(List columns, String apiKey) {
        Component component = new Component();
        component.setColumns(columns);
        return Option.of(ComponentRender.parse(apiKey, component, null));
    }

    public static Option<Component> toComponent(Map result, String type) {
        List<Object> columnsAsList = null;
        if (StringUtils.isNotEmpty(type)) {
            Object typeResult = result.get(type);
            if (typeResult == null || typeResult instanceof Undefined) {
                return Option.none();
            }
            if (typeResult instanceof Map) {
                Object columns = ((Map<?, ?>) typeResult).get(ComponentRender.COLUMNS);
                if (columns != null && columns instanceof List) {
                    columnsAsList = (List<Object>) columns;
                }
                result = (Map) typeResult;
            } else if (typeResult instanceof List) {
                columnsAsList = (List<Object>) typeResult;
            } else {
                return Option.none();
            }
        }
        Component component = new Component(result);
        component.setType(type);
        component.setColumns(columnsAsList);
        return Option.of(component);
    }

    public AggreModel toAggreModel(Component component) {
        List<String> aggreColumns = (List<String>) component.get(ComponentRender.COLUMNS);
        String type = TypeUtils.castToString(component.get(ComponentRender.TYPE));
        Object rawSql = component.get(ComponentRender.RAW_SQL);
        AggreModel aggreModel = new AggreModel(aggreColumns, component);
        if (StringUtils.isNotEmpty(type)) {
            aggreModel.setType(type); //查询类型 map list
        }
        if (rawSql instanceof Function) {
            aggreModel.setRawSql((Function) rawSql);
        }
        return aggreModel;
    }


    private Field parseColumn(Map<String, ?> columnAsMap) {
        return null;
    }

    private Field parseQueryField(Map<String, ?> columnAsMap) {
        return ComponentRender.createField(TypeUtils.castToString(columnAsMap.get(ComponentRender.NAME)), columnAsMap);
    }

    public static Option<NativeObject> runFunction(String key, ScopeView view, Function func) {
        return view.runFunction(key, func);
    }

    public Option<ScopeView> getScope(String apiKey) {
        try {
            Option<ScopeView> view = Option.none();
            for (int i = 0; i < 2; i++) {
                view = scopeViewCache.get(apiKey, () -> this.loadScope(apiKey));
                if (view.isEmpty()) {
                    scopeViewCache.invalidate(apiKey);
                    break;
                } else if (view.get().isChanged()) {
                    scopeViewCache.invalidate(apiKey);
                    view.get().invalidate();
                } else {
                    break;
                }
            }
            return view;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Option<ScopeView> loadScope(String apiKey) {
        try {
            return doLoadScope(apiKey);
        } catch (FileNotFoundException e) {
            LOGGER.warn("load {} view file not found", apiKey);
            return Option.none();
        } catch (Exception e) {
            LOGGER.error("load {} view error", apiKey, e);
            throw new RuntimeException(e);
        }
    }

    private Option<ScopeView> doLoadScope(String apiKey) throws Exception {
        String filePath = getFilePath(apiKey);
        URL fileURL = ResourceUtils.getURL(filePath);
        String jsCode = StreamUtils.copyToString(fileURL.openStream(), Charset.forName("utf-8"));
        // 定义自定义的解析器
        Parser parser = new Parser();
        AstRoot astRoot = parser.parse(jsCode, null, 1);
        astRoot.visitAll(new SQLPlaceHolder("scope"));
        jsCode = astRoot.toSource();

        Context context = Context.enter();
        try {
            Script script = context.compileString(jsCode, filePath, 1, null);
            Scriptable parent = context.initSafeStandardObjects(new ImporterTopLevel(context), true);
            NativeObject scope = new NativeObject();
            scope.setParentScope(parent);
            initRules(scope);

            for (Map.Entry<String, LazyValueResolver> entry : valueResolvers.entrySet()) {
                scope.put(entry.getKey(), scope, Context.javaToJS(entry.getValue(), scope));
            }

            NativeObject runScope = new NativeObject();
            runScope.setParentScope(scope);
            script.exec(context, runScope);
            runScope.sealObject();
            Option<File> file = Option.none();
            if (ResourceUtils.isFileURL(fileURL)) {
                file = Option.of(ResourceUtils.getFile(fileURL));
            }
            ScopeView view = new ScopeView(file, file.map(e -> e.lastModified()).getOrElse(0l), runScope);
            return Option.of(view);
        } finally {
            context.close();
        }
    }


    public void initRules(NativeObject scope) {
        for (Map.Entry<String, Object> entry : Rules.INSTANCE.getRules().entrySet()) {
            scope.put(entry.getKey(), scope, entry.getValue());
        }
        scope.put("format", scope, Rules.format);
        scope.put("parse", scope, Rules.parse);
        scope.put("parseDate", scope, Rules.parseDateFunction);
        scope.put("console", scope, console);
        scope.put("utils", scope, Context.javaToJS(new UtilsNativeObject(), scope));
    }

    public NativeObject createConsole() {
        NativeObject scope = new NativeObject();
        scope.put("trace", scope, new LoggerFunction(0));
        scope.put("debug", scope, new LoggerFunction(1));
        scope.put("info", scope, new LoggerFunction(2));
        scope.put("log", scope, new LoggerFunction(2));
        scope.put("warn", scope, new LoggerFunction(3));
        scope.put("src/main/public/error", scope, new LoggerFunction(4));
        return scope;
    }

    private static class LoggerFunction extends BaseFunction {
        private static final Logger LOGGER = LoggerFactory.getLogger(PageView.class.getPackageName() + ".Console");
        private int level = 0;//TRACE,DEBUG,INFO,WARN,ERROR

        public LoggerFunction(int level) {
            this.level = level;
        }

        @Override
        public Object call(Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length == 0) {
                return null;
            }
            String message = unwrap(args[0]).toString();
            Object[] arguments = toArguments(args);
            switch (level) {
                case 0:
                    LOGGER.trace(message, arguments);
                    break;
                case 1:
                    LOGGER.debug(message, arguments);
                    break;
                case 2:
                    LOGGER.info(message, arguments);
                    break;
                case 3:
                    LOGGER.warn(message, arguments);
                    break;
                case 4:
                    LOGGER.error(message, arguments);
                    break;
            }
            return null;
        }

        public Object[] toArguments(Object[] args) {
            Object[] arguments = new Object[args.length - 1];
            if (arguments.length > 0) {
                for (int i = 1; i < args.length; i++) {
                    arguments[i - 1] = unwrap(args[i]);
                }
            }
            return arguments;
        }

        private Object unwrap(Object value) {
            if (value instanceof Wrapper) {
                return ((Wrapper) value).unwrap();
            } else {
                return value;
            }
        }
    }


    private static class ScopeView {
        private Option<File> file;
        private long lastModifyAt;
        private Scriptable scope;

        private Map<String, Option<NativeObject>> viewCache;


        public ScopeView(Option<File> file, long lastModifyAt, Scriptable scope) {
            this.file = file;
            this.lastModifyAt = lastModifyAt;
            this.scope = scope;
            this.viewCache = Maps.newConcurrentMap();
        }

        public boolean isChanged() {
            return file.map(e -> e.lastModified() > lastModifyAt).getOrElse(false);
        }

        public void invalidate() {
            viewCache.clear();
        }

        public Option<NativeObject> runFunction(String key, Function func) {
            return viewCache.computeIfAbsent(key, (e) -> {
                return runFunction(func);
            });
        }

        public Option<NativeObject> runFunction(Function func) {
            Context context = Context.enter();
            try {
                Object result = func.call(context, scope, null, new Object[0]);
                if (result instanceof NativeObject) {
                    return Option.of((NativeObject) result);
                }
                return Option.none();
            } finally {
                context.close();
            }
        }
    }
}
