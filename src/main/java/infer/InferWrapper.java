package infer;

public class InferWrapper {
    public static <T> T l(T value) {return value;}
    public static void l(Runnable runnable) {runnable.run();}
    public static <T> T r(T value) {return value;}
    public static void r(Runnable runnable) {runnable.run();}
}
