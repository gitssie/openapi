package com.gitssie.openapi.ebean;

import io.ebean.PagedList;

import java.util.List;
import java.util.concurrent.Future;

public class PagedListImpl<T> implements PagedList<T> {
    private List<T> list;

    public PagedListImpl(List<T> list) {
        this.list = list;
    }

    @Override
    public void loadCount() {

    }

    @Override
    public Future<Integer> getFutureCount() {
        return null;
    }

    @Override
    public List getList() {
        return list;
    }

    @Override
    public int getTotalCount() {
        return list.size();
    }

    @Override
    public int getTotalPageCount() {
        return 0;
    }

    @Override
    public int getPageSize() {
        return 0;
    }

    @Override
    public int getPageIndex() {
        return 0;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrev() {
        return false;
    }

    @Override
    public String getDisplayXtoYofZ(String to, String of) {
        return "";
    }
}
