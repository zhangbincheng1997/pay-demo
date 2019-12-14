package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.example.demo.config.PayConfig;
import com.example.demo.utils.PayUtils;
import com.example.demo.utils.SnowFlake;
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
import java.util.*;

@Slf4j
@Controller
public class PayController {

    @Autowired
    private PayConfig payConfig;

    @Autowired
    private AlipayClient alipayClient;

    private SnowFlake snowFlake = new SnowFlake(2, 3);

    @GetMapping(value = "/test")
    @ResponseBody
    public String test() {
        return JSON.toJSONString(alipayClient);
    }

    /**
     * 登录 https://docs.open.alipay.com/284
     */
    @RequestMapping(value = "/login")
    public void login(HttpServletResponse response) throws Exception {
        // 第一步：URL 拼接
        String url = "https://openauth.alipaydev.com/oauth2/publicAppAuthorize.htm?app_id=" + payConfig.getAPP_ID()
                + "&scope=auth_user,auth_base&redirect_uri=" + payConfig.getAUTH_URL();
        // 第二步：获取 auth_code
        response.sendRedirect(url);
    }

    /**
     * 授权 https://docs.open.alipay.com/284
     */
    @RequestMapping(value = "/auth")
    @ResponseBody
    public String auth(HttpServletRequest request) throws Exception {
        // 第二步：获取 auth_code
        Map<String, String> map = PayUtils.requestToMap(request);
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
    }

    /****************************************
     * 电脑网站支付 https://docs.open.alipay.com/270
     * 1.买家在商户网站选择需购买的商品，填写订单信息后，点击立即购买，如下图所示。
     * 2.网页跳转到支付宝收银台页面，此时有两种付款方式供用户选择：
     * 用户可以使用支付宝 APP 扫一扫屏幕二维码，待手机提示付款后选择支付工具输入密码即可完成支付，如图1所示；
     * 如果不使用手机支付，也可以点击图1页面右侧的登录账户付款，输入支付宝账号和支付密码登录 PC 收银台，如图2所示。
     * 3.用户选择付款方式，输入支付密码后点击确认付款，跳转到付款成功页后则表示付款成功。
     * 枚举名称         枚举说明
     * WAIT_BUYER_PAY   交易创建，等待买家付款
     * TRADE_CLOSED     未付款交易超时关闭，或支付完成后全额退款
     * TRADE_SUCCESS    交易支付成功
     * TRADE_FINISHED   交易结束，不可退款
     ****************************************/

//    private List<Map<String, String>> getGoodsDetail() {
//        List<Map<String, String>> detail = new ArrayList<>();
//        Map<String, String> goods = new HashMap<>();
//        goods.put("goods_id", "apple-01"); // 商品的编号
//        goods.put("alipay_goods_id", "20010001"); // 支付宝定义的统一商品编号
//        goods.put("goods_name", "ipad"); // 商品名称
//        goods.put("quantity", "1"); // 商品数量
//        goods.put("price", "77.77"); // 商品单价，单位为元
//        goods.put("goods_category", "34543238"); // 商品类目
//        goods.put("categories_tree", "124868003|126232002|126252004"); // 商品类目树，从商品类目根节点到叶子节点的类目id组成，类目id值使用|分割
//        goods.put("body", "特价手机"); // 商品描述信息
//        goods.put("show_url", "http://www.alipay.com/xxx.jpg"); // 商品的展示地址
//        detail.add(goods);
//        return detail;
//    }

