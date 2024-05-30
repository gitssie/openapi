package com.gitssie.openapi.page;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.ebean.EbeanApi;
import com.gitssie.openapi.service.Provider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.Database;
import io.ebean.ExpressionFactory;
import io.ebean.ExpressionList;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.expression.DefaultExpressionFactory;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.ObjectUtils;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service("ebeanApi")
public class EbeanApiResolver extends DefaultExpressionFactory implements LazyValueResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(EbeanApiResolver.class);
    private final EbeanApi ebeanApi;
    private final ModelConverter modelConverter;
    private final Provider provider;

    public EbeanApiResolver(EbeanApi ebeanApi, Database db, ModelConverter modelConverter, Provider provider) {
        super(db.pluginApi().config().isExpressionEqualsWithNullAsNoop(), db.pluginApi().config().isExpressionNativeIlike() && db.pluginApi().databasePlatform().isSupportsNativeIlike());
        this.ebeanApi = ebeanApi;
        this.modelConverter = modelConverter;
        this.provider = provider;
    }

    public ExpressionFactory expr() {
        return this;
    }

    public ExpressionList where() {
        return ebeanApi.where();
    }

    public ExpressionList or() {
        return ebeanApi.or();
    }

    public void copyMap(Map target, Object src, String... properties) {
        if (target == null || src == null || properties == null || properties.length == 0) {
            return;
        }
        if (src instanceof EntityBean) {
            EntityBean bean = (EntityBean) src;
            BeanType desc = provider.desc(src.getClass());
            for (String name : properties) {
                BeanProperty property = (BeanProperty) desc.property(name);
                if (property == null || property.isAssocProperty()) {
                    continue;
                }
                Object value = property.getValueIntercept(bean);
                target.put(name, value);
            }
        } else if (src instanceof Map) {
            Map map = (Map) src;
            for (String property : properties) {
                target.put(property, map.get(property));
            }
        }
    }

    public Object toJSON(Object... args) {
        if (ObjectUtils.isEmpty(args)) {
            return Undefined.instance;
        } else if (args[0] == null) {
            return Undefined.instance;
        }
        Object data = args[0];
        if (ObjectUtils.isEmpty(data)) {
            return data;
        }
        Object config = null;
        if (args.length > 1) {
            config = args[1];
        }
        Option<Model> modelOpt = Option.none();
        if (config instanceof Map) {
            modelOpt = PageView.toModel((Map) config);
        } else if (config instanceof List) {
            modelOpt = PageView.toModel((List) config);
        }
        Model model = modelOpt.getOrElse((Model) null);
        if (data instanceof Collection) {
            Collection dataList = (Collection) data;
            boolean isMap = false;
            Class<EntityBean> entityClass = null;
            for (Object bean : dataList) {
                if (bean instanceof Map) {
                    isMap = true;
                } else if (bean instanceof EntityBean) {
                    entityClass = (Class<EntityBean>) bean.getClass();
                }
                break;
            }
            if (isMap) {
                FetchContext context = newContext();
                try (context) {
                    return modelConverter.toJSONMap(context, model, (Collection) data);
                }
            } else if (entityClass != null) {
                FetchContext context = newContext();
                try (context) {
                    return modelConverter.toJSON(context, model, entityClass, (Collection) data);
                }
            } else {
                return data;
            }
        } else if (data instanceof EntityBean) {
            FetchContext context = newContext();
            try (context) {
                return modelConverter.toJSON(context, model, (EntityBean) data);
            }
        } else if (data instanceof Map) {
            FetchContext context = newContext();
            try (context) {
                return modelConverter.toJSONMap(context, model, (Map<String, Object>) data);
            }
        } else {
            return data;
        }
    }

    private FetchContext newContext() {
        FetchContext context = new FetchContextMap(provider);
        context.setLazyFetch(true);//enable lazy fetch
        return context;
    }

    public Object sqlOne(Object... args) {
        return sqlQuery("map", args);
    }

    public Object sqlList(Object... args) {
        return sqlQuery("list", args);
    }

    public Object sqlQuery(String type, Object[] args) {
        if (ObjectUtils.isEmpty(args)) {
            return Undefined.instance;
        } else if (args.length < 2) {
            LOGGER.warn("execute raw sql,scope,rawSql parameter is undefined");
            return Undefined.instance;
        }
        NativeObject scope;
        Function rawSql = null;
        Object config = null;

        if (args[0] instanceof NativeObject) {
            scope = (NativeObject) args[0];
        } else {
            scope = new NativeObject();
        }

        if (args[0] instanceof Function) {
            rawSql = (Function) args[0];
        } else if (args[1] instanceof Function) {
            rawSql = (Function) args[1];
        }

        if (rawSql == null) {
            LOGGER.warn("execute raw sql,rawSql parameter is undefined");
            return Undefined.instance;
        }

        if (!(args[1] instanceof Function)) {
            config = args[1];
        } else if (args.length > 2) {
            config = args[2];
        }

        Option<Model> table = Option.none();
        if (config instanceof Map) {
            table = PageView.toModel((Map) config);
        } else if (config instanceof List) {
            table = PageView.toModel((List) config);
        }

        return sqlQuery(scope, rawSql, type, table);
    }

    public Object sqlQuery(NativeObject scope, Function rawSql, String type, Option<Model> table) {
        AggreModel aggre = new AggreModel(Collections.EMPTY_LIST);
        aggre.setType(type);
        aggre.setRawSql(rawSql);

        FetchContext context = newContext();
        try (context) {
            Either<Code, Object> result = ebeanApi.queryRawSql(context, scope, Pageable.unpaged(), aggre, table);
            if (result.isLeft()) {
                LOGGER.warn("execute raw sql:{},error:{},return undefined", rawSql, result.getLeft());
                return Undefined.instance;
            } else {
                return result.get();
            }
        }
    }

    public Map<String, Object> listToMap(List<Map> list, String key) {
        if (ObjectUtils.isEmpty(list)) {
            return Maps.newHashMapWithExpectedSize(0);
        } else {
            Map result = Maps.newHashMapWithExpectedSize(list.size());
            for (Map row : list) {
                result.put(row.get(key), row);
            }
            return result;
        }
    }


    public Object beanId(Object obj) {
        if (ObjectUtils.isEmpty(obj)) {
            return null;
        }
        if (obj instanceof EntityBean) {
            return provider.beanId(obj);
        } else if (obj instanceof List) {
            List list = (List) obj;
            obj = list.get(0);
            if (obj instanceof EntityBean) {
                List res = Lists.newArrayListWithExpectedSize(list.size());
                BeanType<?> desc = provider.desc(obj.getClass());
                Property prop = desc.idProperty();
                for (Object item : list) {
                    res.add(prop.value(item));
                }
            } else {
                return null;
            }
        }
        return null;
    }
}
