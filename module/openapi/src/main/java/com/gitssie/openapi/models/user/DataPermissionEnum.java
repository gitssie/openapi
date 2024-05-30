package com.gitssie.openapi.models.user;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DataPermissionEnum {
    SELF,SELF_STAFF,DEPART,DEPART_STAFF,ALL;
    //[{"label":"本人","value":0},{"label":"本人及下属","value":1},{"label":"本部门","value":2},{"label":"本部门及下级部门","value":3},{"label":"全部","value":4}]

    @JsonValue
    public int value(){
        return this.ordinal();
    }
}
