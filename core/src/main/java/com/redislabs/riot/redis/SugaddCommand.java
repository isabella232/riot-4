package com.redislabs.riot.redis;

import io.lettuce.core.RedisFuture;
import org.springframework.batch.item.redis.support.CommandBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.function.BiFunction;

@Command(name = "sugadd", description = "Add suggestion strings to a RediSearch auto-complete suggestion dictionary")
public class SugaddCommand extends AbstractKeyCommand {

    @Option(names = "--field", required = true, description = "Field containing the strings to add", paramLabel = "<field>")
    private String field;
    @SuppressWarnings("unused")
    @Option(names = "--score", description = "Name of the field to use for scores", paramLabel = "<field>")
    private String scoreField;
    @Option(names = "--score-default", description = "Score when field not present (default: ${DEFAULT-VALUE})", paramLabel = "<num>")
    private double scoreDefault = 1;
    @Option(names = "--payload", description = "Field containing the payload", paramLabel = "<field>")
    private String payload;

    @Override
    public BiFunction<?, Map<String, Object>, RedisFuture<?>> command() {
        return configureKeyCommandBuilder(new SugaddBuilder<>()).stringConverter(stringFieldExtractor(field)).scoreConverter(numberFieldExtractor(Double.class, scoreField, scoreDefault)).payloadConverter(stringFieldExtractor(payload)).build();
    }


}