    /**
     * PC端支付 https://docs.open.alipay.com/api_1/alipay.trade.pay/
     */
    @RequestMapping(value = "/pay")
    @ResponseBody
    public void pay(HttpServletResponse response) throws Exception {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(payConfig.getRETURN_URL()); // 沙箱设置授权回调地址
        request.setNotifyUrl(payConfig.getNOTIFY_URL()); // 沙箱设置授权回调地址
        // 设置参数
        Map<String, Object> biz = new HashMap<>();
        biz.put("out_trade_no", snowFlake.nextId());
        biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
        biz.put("total_amount", 77.77);
        biz.put("subject", "PC支付测试");
        biz.put("body", "支付宝PC支付测试");
//        biz.put("goods_detail", getGoodsDetail());
        log.info(JSON.toJSONString(biz));
        request.setBizContent(JSON.toJSONString(biz));
        // 渲染页面
        String form = alipayClient.pageExecute(request).getBody();
        response.setContentType("text/html;charset=" + payConfig.getCHARSET());
        response.getWriter().write(form); //直接将完整的表单html输出到页面
        response.getWriter().flush();
        response.getWriter().close();
    }

    /**
     * 查询订单 https://docs.open.alipay.com/api_1/alipay.trade.query/
     */
    @RequestMapping(value = "/query")
    @ResponseBody
    public String query(@RequestParam(required = false, name = "outTradeNo") String outTradeNo,
                        @RequestParam(required = false, name = "tradeNo") String tradeNo) throws Exception {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String, Object> biz = new HashMap<>();
        if (!StringUtils.isEmpty(outTradeNo)) {
            biz.put("out_trade_no", outTradeNo);
        }
        if (!StringUtils.isEmpty(tradeNo)) {
            biz.put("trade_no", tradeNo);
        }
        log.info(JSON.toJSONString(biz));
        request.setBizContent(JSON.toJSONString(biz));
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        return response.getBody(); // response.isSuccess()
    }

    /**
     * 退款 https://docs.open.alipay.com/api_1/alipay.trade.refund/
     */
    @RequestMapping(value = "/refund")
    @ResponseBody
    public String refund(@RequestParam(required = false, name = "outTradeNo") String outTradeNo,
                         @RequestParam(required = false, name = "tradeNo") String tradeNo) throws Exception {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        Map<String, Object> biz = new HashMap<>();
        if (!StringUtils.isEmpty(outTradeNo)) {
            biz.put("out_trade_no", outTradeNo);
        }
        if (!StringUtils.isEmpty(tradeNo)) {
            biz.put("trade_no", tradeNo);
        }
        biz.put("refund_amount", 77.77); // 必选
        log.info(JSON.toJSONString(biz));
        request.setBizContent(JSON.toJSONString(biz));
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        return response.getBody(); // response.isSuccess()
    }

    /**
     * 关闭订单 https://docs.open.alipay.com/api_1/alipay.trade.close
     */
    @RequestMapping(value = "/close")
    @ResponseBody
    public String close(@RequestParam(required = false, name = "outTradeNo") String outTradeNo,
                        @RequestParam(required = false, name = "tradeNo") String tradeNo) throws Exception {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        Map<String, Object> biz = new HashMap<>();
        if (!StringUtils.isEmpty(outTradeNo)) {
            biz.put("out_trade_no", outTradeNo);
        }
        if (!StringUtils.isEmpty(tradeNo)) {
            biz.put("trade_no", tradeNo);
        }
        log.info(JSON.toJSONString(biz));
        request.setBizContent(JSON.toJSONString(biz));
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        return response.getBody(); // response.isSuccess()
    }

