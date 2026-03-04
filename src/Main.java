

public class Main {
    //volatile关键字
    //JMM会把该线程本地内存中的变量强制刷新到主内存中去，并且这个写会操作会导致其他线程中的volatile变量缓存无效
    private static volatile int a=0;
    public static void main(String[] args) throws InterruptedException{

        new Thread(()->{
            while(a == 0){
                System.out.println("线程结束");
            }
        }).start();
        Thread.sleep(1000);
        System.out.println("正在修改a的值");
        a = 1;

    }
}