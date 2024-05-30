package com.gitssie.openapi.models.auth;

import com.gitssie.openapi.models.user.User;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.bean.BeanLoader;
import io.ebean.bean.EntityBean;
import io.ebean.bean.EntityBeanIntercept;
import io.ebean.bean.InterceptReadWrite;
import io.vavr.Lazy;
import io.vavr.Value;
import io.vavr.collection.Iterator;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author: Awesome
 * @create: 2024-03-08 10:13
 */
public class LazyUser implements InternalUser, Value<User> {
    private static final long serialVersionUID = 4567742078336690026L;
    private long userId;
    private String username;
    private Long tenantId;
    private transient String ebeanServerName;
    private transient User user;

    public LazyUser(String dbName, User user) {
        this.ebeanServerName = dbName;
        this.user = user;
        this.userId = user.getId();
        this.username = user.getUsername();
        this.tenantId = user.getTenantId();
    }

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public Long getTenantId() {
        return tenantId;
    }


    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public User get() {
        return user;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public boolean isSingleValued() {
        return false;
    }

    @Override
    public Value peek(Consumer action) {
        return null;
    }

    @Override
    public String stringPrefix() {
        return null;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public Value map(Function mapper) {
        return null;
    }


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(userId);
        out.writeUTF(username);
        out.writeUTF(ebeanServerName);
        out.writeObject(tenantId);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.userId = in.readLong();
        this.username = in.readUTF();
        this.ebeanServerName = in.readUTF();
        this.tenantId = (Long) in.readObject();

        this.user = new User();
        this.user.setId(this.userId);
        this.user.setUsername(username);
        if (tenantId != null) {
            this.user.setTenantId(tenantId);
        }

        EntityBean bean = (EntityBean) this.user;
        InterceptReadWrite intercept = (InterceptReadWrite) bean._ebean_getIntercept();
        intercept.setReference(-1);
        intercept.setBeanLoader(new ProxyBeanLoader(ebeanServerName));
    }

    private static class ProxyBeanLoader implements BeanLoader {
        private String ebeanServerName;
        private Lazy<BeanLoader> lazy;

        public ProxyBeanLoader(String name) {
            this.ebeanServerName = name;
            this.lazy = Lazy.of(() -> {
                final Database database = DB.byName(ebeanServerName);
                if (database == null) {
                    throw new PersistenceException("Database [" + ebeanServerName + "] was not found?");
                }
                return database.pluginApi().beanLoader();
            });
        }

        @Override
        public String getName() {
            return ebeanServerName;
        }

        @Override
        public void loadBean(EntityBeanIntercept ebi) {
            lazy.get().loadBean(ebi);
        }

        @Override
        public Lock lock() {
            return lazy.get().lock();
        }
    }
}
