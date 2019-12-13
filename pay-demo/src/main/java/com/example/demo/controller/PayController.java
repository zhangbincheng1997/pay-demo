package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.example.demo.config.PayConfig;
import com.example.demo.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@Controller
public class PayController {

    @Autowired
    private PayConfig payConfig;

    @Autowired
    private AlipayClient alipayClient;

    @GetMapping(value = "/test")
    @ResponseBody
    public String test() {
        return JSON.toJSONString(alipayClient);
    }

    /**
     * 登录 https://docs.open.alipay.com/284
     */
    @RequestMapping(value = "/login")
    public void login(HttpServletResponse response) {
        try {
            // 第一步：URL 拼接
            String url = "https://openauth.alipaydev.com/oauth2/publicAppAuthorize.htm?app_id=" + payConfig.getAPP_ID()
                    + "&scope=auth_user,auth_base&redirect_uri=" + payConfig.getAUTH_URL();
            // 第二步：获取 auth_code
            response.sendRedirect(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 授权 https://docs.open.alipay.com/284
     */
    @RequestMapping(value = "/auth")
    @ResponseBody
    public String auth(HttpServletRequest request) {
        // 第二步：获取 auth_code
        Map<String, String> map = Util.requestToMap(request);
        log.info(JSON.toJSONString(map));
        try {
            // 第三步：换取 access_token
            AlipaySystemOauthTokenRequest tokenRequest = new AlipaySystemOauthTokenRequest();
            tokenRequest.setGrantType("authorization_code");
            tokenRequest.setCode(map.get("auth_code"));
            AlipaySystemOauthTokenResponse tokenResponse = alipayClient.execute(tokenRequest);
            String accessToken = tokenResponse.getAccessToken();

            // 第四步：获取用户信息
            AlipayUserInfoShareRequest shareRequest = new AlipayUserInfoShareRequest();
            AlipayUserInfoShareResponse shareResponse = alipayClient.execute(shareRequest, accessToken);
            return shareResponse.getBody();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "出错啦......";
    }

    /**
     * PC端支付 https://docs.open.alipay.com/270
     */
    @RequestMapping(value = "/pay")
    @ResponseBody
    public void pay(HttpServletResponse response) throws IOException {
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(payConfig.getRETURN_URL()); // 需要设置
        alipayRequest.setNotifyUrl(payConfig.getNOTIFY_URL()); // 需要设置

        AlipayTradePayModel model = new AlipayTradePayModel();
        model.setOutTradeNo(String.valueOf(new Random().nextInt(1000000000)));
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        model.setTotalAmount("77.77");
        model.setSubject("PC支付测试");
        model.setBody("支付宝PC支付测试");
        alipayRequest.setBizModel(model);

//        Map<String, String> biz = new HashMap<>();
//        biz.put("out_trade_no", String.valueOf(new Random().nextInt(1000000000)));
//        biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
//        biz.put("total_amount", "77.77");
//        biz.put("subject", "PC支付测试");
//        biz.put("body", "支付宝PC支付测试");
//        alipayRequest.setBizContent(JSON.toJSONString(biz));

        try {
            String form = alipayClient.pageExecute(alipayRequest).getBody();
            log.info(form);
            response.setContentType("text/html;charset=" + payConfig.getCHARSET());
            response.getWriter().write(form); //直接将完整的表单html输出到页面
            response.getWriter().flush();
            response.getWriter().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getBizContent(String outTradeNo, String tradeNo) {
        Map<String, String> biz = new HashMap<>();
        if (!StringUtils.isEmpty(outTradeNo)) {
            biz.put("out_trade_no", outTradeNo);
        }
        if (!StringUtils.isEmpty(tradeNo)) {
            biz.put("trade_no", tradeNo);
        }
        return JSON.toJSONString(biz);
    }

    /**
     * 查询订单
     */
    @RequestMapping(value = "/query")
    @ResponseBody
    public String query(@RequestParam(required = false, name = "outTradeNo") String outTradeNo,
                        @RequestParam(required = false, name = "tradeNo") String tradeNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            String bizContent = getBizContent(outTradeNo, tradeNo);
            request.setBizContent(bizContent);
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getBody();
            } else {
                return "出错啦......";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 关闭订单
     */
    @RequestMapping(value = "/close")
    @ResponseBody
    public String close(@RequestParam(required = false, name = "outTradeNo") String outTradeNo,
                        @RequestParam(required = false, name = "tradeNo") String tradeNo) {
        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            String bizContent = getBizContent(outTradeNo, tradeNo);
            request.setBizContent(bizContent);
            AlipayTradeCloseResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getBody();
            } else {
                return "出错啦......";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 退款
     */
    @RequestMapping(value = "/refund")
    @ResponseBody
    public String refund(@RequestParam(required = false, name = "outTradeNo") String outTradeNo,
                         @RequestParam(required = false, name = "tradeNo") String tradeNo) {
        try {
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            String bizContent = getBizContent(outTradeNo, tradeNo);
            request.setBizContent(bizContent);
            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getBody();
            } else {
                return "出错啦......";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * charset=UTF-8
     * &out_trade_no=
     * &method=alipay.trade.page.pay.return
     * &total_amount=
     * &sign=
     * &trade_no=
     * &auth_app_id=2016101500696043
     * &version=1.0
     * &app_id=2016101500696043
     * &sign_type=RSA2
     * &seller_id=
     * &timestamp=
     */
    @RequestMapping(value = "/return")
    @ResponseBody
    public String Return(HttpServletRequest request) {
        try {
            // 获取支付宝GET过来的反馈信息
            Map<String, String> map = Util.requestToMap(request);
            log.info(JSON.toJSONString(map));

            boolean verifyResult = AlipaySignature.rsaCheckV1(map,
                    payConfig.getPUBLIC_KEY(), payConfig.getCHARSET(), payConfig.getSIGNT_TYPE());
            if (verifyResult) {
                System.out.println("return_url 验证成功");
                return "success";
            } else {
                System.out.println("return_url 验证失败");
                return "failure";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "failure";
    }

    @RequestMapping(value = "/notify")
    @ResponseBody
    public String Notify(HttpServletRequest request) {
        try {
            // 获取支付宝POST过来的反馈信息
            Map<String, String> map = Util.requestToMap(request);
            log.info(JSON.toJSONString(map));

            boolean verifyResult = AlipaySignature.rsaCheckV1(map,
                    payConfig.getPUBLIC_KEY(), payConfig.getCHARSET(), payConfig.getSIGNT_TYPE());
            if (verifyResult) {
                System.out.println("notify_url 验证成功");
                return "success";
            } else {
                System.out.println("notify_url 验证失败");
                return "failure";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "failure";
    }
}
