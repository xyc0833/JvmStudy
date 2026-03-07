import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        //创建一个初始值为10的计数器锁
        //多任务同步神器。它允许一个或多个线程，等待其他线程完成工作
        CountDownLatch latch = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                    System.out.println("子任务"+ finalI +"执行完成！");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                latch.countDown();   //每执行一次计数器都会-1
            }).start();
        }
        //开始等待所有的线程完成，当计数器为0时，恢复运行
        latch.await();//这个操作可以同时被多个线程执行，一起等待，这里只演示了一个
        System.out.println("所有子任务都完成！任务完成！！！");
        //注意这个计数器只能使用一次，用完只能重新创一个，没有重置的说法

    }
}