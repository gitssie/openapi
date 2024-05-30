package com.gitssie.openapi.ebean;

import com.gitssie.openapi.xentity.XEntityCache;
import com.gitssie.openapi.xentity.XEntityManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.Database;
import io.ebean.bean.BeanCollection;
import io.ebean.bean.EntityBean;
import io.ebean.bean.EntityBeanIntercept;
import io.ebean.plugin.BeanType;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import io.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import io.ebeaninternal.server.deploy.parse.tenant.XEntity;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Deprecated
@Service
public class EbeanConversionService implements Function<Object, Object> {
    private Database database;
    private XEntityManager entityManager;

    private String nameableKey = "label";
    private String nameableRefKey = "-label";

    public EbeanConversionService(Database database, XEntityManager entityManager) {
        this.database = database;
        this.entityManager = entityManager;
    }

    private Object getAssocEntityId(EntityBean bean) {
        if (bean == null) {
            return null;
        }
        return database.beanId(bean);
    }

    public Page<Map<String, Object>> toMap(Page pagedList) {
        List<Map<String, Object>> content = toMap(pagedList.getContent());
        PageImpl result = new PageImpl(content, pagedList.getPageable(), pagedList.getTotalElements());
        return result;
    }

    public List<Map<String, Object>> toMap(Collection dataList) {
        BeanType beanType = null;
        List<Map<String, Object>> content = new LinkedList<>();
        Map<BeanProperty, Set<Object>> assocIdSet = Maps.newHashMap();
        for (Object o : dataList) {
            EntityBean bean = (EntityBean) o;
            if (beanType == null) {
                beanType = database.pluginApi().beanType(bean.getClass());
            }
            content.add(toMap(beanType, bean, assocIdSet, true));
        }
        putAssocNameProperty(content, assocIdSet);
        return content;
    }

    public Map<String, Object> toMap(Object objectBean) {
        EntityBean bean = (EntityBean) objectBean;
        return toMap(bean);
    }

    public Map<String, Object> toMap(EntityBean bean) {
        if (bean == null) {
            return null;
        }
        BeanType beanType = database.pluginApi().beanType(bean.getClass());
        Map<BeanProperty, Set<Object>> assocIdSet = Maps.newHashMap();
        Map<String, Object> result = toMap(beanType, bean, assocIdSet, true);
        List<Map<String, Object>> content = Lists.newLinkedList();
        content.add(result);
        putAssocNameProperty(content, assocIdSet);
        return result;
    }

    private void putAssocNameProperty(List<Map<String, Object>> content, Map<BeanProperty, Set<Object>> assocIdSet) {
        if (assocIdSet.isEmpty()) {
            return;
        }
        List<Map<Object, Object>> beanMaps = new LinkedList<>();
        List<BeanProperty> properties = new LinkedList<>();
        assocIdSet.forEach((property, idSet) -> {
            XEntityCache entity = entityManager.getEntity(property.type());
            if (entity.getNameable() != null && ObjectUtils.isNotEmpty(idSet)) {
                BeanType beanType = database.pluginApi().beanType(entity.getBeanType());
                BeanProperty nameable = (BeanProperty) beanType.property(entity.getNameable().getName());
                Map beanMap = database.createQuery(entity.getBeanType()).select(entity.getNameable().getName())
                        .where().in(beanType.idProperty().name(), idSet).setMapKey(beanType.idProperty().name()).findMap();
                Map objectMap = new HashMap();
                beanMap.forEach((k, v) -> {
                    objectMap.put(k, nameable.value(v));
                });
                beanMaps.add(objectMap);
                properties.add(property);
            }
        });
        BeanProperty property;
        for (Map<String, Object> obj : content) {
            for (int i = 0; i < properties.size(); i++) {
                property = properties.get(i);
                Object idValue = obj.get(property.name());
                Object beanValue = beanMaps.get(i).get(idValue);
                if (beanValue != null) {
                    obj.put(property.name() + nameableRefKey, beanValue);
                }
            }
        }
        /*
        if (entity.getNameable() != null) {
            bean = (EntityBean) database.createQuery(entity.getBeanType()).select(entity.getNameable().getName()).setId(database.beanId(bean)).findOne();
            BeanType beanType = database.pluginApi().beanType(entity.getBeanType());
            BeanProperty nameable = (BeanProperty) beanType.property(entity.getNameable().getName());
            res.put(property.name() + "-label", nameable.value(bean));
        }*/
    }

