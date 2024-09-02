/*
 * Lccomputing Sky DataPilot Hook
 * Copyright 2021 Lccomputing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lccomputing.datapilot.hook.agent;

import com.google.common.base.Strings;
import org.apache.spark.deploy.SparkSubmitArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class SparkHookInstaller {
    private static final Logger LOG = LoggerFactory.getLogger(SparkHookInstaller.class);
    private static final String LCC_HBO_SERVER_URLS = "lcc_hbo_server_urls";
    private static final String JVM_PROFILER_JAR = "lcc-jvm-profiler-1.0.jar";
    private final SparkSubmitArguments args;

    public SparkHookInstaller(SparkSubmitArguments args) {
        this.args = args;
    }

    public void install() throws URISyntaxException, IOException {
        String serverUrl = getServerUrl();
        if (Strings.isNullOrEmpty(serverUrl)) {
            LOG.warn("LCC {} not found in system env, skip install", LCC_HBO_SERVER_URLS);
            return;
        }

        URI jarPath = parseAgentJarPath();
        Path dir = Paths.get(jarPath).getParent();
        Map<String, String> jarNamePathMap = Files.list(dir)
                .filter(path -> hasSuffix(path, ".jar"))
                .collect(Collectors.toMap(path -> path.getFileName().toString(), Path::toString));
        LOG.debug("LCC parse agent dir: {}", dir);

        if (!jarNamePathMap.containsKey(JVM_PROFILER_JAR)) {
            LOG.warn("LCC {} not found in jar path {}, skip install", JVM_PROFILER_JAR, dir);
            return;
        }

        {
            String profileJarPath = jarNamePathMap.get(JVM_PROFILER_JAR);
            String oldFiles = args.files();
            LOG.debug("LCC original args.files is: {}", oldFiles);
            if (oldFiles == null || !oldFiles.contains(profileJarPath)) {
                String newFiles = Strings.isNullOrEmpty(oldFiles) ? profileJarPath : oldFiles + "," + profileJarPath;
                args.files_$eq(newFiles);
                args.sparkProperties().put("spark.files", newFiles);
                LOG.info("LCC changed args.files from {} to {}", oldFiles, newFiles);
            }
        }

        {
            String agentDefine = getAgentDefine(true);
            String oldOpts = getValue(args.sparkProperties().get("spark.executor.extraJavaOptions"));
            LOG.debug("LCC original spark.executor.extraJavaOptions is: {}", oldOpts);
            if (oldOpts == null || !oldOpts.contains(agentDefine)) {
                String newOpts = Strings.isNullOrEmpty(oldOpts) ? agentDefine : oldOpts + " " + agentDefine;
                args.sparkProperties().put("spark.executor.extraJavaOptions", newOpts);
                LOG.info("LCC changed spark.executor.extraJavaOptions from {} to {}", oldOpts, newOpts);
            }
        }

        {
            String agentDefine = getAgentDefine(false);
            if (isClientMode(args)) {
                String oldOpts = getValue(args.sparkProperties().get("spark.yarn.am.extraJavaOptions"));
                LOG.debug("LCC original spark.yarn.am.extraJavaOptions is: {}", oldOpts);
                if (oldOpts == null || !oldOpts.contains(agentDefine)) {
                    String newOpts = Strings.isNullOrEmpty(oldOpts) ? agentDefine : oldOpts + " " + agentDefine;
                    args.sparkProperties().put("spark.yarn.am.extraJavaOptions", newOpts);
                    LOG.info("LCC changed spark.yarn.am.extraJavaOptions from {} to {}", oldOpts, newOpts);
                }
            } else {
                String oldOpts = args.driverExtraJavaOptions();
                LOG.debug("LCC original args.driverExtraJavaOptions is: {}", oldOpts);
                if (oldOpts == null || oldOpts.contains(agentDefine)) {
                    String newOpts = Strings.isNullOrEmpty(oldOpts) ? agentDefine : oldOpts + " " + agentDefine;
                    args.driverExtraJavaOptions_$eq(newOpts);
                    args.sparkProperties().put("spark.driver.extraJavaOptions", newOpts);
                    LOG.info("LCC changed args.driverExtraJavaOptions from {} to {}", oldOpts, newOpts);
                }
            }
        }
    }

    private String getAgentDefine(boolean isExecutor) {
        String serverUrl = getServerUrl();
        StringBuilder agentDefineBuilder = new StringBuilder(100);
        agentDefineBuilder.append("-javaagent:").append(JVM_PROFILER_JAR).append("=");
        agentDefineBuilder.append("reporter=").append(serverUrl);
        if (isExecutor && args.isPython()) {
            agentDefineBuilder.append(";enableSubtree=true");
        }
        return agentDefineBuilder.toString();
    }

    private URI parseAgentJarPath() throws URISyntaxException {
        return SparkHookInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    }

    private boolean hasSuffix(Path path, String suffix) {
        return path.getFileName().toString().endsWith(suffix);
    }

    private String getValue(Option<String> opt) {
        return opt.nonEmpty() ? opt.get() : null;
    }

    private boolean isClientMode(SparkSubmitArguments args) {
        return args.master() == null ||
                "client".equals(args.deployMode()) ||
                (!args.master().equals("yarn-cluster") && args.deployMode() == null);
    }

    private String getServerUrl() {
        String serverUrl = System.getenv(LCC_HBO_SERVER_URLS);
        if (serverUrl != null && serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        return serverUrl;
    }

}
