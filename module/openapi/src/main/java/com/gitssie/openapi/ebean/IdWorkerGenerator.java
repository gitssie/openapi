package com.gitssie.openapi.ebean;

import com.gitssie.openapi.data.IdWorker;
import io.ebean.config.IdGenerator;

public class IdWorkerGenerator implements IdGenerator {
    private final String name;
    private final IdWorker idWorker;

    public IdWorkerGenerator(String name, long workerId) {
        this.name = name;
        this.idWorker = new IdWorker(workerId);
    }

    @Override
    public Object nextValue() {
        return idWorker.nextId();
    }

    @Override
    public String getName() {
        return name;
    }
}
