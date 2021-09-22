package com.he.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * @author hechen
 * @date
 * @return
 * @Description: 核心业务类
 */
public class MybatisBeanDefinitionRegistry extends AbstractBeanDefinitionRegistry implements BeanDefinitionRegistryPostProcessor {
    private Log log = LogFactory.getLog(this.getClass());
    private Map<String, Properties> dataSources = new LinkedHashMap<>();

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        Object onwClass = null;
        loadProperties();
        if(dataSources==null){
            return;
        }
        boolean primary = true;

        for(Map.Entry<String, Properties> entry : dataSources.entrySet()) {
            String name = entry.getKey();
            Properties properties = entry.getValue();
            Object pool = properties.get("pool");
            Map<String, Object> poolMap = JSONObject.parseObject((String) pool, Map.class);
            String type = (String)poolMap.get("type");
            if(type != null){
                try {
                    onwClass = Class.forName(type).newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if(onwClass instanceof PooledDataSource){
                    try {
                        registryBean(beanDefinitionRegistry,primary,name,properties,PooledDataSource.class);
                    } catch (ClassNotFoundException | SQLException | IOException e) {
                        e.printStackTrace();
                    }
                    primary = false;
                }else if(onwClass instanceof DruidDataSource){
                    try {
                        registryBeanWithDruidDataSource(beanDefinitionRegistry,primary,name,properties, DruidDataSource.class);
                    } catch (ClassNotFoundException | SQLException | IOException e) {
                        e.printStackTrace();
                    }
                    primary = false;
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    // DruidDataSource数据源
    private void registryBeanWithDruidDataSource(BeanDefinitionRegistry registry, boolean primary, String keyName, Properties properties,Class<DruidDataSource> druidDataSource) throws ClassNotFoundException, SQLException, IOException {
        Object mybatis = properties.get("mybatis");
        Map<String, Object> mybatisMap = JSONObject.parseObject((String) mybatis, Map.class);
        //创建数据源
        AnnotatedGenericBeanDefinition datasourceBeanDef = new AnnotatedGenericBeanDefinition(druidDataSource);
        ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(datasourceBeanDef);
        datasourceBeanDef.setScope(scopeMetadata.getScopeName());
        Properties properties1 = System.getProperties();
        Properties pro = new Properties();
        pro.putAll(properties1);
        pro.putAll(properties);
        System.setProperties(pro);
//        MutablePropertyValues datasourcePropertyValues = new MutablePropertyValues();
//        datasourcePropertyValues.add("testOnReturn",false);
//        datasourcePropertyValues.add("maxWait",60000);
//        datasourcePropertyValues.add("testWhileIdle",true);
//        datasourcePropertyValues.add("maxPoolPreparedStatementPerConnectionSize",21);
//        datasourcePropertyValues.add("timeBetweenEvictionRunsMillis",60000);
//        datasourcePropertyValues.add("minEvictableIdleTimeMillis",300000);
//        datasourcePropertyValues.add("initialSize",poolMap.get("initial-size"));
//        datasourceBeanDef.setPropertyValues(datasourcePropertyValues);
        // 构造方法设置值
        datasourceBeanDef.getConstructorArgumentValues().addGenericArgumentValue(false);

        datasourceBeanDef.setPrimary(primary);
        AnnotationConfigUtils.processCommonDefinitionAnnotations(datasourceBeanDef);
        BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(datasourceBeanDef, keyName+"DataSource");
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);

        if(mybatis!=null) {
            //创建SqlSessionFactory
            AnnotatedGenericBeanDefinition sqlSessionFactoryBeanDef = new AnnotatedGenericBeanDefinition(SqlSessionFactoryBean.class);
            scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(sqlSessionFactoryBeanDef);
            sqlSessionFactoryBeanDef.setScope(scopeMetadata.getScopeName());
            //属性设值
            MutablePropertyValues sqlSessionPropertyValues = new MutablePropertyValues();
            sqlSessionPropertyValues.add("dataSource",new RuntimeBeanReference(keyName+"DataSource"));
            String mapperLocations = (String)mybatisMap.get("mapper-locations");
            sqlSessionPropertyValues.add("mapperLocations",new PathMatchingResourcePatternResolver().getResources(mapperLocations));
            sqlSessionFactoryBeanDef.setPropertyValues(sqlSessionPropertyValues);
            sqlSessionFactoryBeanDef.setPrimary(primary);
            AnnotationConfigUtils.processCommonDefinitionAnnotations(sqlSessionFactoryBeanDef);
            definitionHolder = new BeanDefinitionHolder(sqlSessionFactoryBeanDef, keyName+"SqlSessionFactory");
            BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);

            //创建DataSourceTransactionManager数据源事务管理
            AnnotatedGenericBeanDefinition transactionManagerBeanDef = new AnnotatedGenericBeanDefinition(DataSourceTransactionManager.class);
            scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(transactionManagerBeanDef);
            transactionManagerBeanDef.setScope(scopeMetadata.getScopeName());
            transactionManagerBeanDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(keyName+"DataSource"));
            transactionManagerBeanDef.setPrimary(primary);
            AnnotationConfigUtils.processCommonDefinitionAnnotations(transactionManagerBeanDef);
            definitionHolder = new BeanDefinitionHolder(transactionManagerBeanDef, keyName+"TransactionManager");
            BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
        }
    }

    // PooledDataSource数据源
    private void registryBean(BeanDefinitionRegistry registry, boolean primary, String keyName, Properties properties,Class<PooledDataSource> pooledDataSource) throws ClassNotFoundException, SQLException, IOException {
        Object pool = properties.get("pool");
        Map<String, Object> poolMap = JSONObject.parseObject((String) pool, Map.class);
        Object mybatis = properties.get("mybatis");
        Map<String, Object> mybatisMap = JSONObject.parseObject((String) mybatis, Map.class);

        if(pool==null){
            pool = new DataSourcePoolProperties();
        }

        // 创建数据源
        AnnotatedGenericBeanDefinition datasourceBeanDef = new AnnotatedGenericBeanDefinition(pooledDataSource);
        ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(datasourceBeanDef);
        datasourceBeanDef.setScope(scopeMetadata.getScopeName());
        //设置构造方法属性值
        datasourceBeanDef.getConstructorArgumentValues().addGenericArgumentValue(properties.get("druid.driverClassName"));
        datasourceBeanDef.getConstructorArgumentValues().addGenericArgumentValue(properties.get("druid.url"));
        datasourceBeanDef.getConstructorArgumentValues().addGenericArgumentValue(properties.get("druid.username"));
        datasourceBeanDef.getConstructorArgumentValues().addGenericArgumentValue(properties.get("druid.password"));
        MutablePropertyValues datasourcePropertyValues = new MutablePropertyValues();
        // 对象内部属性值
        String maximumActive = (String)poolMap.get("maximum-active");
        String maximumIdle = (String)poolMap.get("maximum-idle");
        String maximumCheckoutTime = (String) poolMap.get("maximum-checkout-time");
        datasourcePropertyValues.add("poolMaximumActiveConnections", Integer.valueOf(maximumActive));
        datasourcePropertyValues.add("poolMaximumIdleConnections", Integer.valueOf((maximumIdle)));
        datasourcePropertyValues.add("poolMaximumCheckoutTime", Integer.valueOf((maximumCheckoutTime)));
        datasourceBeanDef.setPropertyValues(datasourcePropertyValues);
        datasourceBeanDef.setPrimary(primary);
        AnnotationConfigUtils.processCommonDefinitionAnnotations(datasourceBeanDef);
        BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(datasourceBeanDef, keyName+"DataSource");
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);

        if(mybatis!=null) {
            //创建SqlSessionFactory
            AnnotatedGenericBeanDefinition sqlSessionFactoryBeanDef = new AnnotatedGenericBeanDefinition(SqlSessionFactoryBean.class);
            scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(sqlSessionFactoryBeanDef);
            sqlSessionFactoryBeanDef.setScope(scopeMetadata.getScopeName());
            MutablePropertyValues sqlSessionPropertyValues = new MutablePropertyValues();
            sqlSessionPropertyValues.add("dataSource",new RuntimeBeanReference(keyName+"DataSource"));
            String mapperLocations = (String)mybatisMap.get("mapper-locations");
            sqlSessionPropertyValues.add("mapperLocations",new PathMatchingResourcePatternResolver().getResources(mapperLocations));
            sqlSessionFactoryBeanDef.setPropertyValues(sqlSessionPropertyValues);
            sqlSessionFactoryBeanDef.setPrimary(primary);
            AnnotationConfigUtils.processCommonDefinitionAnnotations(sqlSessionFactoryBeanDef);
            definitionHolder = new BeanDefinitionHolder(sqlSessionFactoryBeanDef, keyName+"SqlSessionFactory");
            BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);

            //创建SqlSessionTemplate
            AnnotatedGenericBeanDefinition sqlSessionTemplateBeanDef = new AnnotatedGenericBeanDefinition(SqlSessionTemplate.class);
            scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(sqlSessionTemplateBeanDef);
            sqlSessionTemplateBeanDef.setScope(scopeMetadata.getScopeName());
            sqlSessionTemplateBeanDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(keyName+"SqlSessionFactory"));
            sqlSessionTemplateBeanDef.setPrimary(primary);
            AnnotationConfigUtils.processCommonDefinitionAnnotations(sqlSessionTemplateBeanDef);
            definitionHolder = new BeanDefinitionHolder(sqlSessionTemplateBeanDef, keyName+"SqlSessionTemplate");
            BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);

            //创建DataSourceTransactionManager
            AnnotatedGenericBeanDefinition transactionManagerBeanDef = new AnnotatedGenericBeanDefinition(DataSourceTransactionManager.class);
            scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(transactionManagerBeanDef);
            transactionManagerBeanDef.setScope(scopeMetadata.getScopeName());
            transactionManagerBeanDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(keyName+"DataSource"));
            transactionManagerBeanDef.setPrimary(primary);
            AnnotationConfigUtils.processCommonDefinitionAnnotations(transactionManagerBeanDef);
            definitionHolder = new BeanDefinitionHolder(transactionManagerBeanDef, keyName+"TransactionManager");
            BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
        }
    }

    private void loadProperties() {
        String prefix = "platform.datasource";
        // 初始化配置信息到对象的映射
        Map<String, Object> map = null;
        try {
            map = getProperties(prefix, Map.class);
        }
        catch (NoSuchElementException ex){
            log.error("数据库配置信息缺失。prefix=" + prefix);
            return;
        }
//        for (String name : map.keySet()) {
//            DataSourceProperties dataProperties = getProperties(prefix + "." + name,DataSourceProperties.class);
//            dataSources.put(name,dataProperties);
//            dataSourceTemp.put(name,dataProperties);
//        }
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        while(iterator.hasNext()){
            Properties properties = new Properties();
            Map.Entry<String, Object> next = iterator.next();
            String dataSourceName = next.getKey();
            Object value = next.getValue();
            if(value instanceof Map){
                Map<String, Object> valueTemp = (Map) value;
                StringBuffer buffer = new StringBuffer();
                Iterator<Map.Entry<String, Object>> iterator1 = valueTemp.entrySet().iterator();
                while(iterator1.hasNext()){
                    Map.Entry<String, Object> next1 = iterator1.next();
                    String key = next1.getKey();
                    Object value1 = next1.getValue();
                    if(value1 instanceof Map){
                        Map<String, Object> value1Temp = (Map) value1;
                        StringBuffer buffers = new StringBuffer();
                        Iterator<Map.Entry<String, Object>> iterator2 = value1Temp.entrySet().iterator();
                        while(iterator2.hasNext()) {
                            Map.Entry<String, Object> next2 = iterator2.next();
                            String key2 = next2.getKey();
                            Object value2 = next2.getValue();
                            String name = splitProperty(key2);
                            buffers.append("druid.").append(name);
                            properties.setProperty(buffers.toString(), (String)value2);
                            buffers.setLength(0);
                        }
                    }else{
                        String name = splitProperty(key);
                        buffer.append("druid.").append(name);
                        properties.setProperty(buffer.toString(), (String)value1);
                        buffer.setLength(0);
                    }
                }
                Object mybatis = valueTemp.get("mybatis");
                if(mybatis instanceof Map){
                    String mybatisString = JSONObject.toJSONString(mybatis);
                    properties.setProperty("mybatis", mybatisString);
                }
                Object pool = valueTemp.get("pool");
                if(pool instanceof Map){
                    String mybatisString = JSONObject.toJSONString(pool);
                    properties.setProperty("pool", mybatisString);
                }
            }
            dataSources.put(dataSourceName,properties);
        }
    }

    // 拼接key
    private String splitProperty(String name){
        String[] split = name.split("-");
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < split.length; i++) {
            String spl = split[i];
            if(i == 0){
                buffer.append(spl);
                continue;
            }
            String first = spl.substring(0, 1).toUpperCase();
            buffer.append(first).append(spl.substring(1));
        }
        return buffer.toString();
    }
}
