package com.personalworkspace.taskservice.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/** Giữ JSON pagination là public contract ổn định, không phụ thuộc cấu trúc PageImpl nội bộ. */
@Configuration(proxyBeanMethods = false)
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfiguration {}
