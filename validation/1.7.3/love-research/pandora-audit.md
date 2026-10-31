# 爱恋共享适配审计（Pandora 2.4.26）

本表针对项目内 `curseofpandora-2.4.26.jar`、内嵌 Pandora 0.1.10、L2Complements 2.6.1 / L2DamageTracker 0.4.4 及当前祝福源码。不是宣称任意第三方饰品均可安全执行。

## 接口及安全边界

- `curios:pandora_charm` 递归展开共 **84 个物品 ID**：80 个 CoP 物品、4 个可选 L2Hostility 掠夺护符。七祝福是七诅咒物品的独立 NBT 状态，另行识别。
- 普通 `AttributeItem.getAttributeModifiers`、`StabilityCharm.getAttributeModifiers` 是通用属性数据。由 `LoveSharing` 读取隔离快照并按变化更新女仆属性；不修改主人、原始 ItemStack 或实际女仆背包。
- 七诅咒 `ISlotAdderItem.getSlotAdder()` 暴露的 `AttrAdder` 可安全读取；过滤 `SlotAdder`，保留现实指数及魔力耐性。
- `ITokenProviderItem.curioTick` 仅为 Player 创建 ConditionalData；`BaseTickingToken`、`IAttackListenerToken` 所有签名均使用 Player，`PandoraAttackListener` 也只分发玩家。**不能通过直接调用、类型强转或假玩家重放实现共享。** 本适配将通用部分写为 LivingEntity 方法，使用原配置与原 L2 攻防阶段。
- 复杂状态保留在女仆实例对应的弱引用映射中，不保存主人引用，不新增反射。卸下共享来源后清理本适配自己的属性 UUID，不删除其他模组属性。
- 未审计的第三方饰品不执行 `curioTick/onEquip/onUnequip` 等回调。名单与机制禁用项位于 `LoveSharingPolicy`；其 `isKnown` 表示审计过，不表示该物品每一个玩家专属子功能都迁移了。

## 逐项覆盖

“静态”由 `LoveSharing` 更新；“动态/事件”由 `LovePandoraEffects` 更新；“玩家机制禁用”是明确黑名单，不伪造女仆为玩家。以下 ID 省略 `curseofpandora:`。

