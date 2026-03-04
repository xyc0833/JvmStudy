## 笔记

（线程独有）程序计数器：保存当前程序的执行位置。

（线程独有）虚拟机栈：通过栈帧来维持方法调用顺序，帮助控制程序有序运行。

（线程独有）本地方法栈：同上，作用与本地方法。

堆：所有的对象和数组都在这里保存。

方法区：类信息、即时编译器的代码缓存、运行时常量池。


![img.png](img.png)

1.7之前指向的是方法区中同一个地址,1.8之后指向的是堆中同一个地址


-Xms1M -Xmx1M -XX:+HeapDumpOnOutOfMemoryError


java_pid66250.hprof 堆转储文件

## 反射相关复习

```java
public static void main(String[] args) throws IllegalAccessException {
    Field unsafeField = Unsafe.class.getDeclaredFields()[0];
    unsafeField.setAccessible(true);
    Unsafe unsafe = (Unsafe) unsafeField.get(null);
}
```

你这段代码是 Java 反射中**获取并使用 Unsafe 类**的经典片段，核心是突破 Java 访问权限限制拿到 Unsafe 实例。我会逐行拆解，让你清楚每一步的作用和背后的原理。

### 代码整体功能总结
这三行代码通过反射绕过 Java 的访问控制，强行获取 `sun.misc.Unsafe` 类的实例——这个类是 Java 中能直接操作内存、调用底层方法的“危险工具”，本身被设计为仅能由 JDK 内部使用，外部无法直接实例化。

### 逐行详细解释

#### 第一行：`Field unsafeField = Unsafe.class.getDeclaredFields()[0];`
```java
Field unsafeField = Unsafe.class.getDeclaredFields()[0];
```
- **核心作用**：获取 `Unsafe` 类中声明的**第一个成员变量**，并封装成 `Field` 对象。
- 关键知识点拆解：
    1. `Unsafe.class`：通过类字面量获取 `Unsafe` 类的 `Class` 对象（反射的入口，所有反射操作都基于 `Class` 对象）。
    2. `getDeclaredFields()`：获取该类**所有声明过的成员变量**（包括 `private`/`protected`/`public`，但不包括父类继承的），返回 `Field[]` 数组。
    3. `[0]`：取数组第一个元素——因为 `Unsafe` 类的源码中，唯一的实例是一个 `private static final Unsafe theUnsafe` 变量，且是该类第一个声明的字段，所以通过下标 0 能拿到它。
- 注意：这行代码有个小风险——如果 JDK 版本变更导致 `Unsafe` 类的字段顺序变化，`[0]` 可能取不到正确的字段（更严谨的写法是通过字段名 `getDeclaredField("theUnsafe")`）。

#### 第二行：`unsafeField.setAccessible(true);`
```java
unsafeField.setAccessible(true);
```
- **核心作用**：关闭 Java 访问权限检查，允许通过反射访问这个 `private` 修饰的字段。
- 关键知识点：
    1. `Field` 类的 `setAccessible(boolean)` 是反射突破访问权限的核心方法——默认情况下，`private` 字段不允许外部访问，调用 `setAccessible(true)` 后，JVM 会跳过访问修饰符的检查。
    2. 这是反射“打破封装”的关键一步，也是 Unsafe 能被外部获取的核心原因。

#### 第三行：`Unsafe unsafe = (Unsafe) unsafeField.get(null);`
```java
Unsafe unsafe = (Unsafe) unsafeField.get(null);
```
- **核心作用**：获取该字段的值，并强转为 `Unsafe` 类型，拿到实例。
- 关键知识点拆解：
    1. `Field.get(Object obj)`：获取指定对象 `obj` 上该字段的值。
    2. 传 `null` 的原因：因为我们要获取的 `theUnsafe` 是 **static 静态字段**——静态字段属于类本身，而非类的实例，所以用 `null` 代替实例对象即可。
    3. 强转 `(Unsafe)`：因为 `get()` 方法返回值是 `Object` 类型，需要强转为具体的 `Unsafe` 类型。

### 补充说明（帮你理解更透彻）
1. 为什么要这么做？  
   `Unsafe` 类的构造方法是 `private`，且没有公开的创建实例的方法（JDK 设计为仅内部使用），所以只能通过反射获取其静态的 `theUnsafe` 实例。
2. 更严谨的写法（避免字段下标问题）：  
   你原代码用 `[0]` 依赖字段顺序，更稳定的写法是通过字段名获取：
   ```java
   Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe"); // 直接指定字段名
   unsafeField.setAccessible(true);
   Unsafe unsafe = (Unsafe) unsafeField.get(null);
   ```

### 总结
1. 第一行：通过反射获取 `Unsafe` 类中名为 `theUnsafe` 的静态字段（靠下标 `[0]`）；
2. 第二行：关闭访问权限检查，允许操作这个 `private` 字段；
3. 第三行：获取静态字段的值（传 `null`），强转后拿到 `Unsafe` 实例。

核心逻辑是：利用反射突破 `private` 访问限制，获取 JDK 隐藏的 `Unsafe` 类实例。


你现在想深入理解 `Field` 类，这是 Java 反射中操作类成员变量的核心工具，我会从基础到应用，用通俗易懂的方式帮你讲清楚。

### 核心定义
`Field` 是 `java.lang.reflect` 包下的类，它的核心作用是：**封装 Java 类中一个成员变量（字段）的所有信息，并提供对这个字段的读写操作能力**。

