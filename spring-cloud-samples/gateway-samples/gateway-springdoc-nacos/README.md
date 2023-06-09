# Spring Gateway 集成 SpringDoc 文档示例

## 服务介绍

### 网关服务器
#### 应用
* 应用名：gateway-springdoc-nacos
* 端口：8080

#### 功能
* 提供集群服务反向代理、负载均衡
* 提供集群服务 SpringDoc OpenAPI 文档汇总，统一文档入口

### 文档样例服务
#### 应用
* 应用名：foo-service
* 端口：9000

#### 功能
* 演示 SpringDoc OpenAPI 接口注释、文档配置示例

### Jar 包
#### 通用依赖 Jar 包 
* 包名：commons

#### 功能
* 设置 SpringDoc 依赖：springdoc-openapi-starter-webmvc-ui
* 设置 SpringDoc网关地址、版本、作者、响应通用组件设置等等
* 提供通用服务器信息、服务条款请求端点
* 提供通用全局异常处理
* 设置统一 Web 请求响应格式

## 服务运行

* 启动外部 Nacos 服务器
* 启动 foo-service 文档样例服务
* 启动 gateway-springdoc-nacos 网关服务
* 浏览文档页：http://localhost:8080/swagger-ui.html