| 物品 ID | 中文名 | 共享范围与限制 |
|---|---|---|
| orb_of_stability | 恒静宝珠 | 静态抗击退、L2 STABLE_BODY；玩家屏幕抖动没有女仆客户端对象，禁用该显示机制 |
| golden_heart | 黄金之心 | 猪灵初始敌对选择适配；保留受到攻击后的正常反击 |
| charm_of_calmness | 末影的平静护符 | 末影人凝视敌对选择适配；保留正常反击 |
| bless_of_snow_walker | 细雪行者的传承 | L2 SNOW_WALKER 通用实体特性 |
| bless_of_lava_walker | 熔岩行者的传承 | L2 LAVA_WALKER 通用实体特性 |
| charm_of_night_vision | 夜视护符 | 夜视药水状态；不会把女仆变成有玩家摄像机的实体 |
| orb_of_fire_rejection | 抗火宝珠 | 抗火药水、每 tick 熄火、L2 FIRE_REJECT |
| orb_of_master | 领袖宝珠 | L2 OWNER_PROTECTION 通用实体特性 |
| orb_of_nature | 自然宝珠 | L2 ENVIRONMENTAL_REJECT |
| orb_of_explosion_rejection | 防爆宝珠 | L2 EXPLOSION_REJECT |
| orb_of_projectile_rejection | 偏转宝珠 | L2 PROJECTILE_REJECT |
| orb_of_magic_rejection | 魔免宝珠 | L2 MAGIC_REJECT |
| mini_beacon | 微缩信标 | 原物品是无效果材料，可放入但没有额外效果 |
| charm_of_health | 生命宝珠 | 静态最大生命属性 |
| charm_of_armor | 护甲宝珠 | 静态护甲与护甲韧性属性 |
| charm_of_speed | 速度宝珠 | 静态移速属性 |
| charm_of_damage | 伤害宝珠 | 静态攻击伤害属性 |
| charm_of_critical | 狂暴宝珠 | 静态 L2 暴击伤害属性 |
| charm_of_archery | 射手宝珠 | 静态 L2 弓箭伤害属性 |
| charm_of_protection | 守护宝珠 | 静态 L2 伤害减免属性 |
| charm_of_magic | 魔能宝珠 | 静态 L2 魔法伤害属性 |
| charm_of_explosion | 爆破宝珠 | 静态 L2 爆炸伤害属性 |
| charm_of_heavy_weapon | 重装勋章 | 静态伤害增益及攻速减益，均保留 |
| charm_of_accuracy | 凝神宝珠 | 静态暴击率增益及攻速减益，均保留 |
| charm_of_luck | 幸运护符 | 幸运属性；不伪造玩家钓鱼或玩家战利品上下文 |
| orb_of_soulguard | 守灵人宝珠 | 静态移速减益、减伤、魔力耐性 |
| orb_of_executor | 处刑人宝珠 | 静态伤害增益与生命减益 |
| orb_of_prosecutor | 审判官宝珠 | 静态暴击伤害增益与暴击率减益 |
| angelic_wing | 天使之翼 | 原现实指数/露天条件、跌落与撞墙免伤；实体实际处于滑翔时加速，不创建玩家飞行控制 |
| angelic_bless | 天使祝福 | 原现实指数/露天条件、L2 伤害吸收及减伤属性 |
| angelic_descent | 天使神降 | 原现实指数/露天条件、攻击伤害属性、对亡灵增伤 |
| angelic_protection | 天使加护 | 满血保命、清除状态；女仆独立冷却，不访问 Player ItemCooldowns |
| angelic_punishment | 神罚之印 | 解除天使套露天限制、按目标当前生命的伤害下限，原配置冷却 |
| hellfire_skull | 燃魂颅骨 | 攻击使目标灵魂火焰升级，原持续时长与现实指数上限 |
| hellfire_reformation | 浴火塑形 | 免疫灵魂火焰伤害，将自身灵魂火焰传给被击目标 |
| eye_of_cursed_souls | 咒怨之眼 | 攻击时使附近敌方怪物受到以其自身为来源的灵魂伤害，清除当前仇恨；有独立冷却 |
| cursed_soul_crystal | 死灵魂核 | 将来自非亡灵 Mob 的可防御伤害转移到附近亡灵；女仆独立冷却，延后伤害避免破坏 L2 攻击栈 |
| crown_of_demon | 巫妖之证 | 原基础生命阈值的亡灵不主动攻击女仆；女仆攻击时驱使空闲亡灵攻击目标；战斗查询每秒最多一次 |
| shadow_core | 虚影核心 | 给命中目标施加原虚影效果，持续时间按现实指数 |
| shadow_convergence | 虚影汇聚 | 对虚影目标造成伤害后治疗女仆 |
| shadow_consolidation | 虚影化形 | 记录命中位置与最终伤害，原延迟/范围/冷却结算影爆；同位置聚合，排除主人及友军 |
| shadow_reformation | 虚影重塑 | 对虚影目标魔法增伤；受虚影敌人非魔法伤害时减伤 |
| void_overflow | 虚空溢散 | 将影爆来源切换为原虚空伤害类型 |
| abyssal_treasure | 深渊秘藏 | 按女仆所处深度提升幸运属性 |
| abyssal_watcher | 深渊活体 | 按女仆深度每秒回血 |
| abyssal_shell | 深渊甲壳 | 按女仆深度提升护甲/韧性 |
| abyssal_crown | 深渊王冠 | 女仆近战伤害来源按深度概率获得原 BYPASS_MAGIC 标记 |
| abyssal_will | 深渊意志 | 改善深度步长；原地下/穿透伤害条件下保命并给予原深渊庇护；独立冷却 |
| barbaric_instinct | 蛮荒本能 | 魔法伤害减益；持武器完成女仆近战攻击后回血，独立冷却；女仆没有玩家蓄力条，不访问玩家蓄力缓存 |
| barbaric_wrath | 蛮荒暴戾 | 魔法伤害减益、暴击伤害和韧性增益 |
| barbaric_roar | 蛮荒恐吓 | 魔法伤害减益、攻击伤害和减伤增益 |
| barbaric_blade | 蛮荒凶器 | 魔法伤害减益、近战与弓箭伤害增益 |
| primal_force | 原始之力 | 魔法伤害减益、自身按最大生命加甲；攻击女仆的 Mob 同样按原规则获得护甲 |
| evil_spirit_ritual | 邪祟秘法 | **玩家经验掉落回调禁用**，不将女仆的全部击杀全局改成主人击杀 |
| evil_spirit_evoke | 邪祟唤魔 | **玩家灵体所有权机制禁用**；原 EvilSpirit(Player)、setOwner(Player) 无安全女仆等价，不伪造主人或生成假玩家 |
| evil_spirit_awakening | 邪祟觉醒 | 女仆击杀触发自身临时层数与魔法/减伤增益；不写主人层数 |
| evil_spirit_curse | 邪祟咒灭 | 对低血量目标的魔法增伤 |
| evil_spirit_walk | 祟神附体 | 原魔法/近战增益及状态，致死保命与药水；女仆独立冷却 |
| wind_thrust | 疾风刺 | 女仆实际疾跑时增伤/加速；**客户端自动攻击输入禁用** |
| earth_crush | 磐岩坠 | 原攻速阈值下增加暴击伤害 |
| flaming_explosion | 烈炎爆 | 原现实指数条件的爆炸伤害属性 |
| waving_spell | 涌潮咒 | 女仆处于水/气泡时的魔法伤害属性 |
| curse_redirection | 逆魔符 | 根据女仆自己盔甲的诅咒附魔种类计算魔法加成和魔力耐性 |
| seal_of_swords | 破境剑符 | **玩家难度能力禁用**，不在女仆身上读取/创建 PlayerDifficulty |
| spell_singularity | 魔能奇点 | 根据女仆自己各件装备附魔种类与现实指数计算魔法/魔耐 |
| mini_beacon_speed | 微缩信标：速度 | 速度 II |
| mini_beacon_haste | 微缩信标：急迫 | 急迫 II；仅影响女仆任务本来支持的挖掘流程 |
| mini_beacon_strength | 微缩信标：力量 | 力量 II |
| mini_beacon_jump | 微缩信标：跳跃提升 | 跳跃提升 II |
| mini_beacon_resistance | 微缩信标：抗性提升 | 抗性提升 II |
| mini_beacon_regen | 微缩信标：再生 | 再生 II |
| curse_of_inertia | 囚笼 | 现实/魔耐；攻速正增益抑制、上限和低攻速补偿；玩家槽位禁用 |
| curse_of_proximity | 制限 | 现实/魔耐；触及属性正增益抑制、上限与低触及补偿；不替换女仆自身 AI 攻击距离算法；玩家槽位禁用 |
| curse_of_flesh | 血肉 | 现实/魔耐、最大生命正增益抑制；**玩家饱食持续奖励及槽位禁用** |
| curse_of_metabolism | 羸弱 | 现实/魔耐；**Player FoodData 饱食/饥饿驱动的增减益禁用** |
| curse_of_tension | 恐惧 | 现实/魔耐，女仆自己的恐惧/勇气标记及攻防结算；玩家槽位与仅玩家可见的伪药水 HUD 同步禁用 |
| curse_of_prudence | 碎魂 | 现实/魔耐，女仆对同一目标短期连续伤害衰减及单次伤害上限；玩家槽位/经验吸收/反转禁用 |
| curse_of_spell | 咒缚 | 现实/魔耐，根据女仆自身装备计算附魔过载、自损、受伤增加和伤害削弱；玩家槽位/随机反转禁用 |
| illusory_affection | 伪善 | 静态现实指数代价；**神秘遗物玩家戒指仇恨处理禁用** |
| fading_dream | 幻梦 | 静态现实指数代价；**神秘遗物玩家睡眠/时长统计处理禁用** |
| l2hostility:charm_of_looting_1 | 掠夺护符 1 | **玩家战利品上下文禁用** |
| l2hostility:charm_of_looting_2 | 掠夺护符 2 | **玩家战利品上下文禁用** |
| l2hostility:charm_of_looting_3 | 掠夺护符 3 | **玩家战利品上下文禁用** |
| l2hostility:charm_of_looting_4 | 掠夺护符 4 | **玩家战利品上下文禁用** |

