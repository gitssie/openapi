package com.gitssie.openapi.models.user;

import com.gitssie.openapi.ebean.GeneratorType;
import io.ebean.annotation.Index;
import io.ebean.annotation.View;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * @author: Awesome
 * @create: 2024-03-26 10:25
 */
@Entity
@View(name = "user")
@Data
public class UserView extends UserModel {
    @ManyToOne
    @JoinColumn(name = "dim_depart", referencedColumnName = "status")
    protected Department dimDepart;
}
