package com.gitssie.openapi.ebean;

import io.vavr.control.Option;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author: Awesome
 * @create: 2024-05-09 10:34
 */
public class TypeQueryRaw<T> implements TypeQuery<T> {
    private Object data;

    public TypeQueryRaw(Object data) {
        this.data = data;
    }

    @Override
    public T findOne() {
        return (T) data;
    }

    @Override
    public Optional<T> findOneOrEmpty() {
        return Optional.ofNullable(findOne());
    }

    @Override
    public Option<T> findOption() {
        return Option.of(findOne());
    }

    @Override
    public Page<T> findPage() {
        return (Page<T>) data;
    }

    @Override
    public List<T> findList() {
        if (data instanceof Page) {
            return (List<T>) ((Page<?>) data).getContent();
        }
        return (List<T>) data;
    }

    @Override
    public void findEach(Consumer<T> consumer) {

    }


}
