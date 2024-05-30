package com.gitssie.openapi;

import com.gitssie.openapi.ebean.repository.EbeanJsEngine;
import com.gitssie.openapi.rule.Rule;
import com.gitssie.openapi.rule.Rules;
import io.vavr.Tuple2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.*;
import org.springframework.data.domain.Sort;
import org.springframework.util.ResourceUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class JsTest {


    @Test
    public void testLink() {
        String value = MessageFormat.format("{0}****", "你好");
        System.out.println(value);
    }

    @Test
    public void testCSV() throws IOException {
        FileReader fr = new FileReader("E:/tmpdir/13554子项目_20240524135902327_CRMADMIN (2).csv", Charset.forName("GBK"));
        FileWriter fw = new FileWriter("E:/tmpdir/13554子项目_20240524135902327_CRMADMIN(2).csv", Charset.forName("GBK"));
        BufferedReader br = new BufferedReader(fr);
        BufferedWriter bw = new BufferedWriter(fw);
        String line;
        int i = 0;
        Map<String, String> map = new HashMap<>();
        while (br.ready()) {
            line = br.readLine();
            i++;
            if (i == 1) {
                bw.write(line);
                bw.write('\n');
                continue;
            }
            String[] pt = line.split(",");
            if (pt.length < 2) {
                bw.write(line);
                bw.write('\n');
                continue;
            }
            String p = "";
            int j = pt[2].indexOf('-');
            if (j > 0) {
                p = pt[2].substring(j + 1, pt[2].length());
            }
            bw.write(line);
            bw.write(",`");
            bw.write(p);
            bw.write('\n');
        }
        IOUtils.closeQuietly(br);
        IOUtils.closeQuietly(bw);
    }

    @Test
    public void testSort() {
        Sort st = Sort.by(Sort.Order.asc("id"), Sort.Order.desc("date"));
        EbeanJsEngine.SortParameterFunction f1 = new EbeanJsEngine.SortParameterFunction(st);
        //sort("t","id")
        System.out.println(f1.call(null, null, null, new Object[]{"order by", "t", "id", "date"}));

    }

    private String sortString(Sort st, String prefix, String... a) {

        return "";
    }


    @Test
    public void testJsQuery() throws Exception {
        File jsFile = ResourceUtils.getFile("classpath:view/saleOrder.js");
        String jsCode = FileUtils.readFileToString(jsFile, "utf-8");
        Context context = Context.enter();
        try {
            Script script = context.compileString(jsCode, jsFile.getName(), 1, null);
            Scriptable parent = context.initSafeStandardObjects(null, true);
            NativeObject scope = new NativeObject();
            scope.setParentScope(parent);
            scope.put("required", scope, Rules.required);
            scope.put("length", scope, Rules.length);
            scope.put("after", scope, Rules.after);
            scope.put("array", scope, Rules.array);
            scope.put("ascii", scope, Rules.ascii);
            scope.put("between", scope, Rules.between);
            scope.put("date", scope, Rules.date);

            NativeObject runScope = new NativeObject();
            runScope.setParentScope(scope);
            script.exec(context, runScope);
            runScope.sealObject();

            Function func = (Function) runScope.get("Edit", runScope);
            NativeObject that = new NativeObject();

            // EbeanJsEngine.ParameterFunction pf = new EbeanJsEngine.ParameterFunction();
            // scope.put(PARAMETER_FUNC_NAME, scope, pf);

            NativeObject result = (NativeObject) func.call(context, runScope, null, new Object[0]);
            NativeObject form = (NativeObject) result.get("form");
            NativeArray columns = (NativeArray) form.get("columns");
            NativeObject item = (NativeObject) columns.get(0);
            Rule rule = (Rule) item.get("rule");
            System.out.println(rule.validate(Rules.context(), "3333333", "", null, null));
            String json = (String) NativeJSON.stringify(context, runScope, result, null, null);
            System.out.println(json);
        } finally {
            context.close();
        }
    }

    @Test
    public void testJsEngine() throws Exception {
        EbeanJsEngine engine = new EbeanJsEngine();
        Scriptable scope = engine.compile("classpath:JsTest.js");
        Context context = Context.enter();

        try {
            Function f1 = (Function) scope.get("getUserById", scope);
            NativeObject m2 = new NativeObject();
            m2.put("date", m2, new Date());
            m2.put("file", m2, new File("xxx"));
            NativeObject that = new NativeObject();
            Tuple2<String, Object[]> sql = engine.toSQL(f1, that, new Object[]{"Jack", m2});
            System.out.println(sql._1);
            for (Object o : sql._2) {
                System.out.println(o.getClass());
            }
        } finally {
            context.close();
        }

    }

    @Test
    public void testJsEngineMutilThread() throws Exception {
        EbeanJsEngine engine = new EbeanJsEngine();
        Scriptable scope = engine.compile("classpath:JsTest.js");
        Function f1 = (Function) scope.get("getUserById", scope);
        AtomicInteger i = new AtomicInteger();
        Thread thread = Thread.currentThread();
        Flux.range(1, 10000)
                .parallel()
                .runOn(Schedulers.parallel())
                .subscribe(e -> {
                    i.incrementAndGet();
                    NativeObject that = new NativeObject();
                    System.out.println(engine.toSQL(f1, that, new Object[]{"Jack"}));
                });
        thread.join();
    }

    @Test
    public void testJsRun() throws Exception {
        Context rhinoContext = Context.enter();
        Function result;
        try {
            // 初始化全局作用域
            Scriptable scope = rhinoContext.initStandardObjects();
            //Object var1 = Context.javaToJS("Javas' Hello World",scope);

            Map<String, Object> map1 = new HashMap<>();
            map1.put("cc", 1);
            map1.put("abc", 2);

            Object var2 = Context.javaToJS(map1, scope);
            scope.put("var2", scope, var2);
            scope.put("var1", scope, "Javas' Hello World");
            scope.put("var3", scope, 1);

            NativeObject ob = (NativeObject) scope;
            for (Map.Entry<Object, Object> en : ob.entrySet()) {
                System.out.println(en.getKey());
            }
            File jsFile = ResourceUtils.getFile("classpath:JsTest.js");
            String javascriptCode = FileUtils.readFileToString(jsFile, "utf-8");
            // 执行 JavaScript 代码
            // 定义自定义的解析器
            Parser parser = new Parser();
            // 解析脚本
            AstRoot astRoot = parser.parse(javascriptCode, null, 1);
            StringBuilder buf = new StringBuilder();
            astRoot.visitAll((node) -> {
                if (node instanceof StringLiteral) {
                    StringLiteral cp = (StringLiteral) node;
                    String raw = cp.getValue();
                    int i = raw.indexOf('#');
                    int j = raw.indexOf('{', i);
                    int k = raw.indexOf('}', j);
                    if (i >= 0 && j > i && k > j) {
                        buf.setLength(0);
                        buf.append(raw.substring(0, i));
                        buf.append("${setParameter(");
                        buf.append(raw.substring(j + 1, k));
                        buf.append(")}");
                        buf.append(raw.substring(k + 1));
                        cp.setValue(buf.toString());
                    }
                } else if (node instanceof TemplateLiteral) {
                    TemplateLiteral tp = (TemplateLiteral) node;
                    for (AstNode element : tp.getElements()) {
                        if (element instanceof TemplateCharacters) {
                            TemplateCharacters cp = (TemplateCharacters) element;
                            String raw = cp.getRawValue();
                            int i = raw.indexOf('#');
                            int j = raw.indexOf('{', i);
                            int k = raw.indexOf('}', j);
                            if (i >= 0 && j > i && k > j) {
                                buf.setLength(0);
                                buf.append(raw.substring(0, i));
                                buf.append("${setParameter(");
                                buf.append(raw.substring(j + 1, k));
                                buf.append(")}");
                                buf.append(raw.substring(k + 1));
                                cp.setRawValue(buf.toString());
                            }
                        }
                    }
                }
                return true;
            });
            scope.put("setParameter", scope, new MyJavaFunction());

            String transformedScript = astRoot.toSource();
            System.out.println(transformedScript);
            Script script = rhinoContext.compileString(transformedScript, "RhinoScript", 1, null);
            script.exec(rhinoContext, scope);
            Function f1 = (Function) scope.get("getUserById", scope);
            Function f2 = (Function) scope.get("getUserById2", scope);
            Function f3 = (Function) scope.get("setParameter", scope);
            result = f1;

            // 处理 JavaScript 执行结果
            System.out.println(f1.getClass().getSuperclass());
            System.out.println(f2.getClass());
            System.out.println(f3.getClass());
        } finally {
            // 退出 Rhino 上下文
            Context.exit();
        }
        rhinoContext = Context.enter();
        Scriptable scope = rhinoContext.initSafeStandardObjects();
        System.out.println(result.call(rhinoContext, scope, null, new Object[]{"Jack"}));
    }

    public static class MyJavaFunction extends BaseFunction {
        @Override
        public Object call(Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length > 0) {
                Object ag = args[0];
                if (ag instanceof Undefined) {
                    return 'x';
                }
                String name = Context.toString(ag);
                System.out.println("Hello, " + name + "!");
            }
            return "?";
        }
    }
}
