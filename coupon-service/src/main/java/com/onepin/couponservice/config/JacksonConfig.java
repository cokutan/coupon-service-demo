package com.onepin.couponservice.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.cglib.proxy.Mixin;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.hibernate7.Hibernate7Module;

public class JacksonConfig {
    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultVisibility(visibilityChecker -> visibilityChecker
                        .withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                        .withVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                        .withVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
                        .withVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
                )
                .addModule(new Hibernate7Module())
                .addMixIn(ProblemDetail.class, Mixin.class)
                .build();
        return mapper;
    }
}
