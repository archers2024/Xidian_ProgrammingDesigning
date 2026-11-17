# 64条优化点与验收清单

审查日期：2026年11月16日。对象：当前本机代码、部署模板和现有构建产物。

共 **64 条**。优先级：P1 21条、P2 38条、P3 5条。证据等级：静态确认 38条、待验证风险 10条、实测确认 5条、工程优化 11条。

本轮只审查和记录，没有修改业务逻辑、重启服务或执行写入型回归。HTTP探针共6个，包含5个GET和1个OPTIONS。没有新增压力测试、外网安全测试或真机验收。此前274项通过记录属于此前交付，不能代替本轮发现项的验收。

“实测确认”表示本轮已有运行响应或产物证据；“静态确认”表示源码明确存在相应实现或缺口；“工程优化”表示能力改进，不等于现有故障；“待验证风险”表示尚需故障注入、并发或规模验证。P1为上线前或下一轮优先处理，P2为工程质量与规模治理，P3为体验和长期维护。没有将全部64条称为64个已复现漏洞。

优先处理：OPT-001/002/003 凭据治理；OPT-004 导出令牌；OPT-006/007 跨域；OPT-018 账号事务；OPT-033 HTTPS；OPT-037/038 数据库升级；OPT-057/058 健康检查与HTTP错误语义；OPT-064 测试环境围栏。

## 只读运行证据

| 检查 | 观察 |
| --- | --- |
| GET /dict/activity-type | HTTP 200；业务码 0；success |
| GET /admin/current | HTTP 401；业务码 40002；未登录或登录已过期 |
| GET /actuator/health | HTTP 200；业务码 50000；系统开小差了，请稍后重试 |
| GET /actuator/prometheus | HTTP 200；业务码 50000；系统开小差了，请稍后重试 |
| GET /__audit_missing_route_20261116 | HTTP 200；业务码 50000；系统开小差了，请稍后重试 |
| OPTIONS /admin/current | HTTP 401；来源 https://audit.invalid；credentials=true |

完整结果：[只读探针结果](optimization-probes-20261116.json)。构建产物字节为磁盘未压缩大小，不是网络压缩大小或用户延迟。

## 优化清单索引

