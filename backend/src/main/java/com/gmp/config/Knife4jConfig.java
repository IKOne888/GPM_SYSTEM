package com.gmp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Knife4j接口文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public Docket createRestApi() {
        // springfox 2.10.5 仅支持 SWAGGER_2 规范
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.gmp.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("GMP内部培训系统API文档")
                .description("药品生产企业GMP内部培训系统接口文档")
                .contact(new Contact("GMP Training", "", ""))
                .version("1.0.0")
                .build();
    }
}


