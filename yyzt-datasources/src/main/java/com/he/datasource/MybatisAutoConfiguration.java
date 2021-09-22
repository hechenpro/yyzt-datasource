package com.he.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hechen
 * @date
 * @return
 * @Description: 注入到spring中
 */
@Configuration
public class MybatisAutoConfiguration {

    @Bean
    public MybatisBeanDefinitionRegistry mybatisBeanDefinitionRegistry() {
        return new MybatisBeanDefinitionRegistry();
    }
}
