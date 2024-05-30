package com.gitssie.openapi.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.auth.RolePermissionEvaluator;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.page.AggreModel;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.service.AuthenticationService;
import com.gitssie.openapi.service.DataService;
import com.gitssie.openapi.service.PageService;
import com.gitssie.openapi.service.XObjectService;
import com.gitssie.openapi.web.annotation.*;
import com.gitssie.openapi.web.query.QueryForm;
import io.ebean.Expression;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author: Awesome
 * @create: 2024-02-18 16:45
 */
@Controller
@Transactional
public class JsViewAction {
    @Autowired
    private XObjectService objectService;
    @Autowired
    private DataService dataService;
    @Autowired
    private PageService pageService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private RolePermissionEvaluator permissionEvaluator;
    @Autowired
    private MessageSource messageSource;

    /**
     * 数据查询
     *
     * @param apiKey
     * @param queryMap
     * @return
     */
    @Query
    @PostMapping("/api/view/object/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'list')")
    public Either<Code, Page<Map<String, Object>>> query(Authentication authentication,
                                                         @PathVariable String apiKey,
                                                         @Validated QueryForm queryMap,
                                                         BeanType desc,
                                                         Model model) {
        //获取数据查询过滤条件
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.queryMap(desc, model, dataPermission, queryMap.getQuery(), queryMap.getPageable());
    }


    /**
     * 获取详情
     *
     * @param apiKey
     * @param id
     * @return
     */
    @View
    @GetMapping("/api/view/object/{apiKey}/{id}")
    @PreAuthorize("hasPermission(#apiKey, 'view')")
    public Either<Code, Map<String, Object>> getDetail(Authentication authentication,
                                                       @PathVariable String apiKey,
                                                       @PathVariable String id,
                                                       BeanType desc,
                                                       Model model) {

        //获取数据查询过滤条件
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.fetchDetail(desc, id, model, dataPermission);
    }

    /**
     * 数据新增
     *
     * @param apiKey
     * @param body
     * @return
     */
    @Add
    @PutMapping("/api/view/object/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'add')")
    public Either<Code, Map<String, Object>> create(Authentication authentication,
                                                    @PathVariable String apiKey,
                                                    BeanType desc,
                                                    Model model,
                                                    @RequestBody ObjectNode body) {
        //获取数据查询过滤条件
        //Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.create(desc, body, model);
    }

    @Add
    @PutMapping("/api/view/batch/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'add')")
    public Either<Code, List<Map<String, Object>>> batchSave(Authentication authentication,
                                                             @PathVariable String apiKey,
                                                             BeanType desc,
                                                             Model model,
                                                             @RequestBody ArrayNode body) {

        return objectService.restBatchSave(desc, body, model);
    }

    /**
     * 数据修改
     *
     * @param apiKey
     * @param id
     * @param body
     * @return
     */
    @Edit
    @PatchMapping("/api/view/object/{apiKey}/{id}")
    @PreAuthorize("hasPermission(#apiKey, 'edit')")
    public Either<Code, Map<String, Object>> patch(Authentication authentication,
                                                   @PathVariable String apiKey,
                                                   @PathVariable String id,
                                                   BeanType desc,
                                                   Model model,
                                                   @RequestBody ObjectNode body) {

        //获取数据查询过滤条件
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.patch(desc, id, body, model, dataPermission);
    }

    /**
     * 标准ORM统计查询
     *
     * @param apiKey
     * @param queryMap
     * @return
     */
    @PostMapping("/api/view/aggre/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'list')")
    public Either<Code, Object> queryAggre(Authentication authentication,
                                           @PathVariable String apiKey,
                                           @RequestParam(required = false) String funcName,
                                           @Validated QueryForm queryMap) {

        funcName = StringUtils.defaultString(funcName, "Aggre");
        //查询条件
        Option<Model> query = pageService.getQuery(apiKey, funcName).toOption();
        //数据返回格式化
        Option<Model> table = pageService.getTable(apiKey, funcName).toOption();
        //查询SQL
        Either<Code, AggreModel> modelE = pageService.getAggre(apiKey, funcName);
        if (modelE.isLeft()) {
            return (Either) modelE;
        }
        //验证参数
        Tuple2<Option<Errors>, Map<String, Object>> errors = queryMap.validateWithScope(query);
        if (errors._1.isDefined()) {
            return Either.left(Code.INVALID_ARGUMENT.withErrors(errors._1));
        }
        AggreModel model = modelE.get();
        if (model.isRawSql()) {
            return dataService.queryRawSql(errors._2(), queryMap.getPageable(), model, table);
        } else {
            Either<String, BeanType<EntityBean>> entity = objectService.getBeanTypeIfPresent(apiKey);
            if (entity.isLeft()) {
                return (Either) entity;
            }
            //获取数据查询过滤条件
            return dataService.queryMapAggre(entity.get(), null, queryMap.getQuery(), queryMap.getPageable(), model);
        }
    }
}
