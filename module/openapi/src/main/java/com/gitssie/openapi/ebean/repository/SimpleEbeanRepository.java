package com.gitssie.openapi.ebean.repository;

import com.google.common.collect.Lists;
import io.ebean.Database;
import io.ebean.PagedList;
import io.ebean.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

@Transactional(readOnly = true)
public class SimpleEbeanRepository<T, ID> implements PagingAndSortingRepository<T, ID> {
    private Database database;
    private Class<T> beanClass;

    public SimpleEbeanRepository(Database database, Class<T> beanClass) {
        Assert.notNull(database, "Ebean Database must not be null.");
        Assert.notNull(beanClass, "Bean Class must not be null.");
        this.database = database;
        this.beanClass = beanClass;
    }

    @Override
    public Iterable<T> findAll(Sort sort) {
        Query<T> query = database.createQuery(beanClass);
        if (sort != null && sort.isSorted()) {
            for (Sort.Order e : sort) {
                if (e.isDescending()) {
                    query.orderBy().desc(e.getProperty());
                } else {
                    query.orderBy().asc(e.getProperty());
                }
            }
        }
        return query.findList();
    }

    @Override
    public Page<T> findAll(Pageable page) {
        Query<T> query = database.createQuery(beanClass);
        Sort sort = page.getSort();
        if (sort != null && sort.isSorted()) {
            for (Sort.Order e : sort) {
                if (e.isDescending()) {
                    query.orderBy().desc(e.getProperty());
                } else {
                    query.orderBy().asc(e.getProperty());
                }
            }
        }
        query.setFirstRow((int) page.getOffset()).setMaxRows(page.getPageSize());
        PagedList<T> list = query.findPagedList();
        return new PageImpl<>(list.getList(), page, list.getTotalCount());
    }

    @Transactional
    @Override
    public <S extends T> S save(S entity) {
        database.save(entity);
        return entity;
    }

    @Transactional
    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        database.saveAll(entities);
        return entities;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(database.find(beanClass, id));
    }

    @Override
    public boolean existsById(ID id) {
        return database.createQuery(beanClass).setId(id).exists();
    }

    @Override
    public Iterable<T> findAll() {
        return database.createQuery(beanClass).findList();
    }

    @Override
    public Iterable<T> findAllById(Iterable<ID> ids) {
        return database.createQuery(beanClass).setId(ids).findList();
    }

    @Override
    public long count() {
        return database.createQuery(beanClass).findCount();
    }

    @Transactional
    @Override
    public void deleteById(ID id) {
        database.delete(beanClass, id);
    }

    @Transactional
    @Override
    public void delete(T entity) {
        database.delete(entity);
    }

    @Transactional
    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        database.deleteAll(beanClass, Lists.newArrayList(ids));
    }

    @Transactional
    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        database.deleteAll(Lists.newArrayList(entities));
    }

    @Override
    public void deleteAll() {

    }
}
