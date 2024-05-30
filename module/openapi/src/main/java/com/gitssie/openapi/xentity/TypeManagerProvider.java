package com.gitssie.openapi.xentity;

import com.gitssie.openapi.page.AssocType;
import com.gitssie.openapi.utils.Json;
import com.gitssie.openapi.xentity.gen.CodeGenerated;
import io.ebean.annotation.MutationDetection;
import io.ebean.config.DatabaseConfig;
import io.ebean.core.type.ScalarType;
import io.ebeaninternal.server.deploy.generatedproperty.GeneratedProperty;
import io.ebeaninternal.server.deploy.meta.DeployBeanProperty;
import io.ebeaninternal.server.deploy.parse.tenant.XField;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XAnnotations;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XDbJson;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XGeneratedValue;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XGenericType;
import io.ebeaninternal.server.type.DefaultTypeManager;
import io.ebeaninternal.server.type.GeoTypeBinder;
import io.ebeaninternal.server.type.TypeManager;
import io.vavr.control.Either;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.persistence.EnumType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TypeManagerProvider implements ApplicationContextAware, TypeManager {
    private final String reference = "Ref";
    private ApplicationContext context;
    private volatile TypeManager typeManager;
    private final ConcurrentHashMap<String, ScalarType<?>> logicalMap = new ConcurrentHashMap<>();
    private XAnnotations annotations = new XAnnotations();
    private Map<String, GeneratedProperty> generatedPropertyMap = new HashMap<>();

    private List<Map> listMap;
    private Set<Map> setMap;
    private List<Integer> listInteger;
    private List<Long> listLong;
    private Map<String, Object> map;
    private Set<Object> set;

    public TypeManagerProvider() {
        initLogicalMap();
        initGeneratedProperty();
    }

    private void initLogicalMap() {
        addJsonMap("List<Map>", "listMap");
        addJsonMap("Set<Map>", "setMap");
        addJsonMap("Integer[]", "listInteger");
        addJsonMap("Long[]", "listLong");
        addJsonMap("Map", "map");
        addJsonMap("Set", "set");
    }

    private void initGeneratedProperty() {
        generatedPropertyMap.put(CodeGenerated.class.getSimpleName(), new CodeGenerated());
    }

    private void addJsonMap(String name, String fieldName) {
        try {
            Field field = getClass().getDeclaredField(fieldName);
            logicalMap.put(name, new JsonScalarType(field.getType(), field.getGenericType()));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    private TypeManager getTypeManager() {
        if (typeManager == null) {
            synchronized (this) {
                if (typeManager == null) {
                    DatabaseConfig databaseConfig = context.getBean(DatabaseConfig.class);
                    typeManager = new DefaultTypeManager(databaseConfig, null);
                    //typeManager.getScalarType()
                }
            }
        }
        return typeManager;
    }

    @Override
    public void add(ScalarType<?> scalarType) {
        getTypeManager().add(scalarType);
    }

    public ScalarType<?> getScalarType(String cast) {
        ScalarType<?> type = getTypeManager().getScalarType(cast);
        if (type == null) {
            type = logicalMap.get(cast);
        }
        return type;
    }

    public boolean isJson(String cast) {
        return logicalMap.containsKey(cast);
    }

    public void handlerScalarType(ScalarType type, XField field) {
        field.setType(type.getType());
        if (type instanceof JsonScalarType) {
            XDbJson dbJson = new XDbJson();
            dbJson.setMutationDetection(MutationDetection.NONE);
            field.addAnnotation(dbJson);
            field.addAnnotation(new XGenericType(((JsonScalarType) type).getGenericType()));
        }
    }

    public ScalarType<?> getScalarType(int jdbcType) {
        return getTypeManager().getScalarType(jdbcType);
    }

    /**
     * Return the ScalarType for a given logical type.
     */
    public ScalarType<?> getScalarType(Class<?> type) {
        return getTypeManager().getScalarType(type);
    }

    @Override
    public ScalarType<?> getScalarType(Class<?> type, int jdbcType) {
        return getTypeManager().getScalarType(type, jdbcType);
    }

    @Override
    public ScalarType<?> getScalarType(Type propertyType, Class<?> type) {
        return getTypeManager().getScalarType(propertyType, type);
    }

    @Override
    public ScalarType<?> createEnumScalarType(Class<? extends Enum<?>> enumType, EnumType enumerated) {
        return getTypeManager().createEnumScalarType(enumType, enumerated);
    }

    @Override
    public ScalarType<?> getJsonScalarType(DeployBeanProperty prop, int dbType, int dbLength) {
        return getTypeManager().getJsonScalarType(prop, dbType, dbLength);
    }

    @Override
    public ScalarType<?> getArrayScalarType(Class<?> type, Type genericType, boolean nullable) {
        return getTypeManager().getArrayScalarType(type, genericType, nullable);
    }

    @Override
    public ScalarType<?> getDbMapScalarType() {
        return getTypeManager().getDbMapScalarType();
    }

    @Override
    public GeoTypeBinder getGeoTypeBinder() {
        return getTypeManager().getGeoTypeBinder();
    }

    public Either<String, Annotation> createAnnotation(Map<String, Object> map) {
        return createAnnotation(map, false);
    }

    public Either<String, Annotation> createEntityAnnotation(Map<String, Object> map) {
        return createAnnotation(map, true);
    }

    private Either<String, Annotation> createAnnotation(Map<String, Object> map, boolean isEntity) {
        String name = String.valueOf(map.get("@"));
        return createAnnotation(name, map, isEntity);
    }

    private Either<String, Annotation> createAnnotation(String name, Map<String, Object> map, boolean isEntity) {
        Class<?> clazz = isEntity ? annotations.getEntityAnnotationClass(name) : annotations.getClass(name);
        if (clazz == null) {
            return Either.left("不支持的标注类型:" + name);
        }
        Annotation anno = (Annotation) Json.fromJson(Json.toJson(map), clazz);
        initAnnotation(anno, map);
        return Either.right(anno);
    }

    private void initAnnotation(Annotation anno, Map<String, Object> map) {
        if (anno instanceof XGeneratedValue) {
            XGeneratedValue gen = (XGeneratedValue) anno;
            GeneratedProperty gep = generatedPropertyMap.get(gen.generator());
            if (gep != null) {
                gen.setGeneratedProperty(gep);
            }
        }
    }

    public boolean isReference(String type) {
        if (StringUtils.endsWith(type, reference)) {
            return XAssocType.valueOf(type) != null;
        }
        return false;
    }

    public XAssocType toAssocType(String type) {
        XAssocType assocType = XAssocType.valueOf(type);
        return assocType;
    }

}
