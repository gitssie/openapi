package com.gitssie.openapi;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.ebean.EbeanPredicateService;
import com.gitssie.openapi.utils.Json;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.util.DSelectColumnsParser;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthTest {

    static class JsonEntry{
        private Date date;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    @Test
    public void testJson() {
        var data = new JsonEntry();
        data.setDate(new Date());
        String json = Json.toJson(data).toString();
//        json="{\"date\":\"2023-01-01 15:00:00\"}";
        System.out.println(json);
        System.out.println(Json.fromJson(Json.parse(json), JsonEntry.class).getDate());
    }

    @Test
    public void testMatcher() {
        AntPathRequestMatcher matcher = new AntPathRequestMatcher("/api/data/object/{apiKey}", null);

        MockHttpServletRequest req = MockMvcRequestBuilders.post("/api/data/object/user").buildRequest(new MockServletContext());
        RequestMatcher.MatchResult result = matcher.matcher(req);
        System.out.println(result.isMatch());
        System.out.println(result.getVariables());
    }

    @Test
    public void testSQLParse() {
        String x = "count(children.id*chi1l_Dren.taxAmount)";
        String b = "sum(children.taxAmount)";
        String c = "sum(taxAmount)";
        Set<String> st = DSelectColumnsParser.parse("count(children.id*children.taxAmount),sum(children.taxAmount)");
        Pattern pt = Pattern.compile("([\\w\\d\\_]+\\.)+");
        Matcher mt = pt.matcher(x + ',' + b);
        while (mt.find()) {

            System.out.println(String.format("%s,%s,%s", mt.start(), mt.end(), mt.group(1)));
        }
    }

    @Test
    public void testSQLParse2() {
        EbeanPredicateService p = new EbeanPredicateService(null, null);
        List<String> aggre = Lists.newArrayList("count(children.id*children.taxAmount)", "sum(children.taxAmount)");
        Either<Code, Tuple2<Optional<Property>, String>> e = p.parseAggregationProperties(null, aggre);

        System.out.println(e);

    }
}
