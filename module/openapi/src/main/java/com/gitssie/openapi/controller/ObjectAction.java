package com.gitssie.openapi.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.auth.RolePermissionEvaluator;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.form.object.DataTransferReq;
import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.models.user.User;
import com.gitssie.openapi.page.AggreModel;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.service.AuthenticationService;
import com.gitssie.openapi.service.PageService;
import com.gitssie.openapi.service.XObjectService;
import com.gitssie.openapi.web.query.QueryAggre;
import com.gitssie.openapi.web.query.QueryForm;
import com.google.common.collect.Maps;
import io.ebean.Expression;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import io.vavr.control.Either;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Controller
@Transactional
public class ObjectAction {
    @Autowired
    private XObjectService objectService;
    @Autowired
    private PageService pageService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private RolePermissionEvaluator permissionEvaluator;

    /**
     * 数据查询
     *
     * @param apiKey
     * @param pageId
     * @param queryMap
     * @return
     */
    @PostMapping("/api/data/object/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'list')")
    public Either<Code, Page<Map<String, Object>>> query(Authentication authentication,
                                                         @PathVariable String apiKey,
                                                         @RequestParam(required = false) Long pageId,
                                                         @Valid @RequestBody QueryForm queryMap) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Either<Code, Model> modelE = pageService.toModel(apiKey, "List", pageId);
        if (modelE.isLeft()) {
            return (Either) modelE;
        }
        Model model = modelE.get();
        //获取数据查询过滤条件
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.queryMap(entity.get(), model, dataPermission, queryMap.getQuery(), queryMap.getPageable());
    }

    /**
     * 数据查询,只查询指定ID编号的数据
     *
     * @param apiKey
     * @param pageId
     * @param idArray
     * @return
     */
    @PostMapping("/api/data/refresh/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'list')")
    public Either<Code, List<Map<String, Object>>> queryRefresh(Authentication authentication,
                                                                @PathVariable String apiKey,
                                                                @RequestParam(required = false) Long pageId,
                                                                @RequestBody List<String> idArray) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }

        Either<Code, Model> modelE = pageService.toModel(apiKey, "List", pageId);
        if (modelE.isLeft()) {
            return (Either) modelE;
        }
        Model model = modelE.get();
        //获取数据查询过滤条件
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.queryRefreshList(entity.get(), model, dataPermission, idArray);
    }

    /**
     * 聚合查询结果
     *
     * @param apiKey
     * @param pageId
     * @param queryMap
     * @return
     */
    @PostMapping("/api/data/aggre/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'list')")
    public Either<Code, Object> aggreQuery(Authentication authentication,
                                           @PathVariable String apiKey,
                                           @RequestParam(required = false) Long pageId,
                                           @Valid @RequestBody QueryAggre queryMap) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }

        Either<Code, Model> modelE = pageService.toModel(apiKey, "List", pageId);
        if (modelE.isLeft()) {
            return (Either) modelE;
        }
        Model model = modelE.get();
        //获取数据查询过滤条件
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.queryMapAggre(entity.get(), dataPermission, queryMap.getQuery(), Pageable.unpaged(), new AggreModel(queryMap.getAggre()));
    }

    /**
     * 获取详情
     *
     * @param apiKey
     * @param id
     * @param pageId
     * @return
     */
    @GetMapping("/api/data/object/{apiKey}/{id}")
    @PreAuthorize("hasPermission(#apiKey, 'view')")
    public Either<Code, Map<String, Object>> getDetail(Authentication authentication,
                                                       @PathVariable String apiKey,
                                                       @PathVariable String id,
                                                       @RequestParam(required = false) Long pageId) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Either<Code, Model> modelE = pageService.toModel(apiKey, "View", pageId);
        if (modelE.isLeft()) {
            return (Either) modelE;
        }
        Model model = modelE.get();
        //获取数据查询过滤条件
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.fetchDetail(entity.get(), id, model, dataPermission);
    }


    @PostMapping("/api/batch/view/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'view')")
    public Either<Code, List<Map<String, Object>>> batchDetail(Authentication authentication,
                                                               @PathVariable String apiKey,
                                                               @RequestBody List<Long> ids,
                                                               @RequestParam(required = false) Long pageId) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Either<Code, Model> modelE = pageService.toModel(apiKey, "View", pageId);
        if (modelE.isLeft()) {
            return (Either) modelE;
        }
        Model model = modelE.get();
        //获取数据查询过滤条件
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.fetchDetailList(entity.get(), ids, model, dataPermission);
    }

    /**
     * 数据新增
     *
     * @param apiKey
     * @param pageId
     * @param body
     * @return
     */
    @PutMapping("/api/data/object/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'add')")
    public Either<Code, Map<String, Object>> create(Authentication authentication,
                                                    @PathVariable String apiKey,
                                                    @RequestParam(required = false) Long pageId,
                                                    @RequestBody ObjectNode body) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Either<Code, Model> modelE = pageService.toModel(apiKey, "Create", pageId);
        if (modelE.isLeft()) {
            return (Either) modelE;
        }
        Model model = modelE.get();
        //获取数据查询过滤条件
        //Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.create(entity.get(), body, model);
    }

    /**
     * 数据修改
     *
     * @param apiKey
     * @param id
     * @param pageId
     * @param body
     * @return
     */
    @PatchMapping("/api/data/object/{apiKey}/{id}")
    @PreAuthorize("hasPermission(#apiKey, 'edit')")
    public Either<Code, Map<String, Object>> patch(Authentication authentication,
                                                   @PathVariable String apiKey,
                                                   @PathVariable String id,
                                                   @RequestParam(required = false) Long pageId,
                                                   @RequestBody ObjectNode body) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Either<Code, Model> modelE = pageService.toModel(apiKey, "Edit", pageId);
        if (modelE.isLeft()) {
            return (Either) modelE;
        }
        Model model = modelE.get();
        //获取数据查询过滤条件
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.patch(entity.get(), id, body, model, dataPermission);
    }

    /**
     * 修改关联的子属性
     * 权限后置进行判断
     *
     * @param apiKey
     * @param id
     * @param body
     * @return
     */
    @PatchMapping("/api/data/value/{apiKey}/{id}")
    public Either<Code, Map<String, Object>> patchValue(Authentication authentication,
                                                        @PathVariable String apiKey,
                                                        @PathVariable String id,
                                                        @RequestParam String field,
                                                        @RequestBody ObjectNode body) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        BeanType<BasicDomain> desc = entity.get();
        Property property = desc.property(field);
        if (property == null || !(property instanceof BeanPropertyAssocOne)) {
            return Either.left(Code.FAILED_PRECONDITION.withMessage("非关联属性不支持修改"));
        }
        //判断是否有修改当前关联属性的权限
        BeanPropertyAssocOne assocOneP = (BeanPropertyAssocOne) property;
        BeanType<?> assocValueDesc = assocOneP.targetDescriptor();
        //是否有修改关联属性的权限
        boolean grant = permissionEvaluator.hasPermission(authentication, apiKey, "edit") ||
                permissionEvaluator.hasPermission(authentication, assocValueDesc.name(), "edit");
        if (!grant) {
            return Either.left(Code.PERMISSION_DENIED);
        }
        //获取数据查询过滤条件
        Expression dataPermission = authenticationService.filterByRole(authentication).apply(null);
        return objectService.patchValue(desc, assocOneP, id, dataPermission, body);
    }

    /**
     * 数据删除
     *
     * @param apiKey
     * @param id
     * @return
     */
    @DeleteMapping("/api/data/object/{apiKey}/{id}")
    @PreAuthorize("hasPermission(#apiKey, 'delete')")
    public Either<Code, Object> delete(Authentication authentication,
                                       @PathVariable String apiKey,
                                       @PathVariable String id) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Model model = new Model(apiKey);
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.delete(entity.get(), id, model, dataPermission);
    }

    /**
     * 数据批量删除
     *
     * @param apiKey
     * @param ids
     * @return
     */
    @DeleteMapping("/api/batch/object/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'delete')")
    public Either<Code, List<Map<String, Object>>> batchDelete(Authentication authentication,
                                                               @PathVariable String apiKey,
                                                               @RequestBody List<String> ids) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Model model = new Model(apiKey);
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return objectService.batchDelete(entity.get(), ids, model, dataPermission);
    }

    /**
     * 数据批量锁定
     *
     * @param apiKey
     * @param ids
     * @param authentication
     * @return
     */
    @PostMapping("/api/batch/lock/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'lock')")
    public Either<Code, Map<Long, Map<String, Object>>> batchLock(@PathVariable String apiKey, @RequestBody List<Long> ids, Authentication authentication) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Model model = new Model(apiKey);
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        List<Either<Code, BasicDomain>> beans = objectService.lockAll(entity.get(), ids, model, dataPermission);
        Map<Long, Map<String, Object>> result = auditResult(beans);
        return Either.right(result);
    }

    /**
     * 数据批量解锁
     *
     * @param apiKey
     * @param ids
     * @param authentication
     * @return
     */
    @PostMapping("/api/batch/unlock/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'unlock')")
    public Either<Code, Map<Long, Map<String, Object>>> batchUnlock(@PathVariable String apiKey, @RequestBody List<Long> ids, Authentication authentication) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Model model = new Model(apiKey);
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        List<Either<Code, BasicDomain>> beans = objectService.unlockAll(entity.get(), ids, model, dataPermission);
        Map<Long, Map<String, Object>> result = auditResult(beans);
        return Either.right(result);
    }

    /**
     * 数据批量转移
     *
     * @param apiKey
     * @param body           {userId:1000,ids:[2000,3000]}
     * @param authentication
     * @return
     */
    @PostMapping("/api/batch/transfer/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'transfer')")
    public Either<Code, Map<Long, Map<String, Object>>> batchTransfer(@PathVariable String apiKey, @RequestBody DataTransferReq body, Authentication authentication) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Long userId = body.getUserId();
        List<Long> ids = body.getIds();
        Model model = new Model(apiKey);
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        Either<Code, List<BasicDomain>> beansE = objectService.batchTransfer(entity.get(), ids, model, dataPermission, userId);
        return beansE.map(beans -> {
            Map<Long, Map<String, Object>> result = Maps.newHashMapWithExpectedSize(beans.size());
            for (BasicDomain bean : beans) {
                Map<String, Object> userMap = Maps.newHashMapWithExpectedSize(4);
                User user = bean.getOwner();
                userMap.put("id", user.getId());
                userMap.put("name", user.getName());
                result.put(bean.getId(), userMap);
            }
            return result;
        });
    }


    /**
     * 提交审批
     *
     * @param apiKey
     * @param ids
     * @param authentication
     * @return
     */
    @PostMapping("/api/batch/submit/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'apply')")
    public Either<Code, Map<Long, Map<String, Object>>> batchApply(@PathVariable String apiKey, @RequestBody List<Long> ids, Authentication authentication) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        //Authentication to User
        Model model = new Model(apiKey);
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        List<Either<Code, BasicDomain>> beans = objectService.batchSubmit(entity.get(), ids, model, dataPermission, null);
        Map<Long, Map<String, Object>> result = auditResult(beans);
        return Either.right(result);
    }

    private Map<Long, Map<String, Object>> auditResult(List<Either<Code, BasicDomain>> beans) {
        Map<Long, Map<String, Object>> result = Maps.newHashMapWithExpectedSize(beans.size());
        for (Either<Code, BasicDomain> beanE : beans) {
            if (beanE.isRight()) {
                BasicDomain bean = beanE.get();
                Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
                map.put("status", bean.getStatus());
                map.put("lockStatus", bean.isLockStatus());
                result.put(bean.getId(), map);
            } else {
                //log err code
            }
        }
        return result;
    }

    /**
     * 审批通过
     *
     * @param apiKey
     * @param ids
     * @param authentication
     * @return
     */
    @PostMapping("/api/batch/approval/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'approve')")
    public Either<Code, Map<Long, Map<String, Object>>> batchApproval(@PathVariable String apiKey, @RequestBody List<Long> ids, Authentication authentication) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        //Authentication to User
        Model model = new Model(apiKey);
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        List<Either<Code, BasicDomain>> beans = objectService.batchApproval(entity.get(), ids, model, dataPermission, null);
        Map<Long, Map<String, Object>> result = auditResult(beans);
        return Either.right(result);
    }

    /**
     * 反审核
     *
     * @param apiKey
     * @param ids
     * @param authentication
     * @return
     */
    @PostMapping("/api/batch/review/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'review')")
    public Either<Code, Map<Long, Map<String, Object>>> batchReview(@PathVariable String apiKey, @RequestBody List<Long> ids, Authentication authentication) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        //Authentication to User
        Model model = new Model(apiKey);
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        List<Either<Code, BasicDomain>> beans = objectService.batchReview(entity.get(), ids, model, dataPermission, null);
        Map<Long, Map<String, Object>> result = auditResult(beans);
        return Either.right(result);
    }

    /**
     * 简单excel数据导入
     *
     * @param apiKey
     * @param authentication
     * @param file
     * @return
     */
    @PostMapping("/api/import/object/{apiKey}")
    @PreAuthorize("hasPermission(#apiKey, 'import')")
    public Either<String, Page<Map<String, Object>>> importData(@PathVariable String apiKey, Authentication authentication, @RequestParam("file") MultipartFile file) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Model model = new Model(apiKey);
        Expression dataPermission = model.getDataPermissionWhere(authenticationService.filterByRole(authentication));
        return null;
    }
}
