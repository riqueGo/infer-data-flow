package infer;

import java.nio.file.Path;

public class InferUtils {
    public static final String INFER_PACKAGE_NAME = "infer";
    public static final String WRAPPER_CLASS_NAME = "InferWrapper";
    public static final String SOURCE_PROJECT_PATH = "/src/main/java"; //TODO: This should be passed as program argument
    public static final String INFER_PACKAGE_PATH = Path.of(SOURCE_PROJECT_PATH, INFER_PACKAGE_NAME).toString();
}
