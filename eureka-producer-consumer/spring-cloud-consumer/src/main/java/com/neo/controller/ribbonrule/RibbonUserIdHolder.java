package com.neo.controller.ribbonrule;

public class RibbonUserIdHolder {

    //InheritableThreadLocal可以在当前线程正常创建的子线程下自动传递值
    private static ThreadLocal<Integer> userIdThreadLocal = new InheritableThreadLocal();

    public static void put(Integer userId){
        userIdThreadLocal.set(userId);
    }

    public static Integer get(){
        return userIdThreadLocal.get();
    }

    //用完必须要释放掉
    public static void remove(){
        userIdThreadLocal.remove();
    }
}
