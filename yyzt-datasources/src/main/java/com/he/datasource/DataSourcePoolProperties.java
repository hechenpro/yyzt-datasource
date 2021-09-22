package com.he.datasource;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author hechen
 * @date
 * @return 使用流映射时需要使用
 * @Description:
 */
public class DataSourcePoolProperties {
    @Value("${driver-class-name}")
    private String driverClassName;

    @Value("${url}")
    private String url;

    @Value("${username}")
    private String username;

    @Value("${password}")
    private String password;

    @Value("${pool}")
    private DataSourcePoolProperties pool;

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public DataSourcePoolProperties getPool() {
        return pool;
    }

    public void setPool(DataSourcePoolProperties pool) {
        this.pool = pool;
    }
}
