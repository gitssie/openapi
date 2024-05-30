package com.gitssie.openapi;

import com.gitssie.openapi.xentity.NamingUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.util.ParsingUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class TestOpenApi {

    @Test
    public void testA() {
        RestTemplateBuilder b = new RestTemplateBuilder();
        RestTemplate t = b.build();
        String url = "http://so.chipsea.com:8002/sso/validate?service=http://sfh.szhcxx.cn/oauth2/ticket?code%3D9875035b51bbe5ba2ba4644d1f29e34b&ticket=ST-25-lfPsasXkvBC7fo3Zbzul-cas";
        String res = t.getForObject(url,String.class);


        String code = DigestUtils.md5DigestAsHex(String.valueOf(System.currentTimeMillis()).getBytes());
        System.out.println(code);

        String service = getOauthHost("1234567890");


    }

    private String getOauthHost(String code) {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl("http://sfh.szhcxx.cn");
        b.path("/oauth2/ticket");/**/
        b.queryParam("code", code);
        return b.toUriString();
    }


    @Test
    public void testSimple() {
        String name = "categOR_YNAme";
        String n1 = ParsingUtils.reconcatenateCamelCase(name, "_");
        String n2 = NamingUtils.reCamelCase(name);
        System.out.println(n1);
        System.out.println(n2);

        name = "CAGEGORY_NAME_HIDE__c";
        System.out.println(name);
        name = NamingUtils.reCamelCase(name);
        String n3 = NamingUtils.toCamelCase(name);
        String n4 = NamingUtils.reCamelCase("cagegoryNameHide__c");
        System.out.println(n3);
        System.out.println(n4);
    }

}