七祝福：由现有 `BlessingBenefits` 的 LivingEntity 伤害/属性逻辑接入共享身份。现实/魔耐安全共享；第一、第二的属性条件、第五/第六战斗叠层、第七按女仆自身装备加共享串饰的有效附魔等级减伤；第三、第四饱食条件、所有玩家扩槽、自动反转/经验吸收/成就/UI 禁用。既有祝福的主人鉴权在读取主人快照时完成，不给女仆改写珠子所有者。

## 性能与生命周期

- LoveSharing 快照提供常数时间成员判断，攻击回调不再扫描 Curios、容器或主人的整个背包。
- 药水刷新错峰每秒一次；灭火及确实需要的动态属性条件继续按 tick 处理。
- 装备附魔按装备变化事件失效缓存；魔耐改变仅重算小型缓存数组，无每 tick NBT 扫描。
- 属性数值不变时不 remove/add；只操作本适配独立 UUID，静态属性仅在快照变化时处理。
- 巫妖随从列表每秒最多查询一次且只在战斗触发；咒怨/转移伤害仅在事件及冷却满足时查询；影爆仅到期查询一次。
- 女仆保命与伤害转移冷却在临时卸下爱恋时保留，避免重新佩戴重置。实体弱引用状态、服务器停止清理；死亡不保留当前战斗状态。
- 本文件为源码审计与实现清单。最终编译/运行测试结果由上级任务的 `validation/1.7.3/results/love` 报告记录；不要将源码审计替代游戏实测结论。
