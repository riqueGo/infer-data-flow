package infer;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

import static infer.InferUtils.INFER_PACKAGE_NAME;
import static infer.InferUtils.WRAPPER_CLASS_NAME;

public class InferConfig {
    @SerializedName("pulse-taint-sources")
    List<TaintConfig> sources;

    @SerializedName("pulse-taint-sinks")
    List<TaintConfig> sinks;

    public InferConfig() {
        TaintConfig source = new TaintConfig(
            List.of("%s.%s".formatted(INFER_PACKAGE_NAME, WRAPPER_CLASS_NAME)),
            List.of("left")
        );

        sources = List.of(source);

        TaintConfig sink = new TaintConfig(
            List.of("%s.%s".formatted(INFER_PACKAGE_NAME, WRAPPER_CLASS_NAME)),
            List.of("right")
        );
        sinks = List.of(sink);
    }

    public void swap() {
        List<TaintConfig> temp = new ArrayList<>(sources);
        sources = new ArrayList<>(sinks);
        sinks = new ArrayList<>(temp);
    }
}

