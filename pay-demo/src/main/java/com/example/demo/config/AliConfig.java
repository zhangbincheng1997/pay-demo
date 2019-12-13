package com.example.demo.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliConfig {

    @Autowired
    private PayConfig payConfig;

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                payConfig.getSERVER_URL(),
                payConfig.getAPP_ID(),
                payConfig.getFORMAT(),
                payConfig.getCHARSET(),
                payConfig.getPUBLIC_KEY(),
                payConfig.getSIGNT_TYPE());
    }
}
