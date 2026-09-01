#!/bin/bash
# CFR 编译修复清单(所有修改仅作用于 /tmp/decomp-study/dec-cfr 副本)
set -e
cd /tmp/decomp-study
# F1 [机械] CFR 把同包静态成员写成 "AgeOfEmpires.b.x"/"AgeOfEmpires.c.x",与 package/class 同名冲突
#     去掉 "AgeOfEmpires." 前缀即合法。b.java 55 处、c.java 31+8 处、d.java 4+10 处
sed -i '' 's/AgeOfEmpires\.b\./b./g; s/AgeOfEmpires\.c\./c./g' dec-cfr/AgeOfEmpires/b.java dec-cfr/AgeOfEmpires/c.java dec-cfr/AgeOfEmpires/d.java
# F2 [语义修复=CFR字段误解析] c.java/AgeOfEmpires.java 把继承静态 mad.a.a(:Lcom/ulysseo/mad/b;)
#     误解析为 c 自己的实例字段 a(:LAgeOfEmpires/AgeOfEmpires; MIDlet),并用 (Object) 强转掩盖。
#     按原字节码改回继承静态字段(var_com_ulysseo_mad_b_a),共 10 个使用点
perl -0pi -e 's/\(\(com\.ulysseo\.mad\.b\)\(\(Object\)var_AgeOfEmpires_AgeOfEmpires_a\)\)/var_com_ulysseo_mad_b_a/g' dec-cfr/AgeOfEmpires/c.java
perl -0pi -e 's/\(\(com\.ulysseo\.mad\.b\)\(\(Object\)c\.var_AgeOfEmpires_AgeOfEmpires_a\)\)/c.var_com_ulysseo_mad_b_a/g' dec-cfr/AgeOfEmpires/AgeOfEmpires.java
perl -0pi -e 's/var_AgeOfEmpires_AgeOfEmpires_a\.(getWidth|getHeight|setFullScreenMode|isShown|setCommandListener)/var_com_ulysseo_mad_b_a.$1/g' dec-cfr/AgeOfEmpires/c.java
# F3 [局部合并不可编译] c.java:1514 "Object object" 被复用为 byte[] 与类 a 实例,object[0] 非法。
#     拆出一个 byte[] 局部变量(字节码层面恢复原始双槽)
sed -i '' 's/        object = com\.ulysseo\.mad\.c\.byte_arr_a(this\.aF);/        byte[] objectByteArray = com.ulysseo.mad.c.byte_arr_a(this.aF);/; s/int n2 = object\[0\]/int n2 = objectByteArray[0]/; s/int n3 = object\[1\]/int n3 = objectByteArray[1]/' dec-cfr/AgeOfEmpires/c.java
echo "fix-cfr.sh done"
# F4 [旧编译器宽松→现代javac报错] byte/short 的取负/赋值精度:原源码疑无 cast(2005 javac 接受),
#     现代 javac 报 lossy;补显式 cast。若原字节码无 i2b/i2s 会引入 1 条指令噪声(裁决时甄别)
sed -i '' 's/this\.var_short_arr_arr_a\[1\]\[n6 + 2\] = n3;/this.var_short_arr_arr_a[1][n6 + 2] = (short)n3;/; s/this\.var_short_arr_arr_a\[1\]\[n6 + 2\] = n2;/this.var_short_arr_arr_a[1][n6 + 2] = (short)n2;/; s/this\.var_byte_arr_a\[n5\] = -this\.var_byte_arr_a\[n5\];/this.var_byte_arr_a[n5] = (byte)(-this.var_byte_arr_a[n5]);/; s/            by = -by;/            by = (byte)(-by);/' dec-cfr/AgeOfEmpires/c.java
# F5 [CFR表达式乱序,语义失真+不可编译] c.java boolean_b(II)(原 b(II)Z):CFR 文本先算
#     n22=n5+g[by+1](用旧 by)再算 n21=n3+g[by=g[n20++]];原字节码顺序 by -> n21 -> n22。
#     用 scripts/_f5.py 重排恢复原顺序
python3 scripts/_f5.py
