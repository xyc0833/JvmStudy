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

-XX:+PrintGCDetails 

[0.004s][warning][gc] -XX:+PrintGCDetails is deprecated. Will use -Xlog:gc* instead.
[0.016s][info   ][gc,init] CardTable entry size: 512
[0.016s][info   ][gc     ] Using G1
[0.019s][info   ][gc,init] Version: 21.0.10+7-LTS (release)
[0.019s][info   ][gc,init] CPUs: 4 total, 4 available
[0.019s][info   ][gc,init] Memory: 16384M
[0.019s][info   ][gc,init] Large Page Support: Disabled
[0.019s][info   ][gc,init] NUMA Support: Disabled
[0.019s][info   ][gc,init] Compressed Oops: Enabled (Zero based)
[0.019s][info   ][gc,init] Heap Region Size: 2M
[0.019s][info   ][gc,init] Heap Min Capacity: 8M
[0.019s][info   ][gc,init] Heap Initial Capacity: 256M
[0.019s][info   ][gc,init] Heap Max Capacity: 4G
[0.019s][info   ][gc,init] Pre-touch: Disabled
[0.019s][info   ][gc,init] Parallel Workers: 4
[0.019s][info   ][gc,init] Concurrent Workers: 1
[0.019s][info   ][gc,init] Concurrent Refinement Workers: 4
[0.019s][info   ][gc,init] Periodic GC: Disabled
[0.033s][info   ][gc,metaspace] CDS archive(s) mapped at: [0x0000000195000000-0x0000000195cf7000-0x0000000195cf7000), size 13594624, SharedBaseAddress: 0x0000000195000000, ArchiveRelocationMode: 1.
[0.033s][info   ][gc,metaspace] Compressed class space mapped at: 0x0000000196000000-0x00000001d6000000, reserved size: 1073741824
[0.033s][info   ][gc,metaspace] Narrow klass base: 0x0000000195000000, Narrow klass shift: 0, Narrow klass range: 0x100000000
123123
[0.400s][info   ][gc,heap,exit] Heap
[0.401s][info   ][gc,heap,exit]  garbage-first heap   total 264192K, used 3853K [0x0000000700000000, 0x0000000800000000)
[0.402s][info   ][gc,heap,exit]   region size 2048K, 2 young (4096K), 0 survivors (0K)
[0.402s][info   ][gc,heap,exit]  Metaspace       used 768K, committed 896K, reserved 1114112K
[0.402s][info   ][gc,heap,exit]   class space    used 64K, committed 128K, reserved 1048576K


## git 大文件 解决方案 

git filter-repo --invert-paths --path-match java_pid66250.hprof --force 


scavenge /ˈskævɪndʒ/
本义：（动物）捡食腐肉；（人）捡垃圾、搜寻可用之物
引申：清理废物、回收可用资源


## Runnable

```java
public class Main {
    private static int i = 0;
    public static void main(String[] args) throws InterruptedException {
        Lock testLock = new ReentrantLock();   //可重入锁ReentrantLock类是Lock类的一个实现，我们后面会进行介绍
        Runnable action = () -> {
            for (int j = 0; j < 100000; j++) {   //还是以自增操作为例
                testLock.lock();    //加锁，加锁成功后其他线程如果也要获取锁，会阻塞，等待当前线程释放
                i++;
                testLock.unlock();  //解锁，释放锁之后其他线程就可以获取这把锁了（注意在这之前一定得加锁，不然报错）
            }
        };
        new Thread(action).start();
        new Thread(action).start();
        Thread.sleep(1000);   //等上面两个线程跑完
        System.out.println(i);
    }
}
```
你这段代码里的 `Runnable` 是 Java 中实现多线程的核心接口，这里用了 Lambda 表达式简化写法，我会从「是什么、怎么用、代码里的具体作用」三个层面帮你讲清楚，让你彻底理解这段代码中的 `Runnable` 逻辑。

