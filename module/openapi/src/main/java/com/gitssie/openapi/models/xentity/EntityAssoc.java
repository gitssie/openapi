package com.gitssie.openapi.models.xentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gitssie.openapi.ebean.GeneratorType;
import io.ebean.bean.StaticEntity;

import javax.persistence.*;

@Entity
@Table(name = "entity_assoc")
public class EntityAssoc implements StaticEntity {
    @Id
    @GeneratedValue(generator = GeneratorType.IdWorker)
    private Long id;//                 BIGINT            PRIMARY KEY  AUTO_INCREMENT,
    @ManyToOne
    @JsonIgnore
    private EntityMapping entity;//          BIGINT            NOT NULL COMMENT '联方ID',
    @ManyToOne
    @JsonIgnore
    private EntityMapping refer;//           BIGINT            NOT NULL COMMENT '被关联方ID',
    @OneToOne
    @JsonIgnore
    private EntityField field;//           BIGINT            NOT NULL COMMENT '属性ID',

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EntityMapping getEntity() {
        return entity;
    }

    public void setEntity(EntityMapping entity) {
        this.entity = entity;
    }

    public EntityMapping getRefer() {
        return refer;
    }

    public void setRefer(EntityMapping refer) {
        this.refer = refer;
    }

    public EntityField getField() {
        return field;
    }

    public void setField(EntityField field) {
        this.field = field;
    }

    public String getReferName(){
        return getRefer().getName();
    }

    public String getReferLabel(){
        return getRefer().getLabel();
    }
}
