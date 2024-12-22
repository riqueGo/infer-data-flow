package infer;

public class InferWrapper {
    public static <T> T left(T value) {return value;}
    public static void left(Runnable runnable) {runnable.run();}
    public static <T> T right(T value) {return value;}
    public static void right(Runnable runnable) {runnable.run();}
}
