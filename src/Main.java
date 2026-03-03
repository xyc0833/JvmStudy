public class Main {
    public static void main(String[] args) {
        int res = a();
        System.out.println(res);

//        String str1 = "abc";
//        String str2 = "abc";
//
//        System.out.println(str1 == str2);
//        System.out.println(str1.equals(str2));

        //intern()方法相当于把堆中创建的对象 对应的映射到运行时常量池里面
        //所以 intern()方法取的是常量池里的对象
        //不能直接写"abc"，双引号的形式，写了就直接在常量池里面吧abc创好了
        String str1 = new String("ab")+new String("c");
        String str2 = new String("ab")+new String("c");

        System.out.println(str1.intern() == str2.intern());
        System.out.println(str1.equals(str2));
    }

    public static int a(){
        return b();
    }

    public static int b(){
        return c();
    }

    public static int c(){
        int a = 10;
        int b = 20;
        return a + b;
    }



}