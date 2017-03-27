package com.catfish.config;

import com.catfish.spring.support.JsonRequestMethodArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by A on 2017/3/27.
 */
@Configuration
public class MyConfig extends WebMvcConfigurationSupport {

    @Override
    protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        List list = new ArrayList();
        list.add(new MappingJackson2HttpMessageConverter());
        argumentResolvers.add(new JsonRequestMethodArgumentResolver(list));
        super.addArgumentResolvers(argumentResolvers);
    }
}
