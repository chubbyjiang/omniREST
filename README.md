# omniREST
RESTful数据库中间件

#简介

**系统支持**：Centos7+JDK-1.6  
**开发环境**：Intellij IDEA+ MAC OS X+Tomcat6  
**系统目标**：  
基于微服务、CQRS和Event Sourcing思想进行企业级云平台的构建，所有服务通过omniREST接入数据库，通过DSL实现服务扩展，基于EventBus和EventStore实现读写分离、自动同步和自动汇总等功能。  
**系统实现**：
> 1. 配置文件与Swagger-UI整合，提供数据服务的同时提供在线API文档。
> 2. 支持分页、排序、字段比较、多值查询、自定义SQL规则等功能，多个功能之间自由组合。
> 3. 适配新项目只需提供DSL配置文件即可提供对应的数据服务，无需编码实现。
> 4. 基于Event Bus读取事件自动实现三维数据立方体层级指标汇总。
> 5. 读写分离模块基于Event Bus实现读写库自动同步功能。
> 6. 可根据EventStore进行事件回溯，还原任一数据库到指定位置。
