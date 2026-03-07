import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        //创建一个初始值为5的循环屏障
        CyclicBarrier barrier = new CyclicBarrier(5);
        for (int i = 0; i < 5; i++) {   //创建5个线程
            int finalI = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                    System.out.println("玩家 "+ finalI +" 进入房间进行等待... ("+barrier.getNumberWaiting()+"/5)");

                    barrier.await();    //调用await方法进行等待，直到等待线程到达5才会一起继续执行

                    //人数到齐之后，可以开始游戏了
                    System.out.println("玩家 "+ finalI +" 进入游戏！");
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }).start();
            //Thread.sleep(500);   //等一下上面的线程开始运行
            //System.out.println("当前屏障前的等待线程数："+barrier.getNumberWaiting());

            //barrier.reset();
            //System.out.println("重置后屏障前的等待线程数："+barrier.getNumberWaiting());
        }
    }
}