    /**
     * {"charset":"UTF-8",
     * "out_trade_no":"366087187",
     * "method":"alipay.trade.page.pay.return",
     * "total_amount":"77.77",
     * "sign":"JCYNBNGFKkD+cQJ3osd7RSfIOwjWv7BGOBrEgS8ZeSVbFRQ02OqLyhJDTDsHQJa/JbCsQxjbASvf20rGLhWjaQeKAdcUtHkHfHSxGetxSJKVpSVyFS7NxClJYT+Fs63iNvEMzs/SVqfYMp3vT5QRnmIPoFNwKvMIjgwHQxQ1hGNDX38bNPoL2b0LlnlVMmCF6kyQ2mTi5KEPZTa63XQY/tEzGHAXSVJFCa5DoGIRxL1mnJOHafMnjUiMWh2B6hf9CyDJUqzM0OAtN+eaLZ+v8ZVQLemb3xt6xvIhHsf8/Gm/TLt/OZnnmT0dPBPCD/6kjjmlboPNerHhV2jmkUvlvA==",
     * "trade_no":"2019121422001499261000015899",
     * "auth_app_id":"2016101500696043",
     * "version":"1.0",
     * "app_id":"2016101500696043",
     * "sign_type":"RSA2",
     * "seller_id":"2088102179758721",
     * "timestamp":"2019-12-14 14:33:37"}
     */
    @RequestMapping(value = "/return")
    @ResponseBody
    public String Return(HttpServletRequest request) throws Exception {
        // 获取支付宝GET过来的反馈信息
        Map<String, String> map = PayUtils.requestToMap(request);
        log.info(JSON.toJSONString(map));
        boolean verifyResult = AlipaySignature.rsaCheckV1(map, payConfig.getPUBLIC_KEY(), payConfig.getCHARSET(), payConfig.getSIGN_TYPE());
        if (verifyResult) {
            log.info("Return 验证成功");
            return "success";
        } else {
            log.info("Return 验证失败");
            return "failure";
        }
    }

    /**
     * {"gmt_create":"2019-12-14 14:33:21",
     * "charset":"UTF-8",
     * "gmt_payment":"2019-12-14 14:33:29",
     * "notify_time":"2019-12-14 14:33:30",
     * "subject":"PC支付测试",
     * "sign":"nvWhadEbOXn9K2a9bVWQk/5hJlFcTtPKIT8xxTt/zsf0LmXhPC9Sf1dnrhrpqBVFdqZCygr1GyDCWAp1GGFTvZ6tqerF2ilEV9EZugue+JB2PfDvkVMYB4nWDf1Urosdy6FLxWl4tN4w8DktxOUKYl+K4IFOp4er7ktw7oQnOwATJ07R3Y2HOJxNP+5zhKXWlv5Ly9wpR1GtHkljBXJvAJFftPXwU/PVk4kWXRdq2CsXqYFuoSMCwOeO+TsSjd0AyI+EGG0A8ySoBYMByHQGe4qIK/QcXor3ZkV1FvrV+xb0bf1JTxqV3lNM7nW1JEdApPvgqqQOmAqzX2SLjTr4+w==",
     * "buyer_id":"2088102180099262",
     * "body":"支付宝PC支付测试",
     * "invoice_amount":"77.77",
     * "version":"1.0",
     * "notify_id":"2019121400222143330099261000568049",
     * "fund_bill_list":"[{\"amount\":\"77.77\",\"fundChannel\":\"ALIPAYACCOUNT\"}]",
     * "notify_type":"trade_status_sync",
     * "out_trade_no":"366087187",
     * "total_amount":"77.77",
     * "trade_status":"TRADE_SUCCESS",
     * "trade_no":"2019121422001499261000015899",
     * "auth_app_id":"2016101500696043",
     * "receipt_amount":"77.77",
     * "point_amount":"0.00",
     * "app_id":"2016101500696043",
     * "buyer_pay_amount":"77.77",
     * "sign_type":"RSA2",
     * "seller_id":"2088102179758721"}
     */
    @RequestMapping(value = "/notify")
    @ResponseBody
    public String Notify(HttpServletRequest request) throws Exception {
        // 获取支付宝POST过来的反馈信息
        Map<String, String> map = PayUtils.requestToMap(request);
        log.info(JSON.toJSONString(map));
        boolean verifyResult = AlipaySignature.rsaCheckV1(map, payConfig.getPUBLIC_KEY(), payConfig.getCHARSET(), payConfig.getSIGN_TYPE());
        if (verifyResult) {
            log.info("Notify 验证成功");
            return "success";
        } else {
            log.info("Notify 验证失败");
            return "failure";
        }
    }
}
