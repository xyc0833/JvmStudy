import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        //可重入锁 ： 可以反复进行加锁操作
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        lock.lock();
        new Thread(()->{
            System.out.println("线程2想要获取锁");
            lock.lock();
            System.out.println("线程2成功获取到锁");
        }).start();
        lock.unlock();
        System.out.println("线程1释放了一次锁");
        TimeUnit.SECONDS.sleep(1);
        lock.unlock();
        System.out.println("线程1再次释放了一次锁");  //释放两次后其他线程才能加锁
    }
}