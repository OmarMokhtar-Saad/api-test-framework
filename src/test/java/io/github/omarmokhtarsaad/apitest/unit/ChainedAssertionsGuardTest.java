package io.github.omarmokhtarsaad.apitest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.testng.annotations.Test;

/**
 * Fails when a test makes consecutive one-line {@code assertThat(x).…;} statements on the same
 * subject instead of chaining them: {@code assertThat(x).a().b();}.
 */
@Test(groups = "unit")
public class ChainedAssertionsGuardTest {

    private static final Path TEST_SOURCES = Path.of("src", "test", "java");
    private static final Pattern ONE_LINE_ASSERT =
            Pattern.compile("^\\s*assertThat\\((\\w+(?:\\.\\w+)*)\\)\\..*;\\s*$");

    public void noConsecutiveAssertionsOnTheSameSubject() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(TEST_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                violations.addAll(scan(file));
            }
        }
        assertThat(violations)
                .as("chain consecutive assertThat(x) statements: assertThat(x).a().b();")
                .isEmpty();
    }

    public void detectsConsecutiveAssertionsOnTheSameSubject() {
        List<String> lines =
                List.of("assertThat(a).isNotNull();", "assertThat(a).isEqualTo(1);", "assertThat(b).isNull();");

        assertThat(violationsIn("Sample.java", lines)).containsExactly("Sample.java:2");
    }

    private static List<String> scan(Path file) throws IOException {
        return violationsIn(file.toString(), Files.readAllLines(file));
    }

    private static List<String> violationsIn(String name, List<String> lines) {
        List<String> found = new ArrayList<>();
        String previous = null;
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = ONE_LINE_ASSERT.matcher(lines.get(i));
            String subject = m.matches() ? m.group(1) : null;
            if (subject != null && subject.equals(previous)) {
                found.add(name + ":" + (i + 1));
            }
            previous = subject;
        }
        return found;
    }
}
