package com.gitssie.openapi;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.ebean.EbeanApi;
import com.gitssie.openapi.models.user.User;
import com.gitssie.openapi.models.user.UserModel;
import com.gitssie.openapi.models.user.UserView;
import com.gitssie.openapi.page.ModelNodeConversion;
import com.gitssie.openapi.page.NeedContext;
import com.gitssie.openapi.page.NodeConversionMap;
import com.gitssie.openapi.page.PageView;
import com.gitssie.openapi.service.Provider;
import com.gitssie.openapi.utils.Json;
import com.google.common.collect.Maps;
import io.ebean.Database;
import io.ebean.Expression;
import io.ebean.ExpressionList;
import io.ebean.bean.EntityBean;
import io.ebean.bean.EntityBeanIntercept;
import io.vavr.Lazy;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
public class JsSpringTest {
    @Autowired
    private EbeanApi api;
    @Autowired
    private Database database;
    @Autowired
    private NodeConversionMap conversionJson;
    @Autowired
    private ModelNodeConversion nodeConversion;
    @Autowired
    private Provider provider;


    @Test
    public void testUserQuery() {
        List<User> users = database.createQuery(User.class)
//                .fetch("dimDepartRef")
                .where().gt("id", 0)//.isNotEmpty("roles.name")
                .eq("dimDepart.departName", "总经办")//.isNotEmpty("roles.nam
                .findList();

        for (User user : users) {
            System.out.println(user.getDimDepart().getDepartName());
        }
    }


    @Test
    public void testViewQuery() {
        List<UserView> userViews = database.createQuery(UserView.class)
//                .fetch("dimDepart")
                .where()
                .gt("id", 0)
                .eq("dimDepart.departName", "总经办")//.isNotEmpty("roles.name")
                .findList();

        userViews.forEach(e -> {
            System.out.println(e.getDimDepart().getDepartName());
        });
    }

    @Test
    public void testJsonNodeConversion() {
        String apiKey = "saleOrder";
        PageView pageView = new PageView();
        var model = pageView.getCreate(apiKey, "Create").get();
        ObjectNode node = Json.newObject();
        node.put("orderNo", "12345");
        NeedContext context = new NeedContext();
        database.json().toJson("");
        var create = model.createOne(provider.desc(apiKey), provider);
        create.create(node, nodeConversion);
    }

    @Test
    public void testJSONConversion() throws Exception {
        String str = "{\n" +
                "    \"phone\": \"17620319037\",\n" +
                "    \"personalEmail\": \"jack@qq.com\",\n" +
                "    \"employeeCode\": null,\n" +
                "    \"roles\": [\n" +
                "        {\n" +
                "            \"id\": 2302515711315968\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 2302515711315969\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 2302515711315971\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        JSONObject node = JSONObject.parseObject(str);
        User user = api.find(User.class, 16);
        NeedContext context = new NeedContext();
        conversionJson.copy(context, provider, provider.desc(User.class), node, (EntityBean) user);

        EntityBean eb = (EntityBean) user;
        EntityBeanIntercept ebi = eb._ebean_getIntercept();

        System.out.println(ebi.getDirtyPropertyNames());

        //api.update(user);
    }

    @Test
    public void testEbeanApi() {
        ExpressionList expr = api.where()
                .idEq(16);
//                .eq("status", 0);
        List<User> user = api.list(User.class, expr);
        System.out.println(user);
    }

//    @Test
//    public void testRepository() {
//        repository.save(new User());
//    }
//
//    @Test
//    public void testJsQuery() {
//        Map<String, Object> parameters = Maps.newHashMap();
//        parameters.put("begin", DateUtils.addMonths(new Date(), -10));
//        parameters.put("end", new Date());
//        Object a = repository.findByLastname("aaaa", parameters, Pageable.ofSize(10));
//        System.out.println(a);
//    }

    @Test
    public void testSqlQuery() {
        User user = database.findNative(User.class, "select * from user limit 1").findOne();
        System.out.println(user.getName());
    }

    @Configuration
    @Import(AppConfig.class)
    public static class Config {

    }

}