    private List<Map<String, Object>> toListMap(BeanType beanType, Collection<EntityBean> arr) {
        if (ObjectUtils.isEmpty(arr)) {
            return null;
        }
        List<Map<String, Object>> content = new ArrayList<>();
        Map<BeanProperty, Set<Object>> assocIdSet = new HashMap<>();
        for (EntityBean bean : arr) {
            content.add(toMap(beanType, bean, assocIdSet, true));
        }
        putAssocNameProperty(content, assocIdSet);
        return content;
    }

    private Map<String, Object> toMap(BeanType beanType, EntityBean bean, Map<BeanProperty, Set<Object>> assocIdSet, boolean assocNameable) {
        EntityBeanIntercept intercept = bean._ebean_getIntercept();
        Map<String, Object> res = Maps.newLinkedHashMap();
        for (Object p : beanType.allProperties()) {
            BeanProperty property = (BeanProperty) p;
            if (property.isTransient() && property.isEmbedded()) {
                continue;
            }
            if (property.isCustom() || intercept.isLoadedProperty(property.propertyIndex())) {
                if (property.isAssocProperty()) {
                    if (property instanceof BeanPropertyAssocOne) {
                        Object beanId = getAssocEntityId((EntityBean) property.value(bean));
                        res.put(property.name(), beanId);
                        if (assocNameable && beanId != null) {
                            Set<Object> beanIdSet = assocIdSet.get(property);
                            if (beanIdSet == null) {
                                beanIdSet = new HashSet<>();
                                assocIdSet.put(property, beanIdSet);
                            }
                            beanIdSet.add(beanId);
                        }
                    } else if (property instanceof BeanPropertyAssocMany) {
                        BeanPropertyAssocMany mp = (BeanPropertyAssocMany) property;
                        Collection<EntityBean> list = (Collection<EntityBean>) property.getValue(bean);
                        if (list instanceof BeanCollection) {
                            BeanCollection lazyList = (BeanCollection) list;
                            if (lazyList.isPopulated()) {
                                res.put(property.name(), toListMap(mp.targetDescriptor(), list));
                            }
                        }
                    }
                } else {
                    res.put(property.name(), property.value(bean));
                }
            }
        }
        if (assocNameable) {
            XEntityCache entity = entityManager.getEntity(beanType.type());
            if (entity.getNameable() != null) {
                res.put(nameableKey, res.get(entity.getNameable().getName()));
            }
        }
        return res;
    }

    public Map<String, Object> toJson(EntityBean bean) {
        if (bean == null) {
            return null;
        }
        BeanType beanType = database.pluginApi().beanType(bean.getClass());
        return toJson(beanType, bean);
    }

    private Map<String, Object> toJson(BeanType beanType, EntityBean bean) {
        EntityBeanIntercept intercept = bean._ebean_getIntercept();
        Map<String, Object> res = Maps.newLinkedHashMap();

        for (Object p : beanType.allProperties()) {
            BeanProperty property = (BeanProperty) p;
            if (property.isTransient()) {
                continue;
            }
            if (property.isCustom() || intercept.isLoadedProperty(property.propertyIndex())) {
                if (property.isAssocProperty()) {
                    if (!property.isMany()) {
                        Object beanId = getAssocEntityId((EntityBean) property.value(bean));
                        res.put(property.name(), beanId);
                    }
                } else {
                    res.put(property.name(), property.value(bean));
                }
            }
        }
        return res;
    }

    private <T> BeanType<T> desc(Class<T> beanClass) {
        return database.pluginApi().beanType(beanClass);
    }


    @Override
    public Object apply(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof EntityBean) {
            return toMap((EntityBean) o);
        } else if (o instanceof Collection) {
            return toMap((Collection) o);
        } else {
            return o;
        }
    }
}