### 先总结核心作用
代码中的 `Runnable` 本质是一个**任务载体**：它封装了“循环10万次对静态变量 `i` 进行加锁自增”的逻辑，两个线程会各自执行这个任务，最终实现多线程下的安全自增。

---

### 1. Runnable 接口的基础认知
`Runnable` 是 `java.lang` 包下的核心函数式接口，专门用来定义**线程要执行的任务**，核心特点：
- 接口中只有一个抽象方法：`void run()`（无返回值、不抛检查异常）；
- 本身不是线程，只是“待执行的任务”，需要交给 `Thread` 来执行；
- Java 8 后因为是函数式接口，可直接用 Lambda 表达式简化编写（代码里就是这种写法）。

#### 传统写法 vs 代码中的 Lambda 写法（对比更易理解）
你代码里用的是 Lambda 简化版，等价于下面的传统匿名内部类写法，能更直观看到 `Runnable` 的结构：
```java
// 传统匿名内部类写法（和代码中的Lambda完全等价）
Runnable action = new Runnable() {
    @Override
    public void run() {
        for (int j = 0; j < 100000; j++) {
            testLock.lock();
            i++;
            testLock.unlock();
        }
    }
};
```
代码中 `() -> { ... }` 就是 Lambda 对 `Runnable` 的简写：
- `()`：对应 `run()` 方法的空参数列表；
- `->`：Lambda 语法分隔符，左边是参数，右边是方法体；
- `{ ... }`：就是 `run()` 方法的具体执行逻辑。

### 2. 这段代码中 Runnable 的具体执行流程
#### 第一步：定义任务
```java
Runnable action = () -> {
    for (int j = 0; j < 100000; j++) {   
        testLock.lock();    // 加锁，保证同一时间只有一个线程执行i++
        i++;
        testLock.unlock();  // 解锁，释放锁给其他线程
    }
};
```
- 这个 `action` 对象封装了“10万次加锁自增 `i`”的任务逻辑；
- 核心目的：通过 `ReentrantLock` 的加锁/解锁，保证 `i++` 这个非原子操作在多线程下的线程安全。

#### 第二步：将任务交给线程执行
```java
new Thread(action).start();
new Thread(action).start();
```
- `new Thread(action)`：创建线程对象，把 `Runnable` 任务传给线程；
- `start()`：启动线程——JVM 会调用该线程的 `run()` 方法（也就是 `Runnable` 里的逻辑）；
- 这里创建了两个线程，**各自独立执行 `action` 里的 10万次循环**（不是共享循环次数，而是每个线程都跑10万次）。

#### 第三步：线程执行的关键细节
- 两个线程会并发执行，但因为 `testLock.lock()` 的存在，同一时间只有一个线程能进入 `i++` 的执行环节；
- 最终 `i` 的结果理论上是 200000（10万+10万），如果不加锁，结果会小于200000（因为 `i++` 是“读取-修改-写入”三步操作，多线程会出现数据覆盖）。

### 3. 关键注意点（避坑）
1. **Runnable 不是线程**：`action` 只是任务，必须通过 `new Thread(action).start()` 才能启动线程执行；如果直接调用 `action.run()`，不会创建新线程，只是在当前主线程执行逻辑。
2. **锁的作用域**：代码中锁包裹了 `i++`，刚好覆盖非原子操作，是正确的；如果锁加在循环外面（比如 `lock()` 在循环前，`unlock()` 在循环后），会导致一个线程独占锁跑完10万次，另一个线程才执行，失去并发意义。
3. **异常处理**：`Runnable` 的 `run()` 不能抛检查异常（比如 `InterruptedException`），如果任务里有需要捕获的异常，必须在 Lambda 内部用 `try-catch` 处理（代码中 `i++` 无异常，所以没写）。

### 总结
1. 代码中的 `Runnable` 是一个**线程任务载体**，用 Lambda 简化写法封装了“10万次加锁自增 `i`”的逻辑；
2. 两个线程分别执行这个 `Runnable` 任务，通过 `ReentrantLock` 保证 `i++` 的线程安全；
3. `Runnable` 的核心价值是**解耦“任务逻辑”和“线程执行”**，让任务可以复用、线程只负责执行。