| 编号 | 优先级 | 证据等级 | 优化点 |
| --- | --- | --- | --- |
| [OPT-001](#opt-001) | P1 | 静态确认 | 数据库口令仍写在工程配置中 |
| [OPT-002](#opt-002) | P1 | 静态确认 | 首次启动默认超管使用固定口令且写入日志 |
| [OPT-003](#opt-003) | P1 | 静态确认 | 账号重置使用统一固定临时密码 |
| [OPT-004](#opt-004) | P1 | 静态确认 | 名单导出将管理员 JWT 放进 URL |
| [OPT-005](#opt-005) | P2 | 待验证风险 | 管理员会话存于 localStorage |
| [OPT-006](#opt-006) | P1 | 实测确认 | 跨域策略对任意来源反射并允许凭据 |
| [OPT-007](#opt-007) | P1 | 实测确认 | 受保护接口的跨域预检被登录拦截器返回401 |
| [OPT-008](#opt-008) | P2 | 工程优化 | 上传文件没有组织归属和访问级别 |
| [OPT-009](#opt-009) | P2 | 静态确认 | 账号管理服务未自身校验超级管理员权限 |
| [OPT-010](#opt-010) | P2 | 静态确认 | 名单服务层缺少组织与明文手机号权限校验 |
| [OPT-011](#opt-011) | P2 | 待验证风险 | 普通管理员组织为空时查询范围可能变成全平台 |
| [OPT-012](#opt-012) | P2 | 静态确认 | 登录限流主要按已存在账号计数 |
| [OPT-013](#opt-013) | P2 | 工程优化 | 登录失败计数只保存在当前进程 |
| [OPT-014](#opt-014) | P2 | 工程优化 | 报名限流只保存在当前进程 |
| [OPT-015](#opt-015) | P1 | 工程优化 | 活动没有强制准确签到的服务端策略 |
| [OPT-016](#opt-016) | P1 | 工程优化 | 手机号格式正确不代表报名或签到者拥有该手机号 |
| [OPT-017](#opt-017) | P2 | 静态确认 | 浏览器设备标识可由客户端重置 |
| [OPT-018](#opt-018) | P1 | 静态确认 | 部分账号写操作与审计缺少共同事务 |
| [OPT-019](#opt-019) | P1 | 待验证风险 | 最后一个超管保护依赖 JVM synchronized |
| [OPT-020](#opt-020) | P2 | 静态确认 | 密码变更和登录失败的审计覆盖不完整 |
| [OPT-021](#opt-021) | P1 | 静态确认 | 审批历史没有不可变活动内容快照 |
| [OPT-022](#opt-022) | P2 | 工程优化 | 过期活动发布策略尚未明确 |
| [OPT-023](#opt-023) | P2 | 待验证风险 | 编辑缺少客户端版本冲突检测 |
| [OPT-024](#opt-024) | P2 | 工程优化 | 创建活动与报名等写请求缺少业务幂等键 |
| [OPT-025](#opt-025) | P2 | 静态确认 | 活动列表 GET 会触发全局状态更新 |
| [OPT-026](#opt-026) | P2 | 待验证风险 | 状态刷新每轮包含已结束活动 |
| [OPT-027](#opt-027) | P2 | 待验证风险 | 同一活动所有签到争用同一活动行锁 |
| [OPT-028](#opt-028) | P2 | 静态确认 | 报名限流每请求持有全局锁并遍历 IP 集合 |
| [OPT-029](#opt-029) | P2 | 静态确认 | 报名和签到名单未分页 |
| [OPT-030](#opt-030) | P2 | 静态确认 | 组织和管理员列表未分页 |
| [OPT-031](#opt-031) | P2 | 待验证风险 | Excel 导出同时持有全量结果和内存工作簿 |
| [OPT-032](#opt-032) | P2 | 待验证风险 | 设备限频查询缺少匹配三列联合索引 |
| [OPT-033](#opt-033) | P1 | 静态确认 | 部署模板仅提供 HTTP |
| [OPT-034](#opt-034) | P1 | 静态确认 | 运行数据库账号被授予库级全部权限 |
| [OPT-035](#opt-035) | P2 | 工程优化 | 文件上传与本机磁盘直接耦合 |
| [OPT-036](#opt-036) | P2 | 静态确认 | 上传缺少配额和未引用文件生命周期管理 |
| [OPT-037](#opt-037) | P1 | 静态确认 | 增量数据库迁移没有版本账本和校验和 |
| [OPT-038](#opt-038) | P1 | 静态确认 | 上传脚本未包含增量迁移文件 |
| [OPT-039](#opt-039) | P1 | 静态确认 | 初始化脚本缺少已有业务库保护 |
| [OPT-040](#opt-040) | P1 | 静态确认 | 部署原地覆盖应用且没有版本化回滚 |
| [OPT-041](#opt-041) | P2 | 静态确认 | 路由守卫只检查 token 是否存在 |
| [OPT-042](#opt-042) | P3 | 静态确认 | 前端缺少未知路由兜底页面 |
| [OPT-043](#opt-043) | P2 | 静态确认 | 401 的默认提示可能误报为网络连接失败 |
| [OPT-044](#opt-044) | P2 | 待验证风险 | 审批和审计列表缺少过期请求结果保护 |
| [OPT-045](#opt-045) | P2 | 静态确认 | 部分列表请求失败后看起来像正常空数据 |
| [OPT-046](#opt-046) | P2 | 静态确认 | 活动详情的辅助数据失败会使整页不可用 |
| [OPT-047](#opt-047) | P2 | 静态确认 | 创建活动后的提示仍假设可直接发布 |
| [OPT-048](#opt-048) | P2 | 静态确认 | 活动编辑未处理未保存离开 |
| [OPT-049](#opt-049) | P2 | 静态确认 | 工作详情和签到大屏靠下载全部签到记录计数 |
| [OPT-050](#opt-050) | P2 | 静态确认 | 签到大屏倒计时依赖本地每秒递减 |
| [OPT-051](#opt-051) | P3 | 静态确认 | 移动签到页活动名称没有被加载 |
| [OPT-052](#opt-052) | P3 | 静态确认 | 活动标题长度在前后端不一致 |
| [OPT-053](#opt-053) | P2 | 待验证风险 | 时间格式没有明确跨时区契约 |
| [OPT-054](#opt-054) | P3 | 静态确认 | 活动表单校验主要靠统一toast |
| [OPT-055](#opt-055) | P3 | 工程优化 | 前端API契约缺少静态类型约束 |
| [OPT-056](#opt-056) | P2 | 实测确认 | 主入口和图表产物仍超过500KB |
| [OPT-057](#opt-057) | P1 | 实测确认 | 没有可用的就绪与存活探针 |
| [OPT-058](#opt-058) | P1 | 实测确认 | 未知资源被全局异常包装成HTTP200系统错误 |
| [OPT-059](#opt-059) | P2 | 工程优化 | 请求缺少贯通日志与响应的追踪ID |
| [OPT-060](#opt-060) | P2 | 静态确认 | 后端文件日志没有随部署提供轮转策略 |
| [OPT-061](#opt-061) | P1 | 工程优化 | 部署包没有数据库与上传文件恢复演练入口 |
| [OPT-062](#opt-062) | P2 | 静态确认 | CI只构建和跑单元测试，未执行真实数据库接口回归 |
| [OPT-063](#opt-063) | P2 | 静态确认 | 前端没有自动化交互和质量检查入口 |
| [OPT-064](#opt-064) | P1 | 静态确认 | 部分接口回归固定指向8080且没有测试环境围栏 |

## 安全与认证

<a id="opt-001"></a>

### OPT-001 · 数据库口令仍写在工程配置中

**P1 / 静态确认**

证据：[backend/src/main/resources/application.yml:15](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/resources/application.yml:15)。定位片段：`password: <已隐藏>`

影响与边界：源码和构建包会携带本机数据库凭据；是否曾对外泄露未验证。

改进：改为环境变量或外置配置，限制读取权限；外发过的凭据轮换。

验收：新构建包及受版本控制文件不含实际口令；缺失必需配置时给出明确启动错误。

<a id="opt-002"></a>

### OPT-002 · 首次启动默认超管使用固定口令且写入日志

**P1 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/config/DataInitializer.java:37](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/config/DataInitializer.java:37)。定位片段：`admin.setPasswordHash(new BCryptPasswordEncoder().encode("<固定默认口令>"));`

影响与边界：新环境默认凭据可预测，初始化未显式设置必须改密。

改进：首启从受控配置接收一次性口令，强制改密，日志只记录初始化事件。

验收：新库无公开固定凭据；首次登录只能改密；日志不含明文密码。

<a id="opt-003"></a>

### OPT-003 · 账号重置使用统一固定临时密码

**P1 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:124](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:124)。定位片段：`.eq(Admin::getAdminId, adminId).set(Admin::getPasswordHash, encoder.encode("<固定默认口令>"))`

影响与边界：所有被重置账号共享可预测临时密码；强制改密不能替代初次登录保密。

改进：生成每账号一次性随机凭据或短期激活链接，并设置有效期。

验收：两个账号重置凭据不同，过期或使用后不可再次激活。

<a id="opt-004"></a>

### OPT-004 · 名单导出将管理员 JWT 放进 URL

**P1 / 静态确认**

证据：[frontend/src/api/index.js:58](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/api/index.js:58)。定位片段：`/api/v1/admin/activity/${activityId}/registrations/export?maskPhone=${maskPhone}&token=${localStorage.getItem('token') || ''}`

影响与边界：令牌可能进入访问日志、浏览器历史和转发链接。

改进：用 Authorization 请求下载 Blob，或使用绑定用户与用途的短期一次性下载票据。

验收：导出 URL 不含会话 JWT；越权或重放下载票据被拒绝。

<a id="opt-005"></a>

### OPT-005 · 管理员会话存于 localStorage

**P2 / 待验证风险**

证据：[frontend/src/api/request.js:10](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/api/request.js:10)。定位片段：`const token = localStorage.getItem('token')`

影响与边界：一旦发生同源脚本注入，脚本可读取长期会话；本轮未证明存在 XSS 注入点。

改进：评估 HttpOnly/Secure/SameSite Cookie 与 CSRF 配套，或缩短内存令牌生命周期。

验收：明确会话威胁模型，验证退出、刷新、多标签及 CSRF 防护；不能只改存储位置。

<a id="opt-006"></a>

### OPT-006 · 跨域策略对任意来源反射并允许凭据

**P1 / 实测确认**

证据：[backend/src/main/java/com/campus/activity/config/WebConfig.java:29](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/config/WebConfig.java:29)。定位片段：`.allowedOriginPatterns("*")`

影响与边界：OPTIONS 探针反射 audit.invalid 且 allow-credentials=true；目前预检同时被401阻断，不等于已证明跨站数据泄漏。

改进：按环境配置精确来源白名单；未使用 Cookie 凭据时关闭 credentials。

验收：可信来源预检通过，未配置来源无允许跨域头；保留同源访问。

<a id="opt-007"></a>

### OPT-007 · 受保护接口的跨域预检被登录拦截器返回401

**P1 / 实测确认**

证据：[backend/src/main/java/com/campus/activity/security/LoginInterceptor.java:47](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/security/LoginInterceptor.java:47)。定位片段：`String token = resolveToken(request);`

影响与边界：不带实际会话头的浏览器 OPTIONS 预检被拒，跨域前后端部署不可正常调用受保护接口。

改进：先由 CORS 处理合法预检，业务 GET/POST 继续鉴权；不要放开实际请求。

验收：白名单来源的 OPTIONS 返回正确允许头；无 JWT 的实际 GET 仍401。

<a id="opt-008"></a>

### OPT-008 · 上传文件没有组织归属和访问级别

**P2 / 工程优化**

证据：[backend/src/main/java/com/campus/activity/config/WebConfig.java:43](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/config/WebConfig.java:43)。定位片段：`registry.addResourceHandler("/files/**")`

影响与边界：静态文件按路径公开；公开海报合理，但内部名单类附件无法使用同一机制实现组织隔离。

改进：区分公开海报与受限附件，记录归属和权限，受限文件经鉴权下载或签名链接。

验收：公开文件可匿名读取，受限文件跨组织和匿名访问拒绝。

## 权限与身份

<a id="opt-009"></a>

### OPT-009 · 账号管理服务未自身校验超级管理员权限

**P2 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:72](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:72)。定位片段：`public void createAdmin(Admin admin) {`

影响与边界：当前依赖 /super 拦截器；将来其他入口直接调用服务时容易越过权限边界，未证明现有 HTTP 可绕过。

改进：在创建、停用、重置等服务入口统一 requireSuper。

验收：绕过控制器直接调用时，普通管理员和匿名上下文仍被拒绝。

<a id="opt-010"></a>

### OPT-010 · 名单服务层缺少组织与明文手机号权限校验

**P2 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/signup/service/SignupService.java:151](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/signup/service/SignupService.java:151)。定位片段：`public List<RegistrationVO> registrations(Long activityId, boolean maskPhone) {`

影响与边界：组织边界在拦截器，明文手机号权限在控制器；直接复用该服务可能返回其他组织或明文数据。

改进：读服务显式接收访问范围或调用共享 Access；脱敏权限也放服务层。

验收：直接服务调用跨组织和无明文权限均被拒；签到名单读接口同步覆盖。

<a id="opt-011"></a>

### OPT-011 · 普通管理员组织为空时查询范围可能变成全平台

**P2 / 待验证风险**

证据：[backend/src/main/java/com/campus/activity/security/OrganizationAccess.java:48](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/security/OrganizationAccess.java:48)。定位片段：`return admin.isSuper() ? null : admin.getOrgId();`

影响与边界：null 被下游解释为不加组织筛选；正常数据库组织列非空，本轮未发现可构造该上下文的现有 HTTP 路径。

改进：对非 SUPER 的空组织上下文立即拒绝，不与全平台哨兵共用 null。

验收：构造 ADMIN+null 组织单元测试，所有列表查询失败关闭。

<a id="opt-012"></a>

### OPT-012 · 登录限流主要按已存在账号计数

**P2 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:40](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:40)。定位片段：`if (admin == null) throw new BusinessException("账号或密码错误");`

影响与边界：不存在账号在限流前返回；大量未知账号请求、单账号恶意锁定缺少按来源约束。

改进：增加 IP/账号组合限流、风险触发校验与统一认证失败响应。

验收：未知账号高频请求受限；恶意来源不能无限制造数据库查询或轻易锁死他人账号。

<a id="opt-013"></a>

### OPT-013 · 登录失败计数只保存在当前进程

**P2 / 工程优化**

证据：[backend/src/main/java/com/campus/activity/util/LoginAttemptLimiter.java:11](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/util/LoginAttemptLimiter.java:11)。定位片段：`private final Map<String, Attempt> attempts = new HashMap<>();`

影响与边界：重启丢失，多实例不共享，现有单机测试不代表分布式限流有效。

改进：以 TTL 和原子计数实现共享限流，明确不可用时的降级策略。

验收：跨两个实例连续失败达到同一阈值，超时后自动恢复。

<a id="opt-014"></a>

### OPT-014 · 报名限流只保存在当前进程

**P2 / 工程优化**

证据：[backend/src/main/java/com/campus/activity/module/signup/service/SignupService.java:45](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/signup/service/SignupService.java:45)。定位片段：`private final Map<String, long[]> ipWindow = new ConcurrentHashMap<>();`

影响与边界：多实例阈值叠加，校园 NAT 下不同用户共享同一 IP 限额。

改进：设计共享 IP+活动或身份维度限流；结合真实代理信任边界。

验收：跨实例阈值一致，同时验证同校园出口合法多人报名不会大面积误拦。

<a id="opt-015"></a>

### OPT-015 · 活动没有强制准确签到的服务端策略

**P1 / 工程优化**

证据：[backend/src/main/java/com/campus/activity/module/checkin/service/CheckinService.java:126](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/checkin/service/CheckinService.java:126)。定位片段：`String method = "BASIC";`

影响与边界：省略 code 会走 BASIC；当前确实支持基本签到，不能把“支持准确签到”当作“强制定位和动态码”。

改进：为活动增加 BASIC/ACCURATE_REQUIRED 策略，在服务端拒绝降级。

验收：强制准确的活动省略 code、定位或设备均失败；基本模式按既定规则可用。

<a id="opt-016"></a>

### OPT-016 · 手机号格式正确不代表报名或签到者拥有该手机号

**P1 / 工程优化**

证据：[backend/src/main/java/com/campus/activity/module/checkin/service/CheckinService.java:116](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/checkin/service/CheckinService.java:116)。定位片段：`Registration reg = signupService.findByPhone(activityId, phone);`

影响与边界：知晓已报名手机号者可尝试代签；当前没有短信或统一身份所有权验证。

改进：按场景接入校园统一身份或验证手机号，绑定报名主体与签到主体。

验收：仅知手机号但无法证明身份的请求不能完成受身份约束的签到。

<a id="opt-017"></a>

### OPT-017 · 浏览器设备标识可由客户端重置

**P2 / 静态确认**

证据：[frontend/src/mobile/CheckinView.vue:75](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/mobile/CheckinView.vue:75)。定位片段：`let id = localStorage.getItem('checkinDevice')`

影响与边界：localStorage 随机 ID 和客户端坐标都是客户端上报，不能证明唯一物理设备或真实位置。

改进：将其定位为风控信号，结合身份和异常行为检测，文案不宣称强防代签。

验收：清除存储或自定义 device 的测试被记录为可规避信号，关键限制由身份规则兜底。

## 后端一致性

<a id="opt-018"></a>

### OPT-018 · 部分账号写操作与审计缺少共同事务

**P1 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:118](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:118)。定位片段：`public void resetPassword(Long adminId) {`

影响与边界：该服务无事务注解，更新成功后审计失败可能留下部分提交。

改进：为创建、状态、重置等业务方法添加事务，明确审计失败是否回滚。

验收：注入审计写入异常，账号状态或密码不会部分提交。

<a id="opt-019"></a>

### OPT-019 · 最后一个超管保护依赖 JVM synchronized

**P1 / 待验证风险**

证据：[backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:103](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:103)。定位片段：`public synchronized void updateStatus(Long adminId, Integer status) {`

影响与边界：单机串行有效，两个服务实例可能同时认为仍有其他启用超管，需并发复现实验。

改进：用数据库事务锁定统一管理行或超管集合，建立跨实例不变量。

验收：双实例同时停用最后两名超管，至少一人保持启用，失败请求有明确结果。

<a id="opt-020"></a>

### OPT-020 · 密码变更和登录失败的审计覆盖不完整

**P2 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:140](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:140)。定位片段：`public void changePassword(String oldPassword, String newPassword) {`

影响与边界：成功登录有审计，改密/退出和认证失败没有同等事件记录，事故追溯缺口。

改进：定义事件清单，记录结果、原因码、主体、来源及请求ID，绝不记录口令。

验收：对应流程均可查事件，且日志中不含密码和完整 JWT。

<a id="opt-021"></a>

### OPT-021 · 审批历史没有不可变活动内容快照

**P1 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/activity/mapper/ActivityReviewMapper.java:17](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/activity/mapper/ActivityReviewMapper.java:17)。定位片段：`SELECT r.*, a.title AS activity_title, o.name AS org_name, u.real_name AS submitter_name`

影响与边界：历史列表关联当前活动标题，改稿后不能重建某次审批实际审过的完整内容。

改进：提交时保存活动版本及附件清单快照，决定绑定明确版本。

验收：改稿后旧审批仍展示原内容和版本，已删除活动的历史仍可追溯。

<a id="opt-022"></a>

### OPT-022 · 过期活动发布策略尚未明确

**P2 / 工程优化**

证据：[backend/src/main/java/com/campus/activity/module/activity/service/ActivityService.java:113](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/activity/service/ActivityService.java:113)。定位片段：`public void publish(Long activityId) {`

影响与边界：创建校验只保证时间相对顺序；发布未验证当前时间是否已超过活动结束。

改进：区分正常发布与历史归档，正常发布拒绝已过期活动。

验收：过期草稿发布行为有明确用例，归档不能误开放报名。

<a id="opt-023"></a>

### OPT-023 · 编辑缺少客户端版本冲突检测

**P2 / 待验证风险**

证据：[backend/src/main/java/com/campus/activity/module/activity/service/ActivityService.java:69](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/activity/service/ActivityService.java:69)。定位片段：`public void update(Activity activity, List<Attachment> attachments) {`

影响与边界：行锁保证数据库串行写，不会自动发现两个操作者分别打开旧表单后依次保存的覆盖。

改进：添加 version 或 If-Match，保存时进行条件更新并提示冲突。

验收：A和B读取同版，A保存后B保存返回冲突，不覆盖A修改。

<a id="opt-024"></a>

### OPT-024 · 创建活动与报名等写请求缺少业务幂等键

**P2 / 工程优化**

证据：[backend/src/main/java/com/campus/activity/module/activity/service/ActivityService.java:48](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/activity/service/ActivityService.java:48)。定位片段：`activity.setActivityNo(generateNo());`

影响与边界：请求成功但响应丢失时重试可能重复创建活动；唯一报名索引不能覆盖所有写业务的幂等语义。

改进：按主体+操作+请求键保存短期结果，定义重放及冲突行为。

验收：相同键重试返回同一业务结果，不同内容复用同键被拒。

## 性能与数据访问

<a id="opt-025"></a>

### OPT-025 · 活动列表 GET 会触发全局状态更新

**P2 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/activity/service/ActivityService.java:138](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/activity/service/ActivityService.java:138)。定位片段：`refreshAllStatus();`

影响与边界：每次读列表都执行全平台写 SQL，读流量可能放大数据库扫描和锁竞争。

改进：移除列表入口的全局写，使用显示时计算状态或专门调度推进。

验收：列表查询不再触发 UPDATE，状态边界展示仍正确，并对比读负载数据库耗时。

<a id="opt-026"></a>

### OPT-026 · 状态刷新每轮包含已结束活动

**P2 / 待验证风险**

证据：[backend/src/main/java/com/campus/activity/module/activity/mapper/ActivityMapper.java:21](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/activity/mapper/ActivityMapper.java:21)。定位片段：`"update_time = NOW() WHERE deleted = 0 AND status IN ('OPEN','CLOSED','ONGOING','FINISHED') " +`

影响与边界：历史 FINISHED 行持续进入候选条件，规模扩大后的扫描成本需执行计划和数据验证。

改进：只推进仍可变化的状态，利用下一状态时间和索引限制扫描。

验收：百万历史行诊断中已结束数据不反复扫描；保留全部状态边界测试。

<a id="opt-027"></a>

### OPT-027 · 同一活动所有签到争用同一活动行锁

**P2 / 待验证风险**

证据：[backend/src/main/java/com/campus/activity/module/checkin/service/CheckinService.java:101](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/checkin/service/CheckinService.java:101)。定位片段：`Activity a = activityMapper.selectForUpdate(activityId);`

影响与边界：正确性有保护，但高峰签到会串行进入活动临界区；尚未证明目标规模下吞吐足够。

改进：测量锁等待后，细化到报名与设备约束，同时保持取消/关闭的一致性。

验收：同一活动阶梯并发记录成功率、P95/P99和锁等待；重复签到/设备规则不回退。

<a id="opt-028"></a>

### OPT-028 · 报名限流每请求持有全局锁并遍历 IP 集合

**P2 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/signup/service/SignupService.java:132](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/signup/service/SignupService.java:132)。定位片段：`private synchronized void checkIpLimit(String ip) {`

影响与边界：活跃 IP 增加时，所有报名请求争用一把锁且反复清理全表。

改进：使用有界过期缓存或共享原子限流，淘汰工作与热点请求解耦。

验收：多IP并发下临界区开销有界，内存随配置上限稳定。

<a id="opt-029"></a>

### OPT-029 · 报名和签到名单未分页

**P2 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/signup/service/SignupService.java:152](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/signup/service/SignupService.java:152)。定位片段：`List<RegistrationVO> list = registrationMapper.listWithCheckin(activityId);`

影响与边界：一次返回整场人员及关联签到；当前活动名额最多5000，仍需评估响应体与表格渲染。

改进：服务端分页、过滤和稳定排序，导出使用独立读取流程。

验收：单页条数受限，5000人场景翻页无重复或漏行。

<a id="opt-030"></a>

### OPT-030 · 组织和管理员列表未分页

**P2 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:131](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/system/service/AdminService.java:131)。定位片段：`return adminMapper.selectList(new LambdaQueryWrapper<Admin>().orderByDesc(Admin::getAdminId));`

影响与边界：平台组织与账号增长时全量返回；账号页还对每行线性查组织名称。

改进：增加分页搜索，组织选择器使用远程检索或映射字典。

验收：万级账号诊断下单次返回受限，筛选和选择组织仍可用。

<a id="opt-031"></a>

### OPT-031 · Excel 导出同时持有全量结果和内存工作簿

**P2 / 待验证风险**

证据：[backend/src/main/java/com/campus/activity/util/ExcelUtil.java:20](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/util/ExcelUtil.java:20)。定位片段：`try (XSSFWorkbook wb = new XSSFWorkbook()) {`

影响与边界：多份集合加内存工作簿、自动列宽；单场上限5000，不宣称单次必然OOM，但并发导出需验证。

改进：评估流式 SXSSF、分批读取、固定列宽或异步任务，并限制导出并发。

验收：并发大名单导出记录峰值内存与耗时，文件可打开且行数完整。

<a id="opt-032"></a>

### OPT-032 · 设备限频查询缺少匹配三列联合索引

**P2 / 待验证风险**

证据：[backend/src/main/java/com/campus/activity/module/checkin/service/CheckinService.java:146](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/checkin/service/CheckinService.java:146)。定位片段：`.eq(CheckIn::getDeviceFingerprint, deviceFingerprint)`

影响与边界：查询按活动、设备及签到时间过滤，init.sql 只有单列活动/时间索引。

改进：用 EXPLAIN 验证后评估 activity_id,device_fingerprint,checkin_time 联合索引。

验收：大场签到样本中扫描行数下降，写放大和索引空间也被记录。

## 部署与数据治理

<a id="opt-033"></a>

### OPT-033 · 部署模板仅提供 HTTP

**P1 / 静态确认**

证据：[deploy/nginx.conf:7](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/deploy/nginx.conf:7)。定位片段：`listen 80 default_server;`

影响与边界：登录和参与者资料缺少传输加密，公网 HTTP 也通常无法使用浏览器精确定位。

改进：配置可信证书、HTTPS及HTTP跳转，验证移动端定位和二维码域名。

验收：公网全流程HTTPS，HTTP跳转且手机定位可用，不以localhost代替真机验证。

<a id="opt-034"></a>

### OPT-034 · 运行数据库账号被授予库级全部权限

**P1 / 静态确认**

证据：[deploy/setup.sh:75](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/deploy/setup.sh:75)。定位片段：`GRANT ALL PRIVILEGES ON campus_activity.* TO 'campus'@'127.0.0.1';`

影响与边界：业务进程一旦受损可执行超出读写所需的DDL；迁移与运行权限未隔离。

改进：运行账号仅授必要DML，数据库迁移用独立受控账号。

验收：运行账号不能DROP/ALTER表，正常业务与受控迁移分别通过。

<a id="opt-035"></a>

### OPT-035 · 文件上传与本机磁盘直接耦合

**P2 / 工程优化**

证据：[backend/src/main/java/com/campus/activity/module/system/controller/FileController.java:71](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/system/controller/FileController.java:71)。定位片段：`file.transferTo(dest);`

影响与边界：多实例无法自动共享资源，本地节点或磁盘丢失会影响文件可用性。

改进：提取文件存储接口，保留本地实现并支持受控对象存储。

验收：业务URL不依赖某一进程工作目录，切换存储实现不修改活动业务。

<a id="opt-036"></a>

### OPT-036 · 上传缺少配额和未引用文件生命周期管理

**P2 / 静态确认**

证据：[backend/src/main/java/com/campus/activity/module/system/controller/FileController.java:69](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/module/system/controller/FileController.java:69)。定位片段：`String name = UUID.randomUUID() + "." + ext;`

影响与边界：每次上传都新建文件，单文件限制不限制累计空间；表单放弃或替换会留下未引用文件。

改进：记录上传主体、引用关系和大小，设置组织配额及延迟垃圾回收。

验收：超配额上传拒绝；清理只删除过期未引用文件，不误删在用附件。

<a id="opt-037"></a>

### OPT-037 · 增量数据库迁移没有版本账本和校验和

**P1 / 静态确认**

证据：[db/migrate-review-20261116.sql:1](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/db/migrate-review-20261116.sql:1)。定位片段：`ALTER TABLE t_organization ADD COLUMN require_review TINYINT NOT NULL DEFAULT 1;`

影响与边界：重复执行ALTER失败，数据库当前版本依赖人工判断。

改进：使用版本化迁移执行器，记录已执行版本与校验和，先做备份与兼容策略。

验收：新库与旧库升级均可重复部署；已执行版本不重复执行，修改历史迁移可被检测。

<a id="opt-038"></a>

### OPT-038 · 上传脚本未包含增量迁移文件

**P1 / 静态确认**

证据：[deploy/upload.sh:21](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/deploy/upload.sh:21)。定位片段：`scp "$ROOT/db/init.sql" "$HOST:~/activity-system-pkg/"`

影响与边界：只上传init.sql；setup遇旧表就跳过初始化，升级旧服务器可能直接运行依赖新列的程序。

改进：打包迁移目录并在切换应用前执行受控迁移和版本检查。

验收：用上一版数据库升级，无需手工找SQL，缺少迁移时部署失败而非带病启动。

<a id="opt-039"></a>

### OPT-039 · 初始化脚本缺少已有业务库保护

**P1 / 静态确认**

证据：[db/init.sql:51](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/db/init.sql:51)。定位片段：`DROP TABLE IF EXISTS t_activity;`

影响与边界：脚本包含删表，而组织/审批表使用IF NOT EXISTS；误用于旧库可能出现数据破坏和残留历史引用。

改进：初始化入口先确认空库，迁移与重置拆开；重置脚本必须显式指定临时库。

验收：有业务表时初始化脚本拒绝执行；全新库完整建表，旧库走迁移。

<a id="opt-040"></a>

### OPT-040 · 部署原地覆盖应用且没有版本化回滚

**P1 / 静态确认**

证据：[deploy/setup.sh:84](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/deploy/setup.sh:84)。定位片段：`install -m 644 "$PKG_DIR/activity-system-1.0.0.jar" "$APP_ROOT/app/activity-system-1.0.0.jar"`

影响与边界：jar和前端直接覆盖既有目录，失败后没有本次部署前可验证版本与一键回滚。

改进：按版本发布目录保存制品与哈希，健康检查后原子切换，保存回滚版本。

验收：模拟启动失败自动保留/恢复旧版本；数据库兼容与回滚边界明确。

## 前端交互

<a id="opt-041"></a>

### OPT-041 · 路由守卫只检查 token 是否存在

**P2 / 静态确认**

证据：[frontend/src/router/index.js:40](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/router/index.js:40)。定位片段：`if (isAdminRoute && !localStorage.getItem('token')) {`

影响与边界：普通管理员可直接进入超管页面外壳后才收到接口拒绝；后端权限仍有效。

改进：路由meta标记角色并复用登录态，增加403页面。

验收：手工输入超管URL立即显示无权限，后端接口鉴权保持不变。

<a id="opt-042"></a>

### OPT-042 · 前端缺少未知路由兜底页面

**P3 / 静态确认**

证据：[frontend/src/router/index.js:4](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/router/index.js:4)。定位片段：`const routes = [`

影响与边界：未知链接没有专门404及返回入口。

改进：新增catch-all路由、404说明和回到工作台或活动入口。

验收：随机不存在路径有明确页面和可操作返回按钮。

<a id="opt-043"></a>

### OPT-043 · 401 的默认提示可能误报为网络连接失败

**P2 / 静态确认**

证据：[frontend/src/api/request.js:27](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/api/request.js:27)。定位片段：`const message = err.response?.data?.message || (err.response?.status === 403 ? '无权限访问，请检查账号权限' : '网络连接失败，请稍后重试')`

影响与边界：服务端未带message时401仍走网络失败文案，实际随后会清理令牌跳转。

改进：按401/403/429/超时/断网分别提示，登录后恢复原目标页。

验收：模拟各错误状态，提示与行为匹配，重定向不丢目标路由。

<a id="opt-044"></a>

### OPT-044 · 审批和审计列表缺少过期请求结果保护

**P2 / 待验证风险**

证据：[frontend/src/views/platform/AuditView.vue:7](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/views/platform/AuditView.vue:7)。定位片段：`async function load(){loading.value=true;try{const data=await organizationApi.audit(query);rows.value=data.records;total.value=data.total}catch{}finally{loading.value=false}}`

影响与边界：快速切筛选、翻页后较早的慢响应可能覆盖后发结果；活动列表已有requestId机制可复用。

改进：统一分页加载组合函数，使用取消或请求序号防止乱序回写。

验收：人为颠倒响应顺序，页面只展示最后一次查询结果。

<a id="opt-045"></a>

### OPT-045 · 部分列表请求失败后看起来像正常空数据

**P2 / 静态确认**

证据：[frontend/src/views/platform/OrganizationView.vue:7](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/views/platform/OrganizationView.vue:7)。定位片段：`async function load(){loading.value=true;try{rows.value=await organizationApi.list()}catch{}finally{loading.value=false}}`

影响与边界：catch为空且只有全局toast，页面缺少持久错误态和重试入口。

改进：区分加载、成功为空、失败三种状态，对组织与审计列表统一处理。

验收：断网时显示失败和重试，恢复后可重新加载，不误显示零条业务数据。

<a id="opt-046"></a>

### OPT-046 · 活动详情的辅助数据失败会使整页不可用

**P2 / 静态确认**

证据：[frontend/src/views/activity/ActivityDetailView.vue:86](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/views/activity/ActivityDetailView.vue:86)。定位片段：`} catch (e) { activity.value = null; loadError.value = e.message }`

影响与边界：审批历史、签到列表或二维码任一失败都会清空已成功获取的活动详情。

改进：核心信息独立加载，辅助面板独立错误状态和重试；QR按打开时生成。

验收：签到接口失败时仍能查看活动内容，失败面板可单独重试。

<a id="opt-047"></a>

### OPT-047 · 创建活动后的提示仍假设可直接发布

**P2 / 静态确认**

证据：[frontend/src/views/activity/ActivityEditView.vue:112](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/views/activity/ActivityEditView.vue:112)。定位片段：`ElMessage.success('已创建，请在列表页点击发布')`

影响与边界：新组织默认要审核，而列表不会给未批准活动显示发布，提示与真实流程冲突。

改进：根据组织策略提示提交审核或发布，保存后直达活动详情。

验收：需审核和可直发组织分别得到正确下一步入口。

<a id="opt-048"></a>

### OPT-048 · 活动编辑未处理未保存离开

**P2 / 静态确认**

证据：[frontend/src/views/activity/ActivityEditView.vue:88](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/views/activity/ActivityEditView.vue:88)。定位片段：`const submit = async () => {`

影响与边界：填写较长内容后切菜单、关闭标签或返回，未保存内容可能丢失。

改进：增加脏状态离开确认和可选草稿恢复，保存成功后清除。

验收：编辑后离开有提醒，保存后无误提示，恢复草稿不会覆盖服务器新版本。

## 前端体验与性能

<a id="opt-049"></a>

### OPT-049 · 工作详情和签到大屏靠下载全部签到记录计数

**P2 / 静态确认**

证据：[frontend/src/mobile/CheckinBoardView.vue:46](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/mobile/CheckinBoardView.vue:46)。定位片段：`const list = await checkinApi.list(route.params.activityId)`

影响与边界：只是显示总数却传输姓名、脱敏手机号等全量记录，浪费带宽与解析。

改进：提供组织权限校验的计数/汇总接口，名单按需分页。

验收：计数请求响应体大小不随参与人数线性增加，数字与数据库一致。

<a id="opt-050"></a>

### OPT-050 · 签到大屏倒计时依赖本地每秒递减

**P2 / 静态确认**

证据：[frontend/src/mobile/CheckinBoardView.vue:54](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/mobile/CheckinBoardView.vue:54)。定位片段：`board.value.remainSeconds--`

影响与边界：后台标签节流、休眠后倒计时和二维码可能偏离服务端时间片，人数也仅随刷新更新。

改进：按服务端过期时间与时钟差计算倒计时，页面恢复可见时刷新；计数独立节奏。

验收：后台停留一分钟再回来立即校准，过期码不继续显示为可用。

<a id="opt-051"></a>

### OPT-051 · 移动签到页活动名称没有被加载

**P3 / 静态确认**

证据：[frontend/src/mobile/CheckinView.vue:33](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/mobile/CheckinView.vue:33)。定位片段：`const title = ref('')`

影响与边界：title仅初始化，没有赋值，用户签到前无法确认具体活动。

改进：提供最小公开签到元信息，不暴露签到密钥或内部名单。

验收：扫码进入即显示活动名称及开放状态，错误活动ID有明确提示。

<a id="opt-052"></a>

### OPT-052 · 活动标题长度在前后端不一致

**P3 / 静态确认**

证据：[frontend/src/views/activity/ActivityEditView.vue:7](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/views/activity/ActivityEditView.vue:7)。定位片段：`<el-form-item label="活动名称" required><el-input v-model="form.title" maxlength="60" /></el-form-item>`

影响与边界：后端和数据库允许200字，前端只能输入60字，已有长标题编辑体验不一致。

改进：统一业务最大长度并共享校验说明，选择合理的显示截断策略。

验收：同一边界长度在UI与API结果一致，长标题仍可查看完整内容。

<a id="opt-053"></a>

### OPT-053 · 时间格式没有明确跨时区契约

**P2 / 待验证风险**

证据：[frontend/src/utils/display.js:1](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/utils/display.js:1)。定位片段：`export const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 19) : '—'`

影响与边界：直接替换字符串展示无时区时间；浏览器Date、JVM时间和MySQL NOW混用，多时区客户端需验证。

改进：明确活动业务时区，使用带时区时间戳或统一转换，并注入Clock便于测试。

验收：中国与其他时区浏览器展示一致业务时刻，截止边界和数据库状态相符。

<a id="opt-054"></a>

### OPT-054 · 活动表单校验主要靠统一toast

**P3 / 静态确认**

证据：[frontend/src/views/activity/ActivityEditView.vue:91](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/views/activity/ActivityEditView.vue:91)。定位片段：`ElMessage.warning('请填写完整必填项')`

影响与边界：字段多时不知道哪项有问题，也没有自动聚焦首个错误。

改进：使用字段级rules、行内错误和首错聚焦，保留后端最终校验。

验收：键盘可定位并修复错误，每项限制在输入处可见。

<a id="opt-055"></a>

### OPT-055 · 前端API契约缺少静态类型约束

**P3 / 工程优化**

证据：[frontend/src/api/index.js:39](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/src/api/index.js:39)。定位片段：`export const activityApi = {`

影响与边界：页面依赖data.activity/records等隐式结构，字段改名往往到运行时才暴露。

改进：逐步引入TypeScript或JSDoc+checkJs，并生成/维护接口类型。

验收：故意更改关键响应字段后类型检查失败，类型检查接入CI。

<a id="opt-056"></a>

### OPT-056 · 主入口和图表产物仍超过500KB

**P2 / 实测确认**

证据：[frontend/vite.config.js:6](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/vite.config.js:6)。定位片段：`plugins: [vue()],`

影响与边界：本地dist主入口690389字节、统计页520286字节，均为未压缩大小；这不等于已测得首屏慢。

改进：先测资源瀑布，再按依赖拆包/延迟加载，避免只提高警告阈值。

验收：构建体积与冷启动FCP/LCP有前后对比，主要流程及图表不因拆包报错。

## 可运维与验证

<a id="opt-057"></a>

### OPT-057 · 没有可用的就绪与存活探针

**P1 / 实测确认**

证据：[backend/src/main/java/com/campus/activity/ActivityApplication.java:12](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/ActivityApplication.java:12)。定位片段：`public class ActivityApplication {`

影响与边界：/actuator/health当前HTTP200但业务code50000，不是有效健康检查；同样无可用Prometheus端点。

改进：提供受控liveness/readiness，ready验证数据库等必需依赖并使用正确HTTP状态。

验收：数据库故障时readiness失败，存活探针不导致无谓重启；部署检查解析真实结果。

<a id="opt-058"></a>

### OPT-058 · 未知资源被全局异常包装成HTTP200系统错误

**P1 / 实测确认**

证据：[backend/src/main/java/com/campus/activity/common/GlobalExceptionHandler.java:34](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/common/GlobalExceptionHandler.java:34)。定位片段：`@ExceptionHandler(Exception.class)`

影响与边界：两个不存在的探针地址均返回HTTP200/code50000，curl -f或仅看200会误判成功。

改进：分别处理404、参数、业务与系统错误，明确HTTP与业务码契约并逐步兼容前端。

验收：不存在路径返回404；服务器故障5xx；有效业务成功2xx；测试不能只断言200。

<a id="opt-059"></a>

### OPT-059 · 请求缺少贯通日志与响应的追踪ID

**P2 / 工程优化**

证据：[backend/src/main/java/com/campus/activity/common/GlobalExceptionHandler.java:36](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/backend/src/main/java/com/campus/activity/common/GlobalExceptionHandler.java:36)。定位片段：`log.error("系统异常", e);`

影响与边界：用户报错难以对应具体请求、审计与数据库问题，日志排查依赖时间猜测。

改进：过滤器生成/校验requestId，写入MDC与响应头，异常和审计关联同一ID。

验收：可从一个响应ID检索完整请求日志，不把令牌和手机号当追踪标识。

<a id="opt-060"></a>

### OPT-060 · 后端文件日志没有随部署提供轮转策略

**P2 / 静态确认**

证据：[deploy/activity-system.service:19](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/deploy/activity-system.service:19)。定位片段：`StandardOutput=append:/opt/activity-system/logs/backend.log`

影响与边界：backend.log持续追加，运行时间长后占满磁盘的风险未受模板治理。

改进：使用journald限额或logrotate，设保留期、压缩和磁盘告警。

验收：达到设定大小后轮转，保留期到期清理，不丢失当前写入文件。

<a id="opt-061"></a>

### OPT-061 · 部署包没有数据库与上传文件恢复演练入口

**P1 / 工程优化**

证据：[deploy/setup.sh:60](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/deploy/setup.sh:60)。定位片段：`echo "==> [5/10] 建库并导入 init.sql（幂等）"`

影响与边界：现有部署脚本可建库启动，但目录中没有定时备份/恢复验证流程；不能据此假设宿主机无外部备份。

改进：定义数据库和文件一致性备份、加密保留及隔离恢复演练。

验收：从备份恢复到新环境，校验组织、活动、报名、审批和文件引用，记录RPO/RTO。

<a id="opt-062"></a>

### OPT-062 · CI只构建和跑单元测试，未执行真实数据库接口回归

**P2 / 静态确认**

证据：[.github/workflows/ci.yml:18](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/.github/workflows/ci.yml:18)。定位片段：`- run: mvn -B -f backend/pom.xml verify`

影响与边界：组织隔离、锁竞争和迁移依赖MySQL，现有CI不会自动运行docs/testing的接口脚本。

改进：CI启动隔离MySQL，执行迁移、启动应用并跑接口回归，保留失败日志。

验收：故意破坏隔离查询或迁移后CI失败；测试库与用户业务库明确隔离。

<a id="opt-063"></a>

### OPT-063 · 前端没有自动化交互和质量检查入口

**P2 / 静态确认**

证据：[frontend/package.json:9](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/frontend/package.json:9)。定位片段：`"build": "vite build",`

影响与边界：scripts只有dev/build/preview，构建不能检查点击流程、窄屏、可访问性或规则风格。

改进：补充组件/E2E关键路径、lint和类型检查，并在CI分层执行。

验收：登录、创建审核发布、报名签到及403/404关键路径自动验收，覆盖桌面和窄屏。

<a id="opt-064"></a>

### OPT-064 · 部分接口回归固定指向8080且没有测试环境围栏

**P1 / 静态确认**

证据：[docs/testing/platform.mjs:2](C:/Users/33719/Desktop/Xidian_ProgrammingDesigning-main/docs/testing/platform.mjs:2)。定位片段：`const base='http://127.0.0.1:8080',tag='PLAT_'+Date.now(),results=[],orgs=[],accounts=[],activities=[];`

影响与边界：脚本会创建、停用账号和组织、逻辑删除活动；若8080被指向业务实例，会在业务库产生测试数据。

改进：所有脚本统一测试BASE、专用账号、隔离schema和测试标记握手；默认拒绝非测试环境。

验收：连接普通业务实例时拒绝写入，隔离测试环境可完整执行并输出清理清单。

## 建议推进顺序

1. 先修身份凭据、导出令牌、跨域预检、账号事务和HTTP状态，逐项补回归证据。
2. 建立迁移版本、测试环境围栏、HTTPS、恢复和健康检查，再验证部署升级。
3. 在隔离环境测量名单、导出、签到行锁与状态扫描后按瓶颈优化，不预先宣称容量。
4. 补齐审批快照、编辑版本和幂等，再做前端错误态、流程文案及交互验收。
5. 将数据库接口测试与前端自动化纳入CI，再逐项关闭清单。

每项关闭必须记录实际变更、测试命令、结果与剩余限制；本清单不是已修复清单，也不保证穷尽所有问题。