简单来说，一个 `Field` 对象就代表类里的一个具体字段（比如 `private String name`、`public static int age`），通过它你可以：
- 查看字段的修饰符（`public`/`private`/`static`/`final` 等）、类型、名称；
- 突破访问权限限制，读写任意对象的该字段值（哪怕是 `private` 字段）。

### 关键特性 & 常用方法
先结合你之前的 Unsafe 代码，再扩展核心方法，帮你对应理解：

| 方法 | 作用 | 对应你代码中的使用 |
|------|------|--------------------|
| `setAccessible(true)` | 关闭访问权限检查，允许操作 `private`/`protected` 字段 | 你代码中用来操作 `Unsafe` 的 `private static final theUnsafe` 字段 |
| `get(Object obj)` | 获取 `obj` 对象上该字段的值；如果是静态字段，`obj` 传 `null` | 你代码中 `unsafeField.get(null)` 就是获取静态字段的值 |
| `set(Object obj, Object value)` | 给 `obj` 对象的该字段设置 `value`；静态字段 `obj` 传 `null` | 比如 `field.set(user, "张三")` 给 user 对象的 name 字段赋值 |
| `getName()` | 获取字段的名称（字符串） | `unsafeField.getName()` 会返回 "theUnsafe" |
| `getType()` | 获取字段的类型（返回 `Class` 对象） | `unsafeField.getType()` 会返回 `Unsafe.class` |
| `getModifiers()` | 获取字段的修饰符（返回 int，需配合 `Modifier` 类解析） | 可判断字段是否是 `static`/`final`/`private` 等 |

### 实战示例（帮你理解）
用一个简单的类演示 `Field` 的核心用法，比单纯讲概念更易理解：

```java
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

// 定义一个普通类
class User {
    // 不同修饰符的字段
    private String name;
    public static int age;

    public User(String name) {
        this.name = name;
    }
}

public class FieldDemo {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        // 1. 获取 Class 对象（反射入口）
        Class<User> userClass = User.class;

        // 2. 获取指定字段（private 字段）
        Field nameField = userClass.getDeclaredField("name");
        // 3. 突破访问限制
        nameField.setAccessible(true);

        // 4. 创建对象实例
        User user = new User("原始名称");
        // 5. 读取字段值
        String nameValue = (String) nameField.get(user);
        System.out.println("读取 private 字段：" + nameValue); // 输出：原始名称

        // 6. 修改字段值
        nameField.set(user, "修改后的名称");
        System.out.println("修改后的值：" + nameField.get(user)); // 输出：修改后的名称

        // ---------------------- 操作静态字段 ----------------------
        // 获取静态字段
        Field ageField = userClass.getDeclaredField("age");
        // 静态字段无需 setAccessible（public），直接操作
        // 读取静态字段：obj 传 null
        System.out.println("静态字段初始值：" + ageField.get(null)); // 输出：0
        // 修改静态字段
        ageField.set(null, 20);
        System.out.println("静态字段修改后：" + ageField.get(null)); // 输出：20

        // ---------------------- 查看字段信息 ----------------------
        // 获取字段修饰符（解析 int 类型的修饰符）
        int modifiers = nameField.getModifiers();
        System.out.println("name 字段是否是 private：" + Modifier.isPrivate(modifiers)); // true
        System.out.println("name 字段的类型：" + nameField.getType().getName()); // java.lang.String
    }
}
```

### 关键注意点（避坑）
1. **getDeclaredField vs getField**：
    - `getDeclaredField("字段名")`：获取类自身声明的所有字段（包括 private/protected/public），但不包括父类的；
    - `getField("字段名")`：只能获取类的 **public 字段**（包括父类继承的 public 字段）。
      你之前的 `Unsafe.class.getDeclaredFields()[0]` 用的是 `getDeclaredFields()`（复数），返回所有声明的字段数组。

2. **静态字段 vs 实例字段**：
    - 实例字段（非 static）：操作时必须传具体的对象实例（比如 `nameField.get(user)`）；
    - 静态字段（static）：操作时传 `null` 即可（比如 `ageField.get(null)`），因为静态字段属于类，而非对象。

3. **异常处理**：
   操作 `Field` 时常见异常：
    - `NoSuchFieldException`：找不到指定名称的字段；
    - `IllegalAccessException`：没调用 `setAccessible(true)` 就操作 private 字段；
    - `NullPointerException`：操作实例字段时传了 `null`。

### 总结
1. `Field` 是反射中**操作类成员变量**的核心类，一个 `Field` 对象对应类的一个具体字段；
2. 核心能力是：突破访问权限（`setAccessible(true)`）、读写字段值（`get()`/`set()`）、查看字段元信息（名称、类型、修饰符）；
3. 区分 `getDeclaredField`（自身所有字段）和 `getField`（仅 public 字段），以及静态/实例字段的操作差异。

简单记：`Field` 就是反射给你的“字段操作工具”，能让你在运行时动态操作类的任意字段，哪怕是私有的。


## JNI 调用本地方法

Java还有一个JNI机制，它的全称：Java Native Interface，即Java本地接口。
它允许在Java虚拟机内运行的Java代码与其他编程语言（如C/C++和汇编语言）编写的程序和库进行交互
（在Android开发中用得比较多）比如我们现在想要让C语言程序帮助我们的Java程序实现a+b的运算