简单记：`Runnable` 就是“线程要做的事”，你定义好“事”（Lambda 里的逻辑），交给 Thread 去“做”（start()），多线程就跑起来了。


## 介绍一下 condition


你想了解的 `Condition` 是 Java JUC 包中与显式锁（`Lock`）配合使用的**线程等待/通知机制**，可以理解为 `synchronized` 
搭配 `wait()`/`notify()` 的“升级版”，
我会从「是什么、怎么用、核心优势、实战例子」四个维度帮你讲清楚，结合你之前学的 `ReentrantLock` 更容易理解。

### 先明确核心定义
`Condition`（条件对象）是 `java.util.concurrent.locks` 包下的接口，
必须通过 `Lock` 对象的 `newCondition()` 方法创建，
核心作用是：**让线程在满足特定条件时等待，当条件满足时被唤醒**。

简单类比：
- `synchronized` + `wait()`/`notify()` = 老式的“单条件等待通知”；
- `Lock` + `Condition` = 新式的“多条件精准等待通知”。

### 1. Condition 核心方法（对比 wait/notify 更易理解）
| Condition 方法       | 对应 Object 方法 | 作用说明                                                                 |
|----------------------|------------------|--------------------------------------------------------------------------|
| `await()`            | `wait()`         | 让当前线程等待，释放锁，直到被唤醒或中断                                 |
| `await(long time, TimeUnit unit)` | `wait(long timeout)` | 限时等待，超时自动唤醒                                                   |
| `signal()`           | `notify()`       | 唤醒一个等待在该 Condition 上的线程                                       |
| `signalAll()`        | `notifyAll()`    | 唤醒所有等待在该 Condition 上的线程                                       |

⚠️ 关键注意：
和 `wait()`/`notify()` 一样，调用 `Condition` 的方法前，必须先获取对应的 `Lock` 锁（否则会抛 `IllegalMonitorStateException`）。

### 2. Condition 的核心优势（对比 synchronized + wait/notify）
这是 `Condition` 最核心的价值，也是为什么要引入它：
#### 优势1：支持多条件精准唤醒（最核心）
`synchronized` 只有一个“等待集”，`notify()` 只能随机唤醒一个线程，`notifyAll()` 会唤醒所有线程（无关线程也会被唤醒，造成“惊群效应”）；
`Condition` 可以为一个 `Lock` 创建多个条件对象，实现“按需唤醒”——只有满足特定条件的线程才会被唤醒，避免无效竞争。

#### 优势2：更灵活的等待机制
- 支持限时等待（带时间单位，比 `wait(long)` 更直观）；
- 支持可中断等待（`await()` 响应线程中断，抛出 `InterruptedException`）；
- 还提供 `awaitUninterruptibly()`（不响应中断的等待）、`awaitUntil(Date deadline)`（截止时间等待）等高级方法。

