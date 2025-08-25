# Demo Lab · 技术专题实践
`#SpringBoot实战#` · `#Spring源码#` · `#技术点复现#`

> **项目说明**：本仓库为个人技术研究实验室，通过独立模块复现实际项目中的核心场景与技术方案，代码均为脱敏后的简化实现。

---

## 📦 experiments
| 模块名称             | 技术焦点                 | 关联场景       |  
|------------------|----------------------|------------|
| **distributed-lock** | Redis分布式锁 + Redisson | 控制对共享资源对访问 |
| **echo-pattern** | 过滤器 + 拦截器            | 日志记录       |
| **dynamic-config-refresh**  | Spring Binder + 反射   | 动态配置刷新     |


### 🎯 模块技术细节

#### 1.distributed-lock 模块
- **场景还原**：使用分布式锁，避免高并发下大量请求同时请求MySQL，导致宕机。
- **主要内容**：
  - 基于原生 Redis 实现分布式锁
  - 基于 Redisson 实现分布式锁
- **测试方式**
  - http://localhost:8080/distributedlock.html

#### 2.echo-patter 模块
- **相关文档**：[点击阅读](https://hachinekooo.github.io/docs/code/backend/java/echo-pattern-in-request-processing.html)
- **场景还原**：在拦截器层面, 实现日志统一处理.
- **主要内容**：
  - 过滤器：记录请求的URL和参数信息
  - 拦截器：记录 Handler 的具体信息
- **测试方式**
  - http://localhost:8080/echoPattern.html

#### 3.dynamic-config-refresh 模块
- **相关文档**：
  - [属性类的动态刷新](https://hachinekooo.github.io/docs/code/backend/java/dynamic-config-configuration.html)
  - [@Value值注入的动态刷新](https://hachinekooo.github.io/docs/code/backend/java/dynamic-config-configuration.html)
- **场景还原**：需要配置信息实时生效的场景、不想要引入其它过重服务的场景.
- **主要内容**：
  - 属性类的动态刷新
  - @Value 注解的动态刷新
- **测试方式**
  - http://localhost:8080/dynamicConfig.html

## services

| 模块名称           | 实现方案     | 业务场景         |  
|----------------|----------|--------------|
| **blockchain** | AOP、设计模式 | 对业务操作进行区块链存证 |

### 🎯 模块技术细节

#### 1.blockchain 模块
- **相关文档**：
- **场景还原**：
- **主要内容**：
  - 基于 AOP 实现对业务方法的拦截
  - 基于 设计模式 实现对业务方法的封装
  - 基于 策略模式 实现对不同的区块链存证方案的切换
- **测试方式**
    - 


