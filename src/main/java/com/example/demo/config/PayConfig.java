package com.example.demo.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class PayConfig {

    @Value("${alipay.appId}")
    private String APP_ID;

    @Value("${alipay.privateKey}")
    private String PRIVATE_KEY;

    @Value("${alipay.publicKey}")
    private String PUBLIC_KEY;

    @Value("${alipay.serverUrl}")
    private String SERVER_URL;

    @Value("${alipay.authUrl}")
    private String AUTH_URL;

    @Value("${alipay.notifyUrl}")
    private String NOTIFY_URL;

    @Value("${alipay.returnUrl}")
    private String RETURN_URL;

    private String FORMAT = "json";
    private String CHARSET = "UTF-8";
    private String SIGN_TYPE = "RSA2";

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(SERVER_URL, APP_ID, PRIVATE_KEY, FORMAT, CHARSET, PUBLIC_KEY, SIGN_TYPE);
    }
}
