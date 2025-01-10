package infer;

import java.nio.file.Path;

public class InferConstants {
    public static final String INFER_PACKAGE_NAME = "infer";
    public static final String WRAPPER_CLASS_NAME = "InferWrapper";
    public static final String SOURCE_PROJECT_PATH = "/src/main/java"; //TODO: This should be passed as program argument
    public static final String INFER_PACKAGE_PATH = Path.of(SOURCE_PROJECT_PATH, INFER_PACKAGE_NAME).toString();
    public static final String INFER_CONFIG_LEFT_TO_RIGHT = "infer_config_left_to_right.json";
    public static final String INFER_CONFIG_RIGHT_TO_LEFT = "infer_config_right_to_left.json";
    public static final String INFER_OUT = "infer-out";
    public static final String REWRITTEN_PROPERTY = "rewritten";
    public static final String WORKING_DIRECTORY = System.getProperty("user.dir");
    public static final int INTERPROCEDURAL_DEPTH = 5;
}
