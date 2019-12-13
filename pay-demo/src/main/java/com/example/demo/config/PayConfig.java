package com.example.demo.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
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
    private String SIGNT_TYPE = "RSA2";
}
