# Vineflower 1.10.1 基准反编译输出（2026-09-01）

原 jar: ~/Downloads/age_of_empires_ii_240x320-9174.jar（2005-09-09）。
重新生成:
  $JAVA_HOME/bin/java -jar tools/vineflower-1.10.1.jar \
    ~/Downloads/age_of_empires_ii_240x320-9174.jar decompiled-vf
（生成后删掉 META-INF/res/*.mid 等资源，只留 java。）

用途: CFR 的"对照oracle"。CFR 0.152 在本项目有一处已证实的静默控制流丢失
（aimProjectiles 待瞄准扫描的循环出口被渲染成 body 末尾裸 continue，
见 DEVELOPMENT.md 卡死修复条目与 docs/game-mechanics.md 投射物节）；
Vineflower 对同一处渲染正确（if (--n<=0) break;），且 body 末尾 continue
 census 为 0（CFR 9 处、Procyon 0.6.0 24 处）。凡 src 里控制流可疑，
先来本树查同方法的 Vineflower 渲染，再用 javap 对照原 jar 仲裁。
方法级控制流计数对比: CFR↔VF 70/207 个方法有风格差（while↔for、continue↔
break 等），抽样（含地图生成 d.e、脚本解释器大 switch）均为等价渲染。
