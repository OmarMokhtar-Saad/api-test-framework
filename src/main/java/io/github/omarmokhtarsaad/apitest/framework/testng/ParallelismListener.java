package io.github.omarmokhtarsaad.apitest.framework.testng;

import io.github.omarmokhtarsaad.apitest.framework.config.ConfigKeys;
import io.github.omarmokhtarsaad.apitest.framework.config.ConfigProvider;
import io.github.omarmokhtarsaad.apitest.framework.config.ConfigurationException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

/**
 * Lets {@code -Dthreads=N} (or {@code API_THREADS}) override the thread count of every suite,
 * including data-provider threads, without editing suite XML.
 */
public final class ParallelismListener implements IAlterSuiteListener {

    private static final Logger LOG = LoggerFactory.getLogger(ParallelismListener.class);

    @Override
    public void alter(List<XmlSuite> suites) {
        threads()
                .ifPresent(threads -> suites.forEach(suite -> {
                    suite.setThreadCount(threads);
                    suite.setDataProviderThreadCount(threads);
                    LOG.info("Suite '{}': thread count set to {}", suite.getName(), threads);
                }));
    }

    static Optional<Integer> threads() {
        return ConfigProvider.bootstrap().get(ConfigKeys.THREADS).map(ParallelismListener::parse);
    }

    static int parse(String raw) {
        try {
            int threads = Integer.parseInt(raw.trim());
            if (threads < 1) {
                throw new ConfigurationException("'threads' must be >= 1, was " + threads);
            }
            return threads;
        } catch (NumberFormatException e) {
            throw new ConfigurationException("'threads' must be an integer, was '" + raw + "'");
        }
    }
}
