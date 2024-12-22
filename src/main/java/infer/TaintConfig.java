package infer;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TaintConfig {
    @SerializedName("class_names")
    List<String> classNames;
    @SerializedName("method_names")
    List<String> methodNames;

    public TaintConfig(List<String> classNames, List<String> methodNames) {
        this.classNames = classNames;
        this.methodNames = methodNames;
    }
}