### 3. 实战例子（生产者-消费者模型，体现多条件优势）
用 `ReentrantLock + Condition` 实现经典的生产者-消费者，对比 `synchronized` 能直观看到“精准唤醒”的价值：
```java
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// 生产者-消费者容器
class Buffer {
    private final LinkedList<String> queue = new LinkedList<>();
    private final int MAX_CAPACITY = 5; // 队列最大容量
    private final Lock lock = new ReentrantLock();
    // 两个Condition：空条件（队列空时消费者等待）、满条件（队列满时生产者等待）
    private final Condition emptyCondition = lock.newCondition();
    private final Condition fullCondition = lock.newCondition();

    // 生产方法
    public void produce(String item) throws InterruptedException {
        lock.lock(); // 先获取锁
        try {
            // 队列满时，生产者等待（直到队列有空位）
            while (queue.size() == MAX_CAPACITY) {
                fullCondition.await(); // 生产者在fullCondition上等待
            }
            queue.add(item);
            System.out.println("生产：" + item + "，队列大小：" + queue.size());
            emptyCondition.signal(); // 唤醒等待的消费者（只有消费者会在emptyCondition上等）
        } finally {
            lock.unlock(); // 必须在finally中解锁，避免异常导致锁无法释放
        }
    }

    // 消费方法
    public String consume() throws InterruptedException {
        lock.lock();
        try {
            // 队列空时，消费者等待（直到有产品）
            while (queue.size() == 0) {
                emptyCondition.await(); // 消费者在emptyCondition上等待
            }
            String item = queue.removeFirst();
            System.out.println("消费：" + item + "，队列大小：" + queue.size());
            fullCondition.signal(); // 唤醒等待的生产者（只有生产者会在fullCondition上等）
            return item;
        } finally {
            lock.unlock();
        }
    }
}

public class ConditionDemo {
    public static void main(String[] args) {
        Buffer buffer = new Buffer();

        // 启动3个生产者线程
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 2; j++) {
                        buffer.produce("产品" + j);
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        // 启动2个消费者线程
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 3; j++) {
                        buffer.consume();
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
```

#### 代码关键解读：
1. 创建了两个 `Condition`：
    - `emptyCondition`：队列空时，消费者在这个条件上等待；
    - `fullCondition`：队列满时，生产者在这个条件上等待。
2. 精准唤醒：
    - 生产者生产后，只唤醒等待的消费者（`emptyCondition.signal()`），不会唤醒其他生产者；
    - 消费者消费后，只唤醒等待的生产者（`fullCondition.signal()`），不会唤醒其他消费者。
3. 安全保障：
    - 用 `while` 循环检查条件（而非 `if`），避免“虚假唤醒”（线程被唤醒后需重新检查条件）；
    - 解锁操作放在 `finally` 中，确保无论是否抛异常，锁都会释放。

### 4. 核心注意事项
1. **必须先加锁**：调用 `await()`/`signal()` 前，必须先获取 `Lock` 锁，否则抛 `IllegalMonitorStateException`；
2. **避免虚假唤醒**：永远用 `while` 循环检查条件（`while (条件不满足) { await(); }`），而非 `if`；
3. **解锁时机**：解锁操作必须放在 `finally` 块中，防止异常导致锁泄露；
4. **和 Lock 绑定**：一个 `Condition` 只能和创建它的 `Lock` 配合使用，不能跨 Lock 使用。

### 总结
1. `Condition` 是 JUC 中与 `Lock` 配合的条件等待/通知接口，替代 `synchronized` 的 `wait()`/`notify()`；
2. 核心优势是**多条件精准唤醒**，避免惊群效应，提升并发效率；
3. 核心方法：`await()`（等待）、`signal()`（唤醒单个）、`signalAll()`（唤醒全部），使用前必须先获取锁；
4. 典型场景：生产者-消费者、线程池任务等待等需要精准控制线程等待/唤醒的场景。

简单记：`Condition` 就是给 `Lock` 锁配的“精准唤醒开关”，一个锁可以配多个开关，想唤醒哪类线程就开哪个开关。



## ReentrantLock

```java
public static void main(String[] args) throws InterruptedException {
    // 1. 创建一个ReentrantLock实例（只有这一把锁，所有线程竞争的都是这把锁）
    ReentrantLock lock = new ReentrantLock();
    
    // 2. 主线程（main线程）获取这把锁，且后续没有释放！
    lock.lock();
    
    // 3. 创建两个子线程t1、t2，线程的任务就是调用lock.lock()（尝试获取这把锁）
    // 这里用了方法引用：lock::lock 等价于 () -> { lock.lock(); }
    Thread t1 = new Thread(lock::lock), t2 = new Thread(lock::lock);
    
    // 4. 启动两个子线程，它们会立刻尝试获取锁，但此时锁被主线程持有，获取失败
    t1.start();
    t2.start();
    
    // 5. 主线程休眠1秒，确保t1、t2有足够时间尝试获取锁并进入等待队列
    TimeUnit.SECONDS.sleep(1);
    
    // 6. 打印等待队列相关信息
    // 6.1 获取等待该锁释放的线程数量（预期是2，因为t1、t2都在等）
    System.out.println("当前等待锁释放的线程数："+lock.getQueueLength());
    // 6.2 检查t1是否在等待队列中（预期true）
    System.out.println("线程1是否在等待队列中："+lock.hasQueuedThread(t1));
    // 6.3 检查t2是否在等待队列中（预期true）
    System.out.println("线程2是否在等待队列中："+lock.hasQueuedThread(t2));
    // 6.4 检查主线程自己是否在等待队列中（预期false，因为主线程正持有锁，不需要等）
    System.out.println("当前线程是否在等待队列中："+lock.hasQueuedThread(Thread.currentThread()));
}

```

