# pay-demo

微信支付对个人开发者很不友好，还好支付宝提供沙箱服务。
> 蚂蚁沙箱环境(Beta)是协助开发者进行接口功能开发及主要功能联调的模拟环境，在沙箱完成接口开发及主要功能调试后，请务必在蚂蚁正式环境进行完整的功能验收测试。所有返回码及业务逻辑以正式环境为准。

## [沙箱](https://openhome.alipay.com/platform/appDaily.htm)
https://sandbox.alipaydev.com/user/accountDetails.htm?index=4

## 文档
https://docs.open.alipay.com/

### [授权](https://docs.open.alipay.com/284)
1. 第一步：URL 拼接
2. 第二步：获取 auth_code
3. 第三步：换取 access_token 和 userId
4. 第四步：获取用户信息

### [支付](https://docs.open.alipay.com/270)
1. alipay.trade.page.pay(统一收单下单并支付页面接口) 
2. alipay.trade.query(统一收单线下交易查询)
3. alipay.trade.refund(统一收单交易退款接口)
4. alipay.trade.close(统一收单交易关闭接口)

## 回调地址
- alipay.authUrl=http://www.littleredhat1997.com:8888/auth
- alipay.notifyUrl=http://www.littleredhat1997.com:8888/notify
- alipay.returnUrl=http://www.littleredhat1997.com:8888/return

### auth_url
前往沙箱设置授权回调地址，而且必须使用外网地址。

1. Ngrok 内网穿透：自己搭建或者买买买......

2. SSH 端口转发：你只需要一台服务器......
```
ssh -NR <local-host>:<local-port>:<remote -host>:<remote-port> user@host

例如：
ssh -NR 8888:127.0.0.1:8080 root@www.littleredhat1997.com
访问：
http://www.littleredhat1997.com:8888/test
转发：
http://localhost:8080/test

-N 表示只连接远程主机，不打开远程shell
-R 将端口绑定到远程服务器，反向代理
```

### return_url和notify_url：
1. 商户系统请求支付宝接口 alipay.trade.page.pay，支付宝对商户请求参数进行校验，而后重新定向至用户登录页面。
2. 用户确认支付后，支付宝通过 get 请求 returnUrl（商户入参传入），返回同步返回参数。
3. 交易成功后，支付宝通过 post 请求 notifyUrl（商户入参传入），返回异步通知参数。
4. 若由于网络等问题异步通知没有到达，商户可自行调用交易查询接口 alipay.trade.query 进行查询，根据查询接口获取交易以及支付信息（商户也可以直接调用查询接口，不需要依赖异步通知）。

- 由于同步返回的不可靠性，支付结果必须以异步通知或查询接口返回为准，不能依赖同步跳转。
- 商户系统接收到异步通知以后，必须通过验签（验证通知中的 sign 参数）来确保支付通知是由支付宝发送的。详细验签规则参考异步通知验签。
- 接收到异步通知并验签通过后，一定要检查通知内容，包括通知中的 app_id、out_trade_no、total_amount 是否与请求中的一致，并根据 trade_status 进行后续业务处理。
- 在支付宝端，partnerId 与 out_trade_no 唯一对应一笔单据，商户端保证不同次支付 out_trade_no 不可重复；若重复，支付宝会关联到原单据，基本信息一致的情况下会以原单据为准进行支付。

## 唯一ID算法SnowFlake
SnowFlake算法用来生成64位的ID，刚好可以用long整型存储，能够用于分布式系统中生产唯一的ID， 并且生成的ID有大致的顺序。 在这次实现中，生成的64位ID可以分成5个部分：
```
0 - 41位时间戳 - 5位数据中心标识 - 5位机器标识 - 12位序列号
```
5位数据中心标识跟5位机器标识这样的分配仅仅是当前实现中分配的，如果业务有其实的需要，可以按其它的分配比例分配，如10位机器标识，不需要数据中心标识。
 