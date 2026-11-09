package io.github.omarmokhtarsaad.apitest.framework.config;

/** When captured HTTP exchanges are published to the report and console. */
public enum HttpLogPolicy {
    /** Publish only for failed tests. The default: green runs stay quiet, red runs are debuggable. */
    FAILURES,
    /** Publish for every test. Useful while writing a new test. */
    ALL,
    /** Never capture. */
    NONE
}
