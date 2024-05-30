package com.gitssie.openapi.ebean;

import io.ebean.SqlQuery;
import io.vavr.control.Option;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * @author: Awesome
 * @create: 2024-05-09 10:33
 */
public interface TypeQuery<T> extends SqlQuery.TypeQuery<T> {
    Option<T> findOption();

    Page<T> findPage();
}