## 公平锁 非公平锁

公平锁：多个线程按照申请锁的顺序去获得锁，线程会直接进入队列去排队，永远都是队列的第一位才能得到锁。
非公平锁：多个线程去获取锁的时候，会直接去尝试获取，获取不到，再去进入等待队列，如果能获取到，就直接获取到锁。


## Runnable的介绍

一、Runnable 的核心作用
Runnable 是 Java 中一个函数式接口（只有一个抽象方法 run()），它的核心作用是封装要在新线程中执行的任务逻辑。
简单来说：
你把需要异步执行的代码写在 Runnable 的 run() 方法里，这个接口就像一个 “任务模板”，只定义 “要做什么”，但不关心 “谁来执行、怎么执行”。
它解耦了 “任务逻辑” 和 “线程本身的生命周期管理”（比如线程启动、暂停、终止），符合面向对象的 “单一职责原则”。


二、Runnable 和 Thread 的关系
Thread 是 Java 中线程的具体实现类，负责管理线程的生命周期（启动、运行、终止等）；Runnable 是任务的抽象，负责定义线程要执行的内容。二者是 “执行者” 和 “任务” 的关系，具体可以拆解为：
1. 本质关系：Thread 依赖 Runnable 提供执行逻辑
   Thread 类本身也实现了 Runnable 接口（public class Thread implements Runnable），它的 run() 方法逻辑如下：


Thread 是线程的 “容器”，Runnable 是线程要执行的 “内容”。


3. 为什么优先用 Runnable？
   避免单继承限制：Java 是单继承，继承 Thread 后就不能继承其他类，而实现 Runnable 还能继承其他类。
   任务复用：一个 Runnable 实例可以交给多个 Thread 执行，比如：
   java
   运行
   Runnable task = new MyTask();
   // 多个线程执行同一个任务
   new Thread(task, "线程1").start();
   new Thread(task, "线程2").start();
   适配线程池：Java 线程池（ExecutorService）只接收 Runnable/Callable 任务，不直接接收 Thread，这是实际开发中最常用的场景。
   总结
Runnable 的核心作用是封装异步执行的任务逻辑，只定义 “要做什么”，是任务的抽象；
   Thread 是线程的具体实现，负责管理线程生命周期，是任务的 “执行者”；
   
**二者的核心关系是：Thread 通过调用 Runnable 的 run() 方法执行任务**
实际开发中优先使用 Runnable（解耦、无单继承限制、适配线程池）。


## 是只有一个抽象方法的情况下才可以简写为 lambda表达式吗？

你问的是 “是不是只有一个抽象方法的接口才能简写为 Lambda 表达式”，这个理解基本正确，但需要补充一些关键细节来让你完全掌握这个知识点。
一、核心结论：Lambda 表达式的核心前提
Lambda 表达式在 Java 中是函数式接口的 “语法糖”（简化写法），而函数式接口的定义就是：只有一个抽象方法的接口（注意是 “抽象方法”，不是所有方法）。
也就是说：
✅ 能简写为 Lambda：只有当接口满足 “函数式接口” 条件（仅一个抽象方法）时，才能用 Lambda 表达式替代该接口的实现类。

