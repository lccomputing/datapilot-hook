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
import org.apache.hadoop.conf.Configuration;
import org.apache.tez.dag.api.TezConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class TezHookInstaller {
    private static final Logger LOG = LoggerFactory.getLogger(TezHookInstaller.class);
    private static final String LCC_HBO_SERVER_URLS = "lcc_hbo_server_urls";
    private static final String LCC_HOOK_AGENT_JAR = "lcc-hook-agent-tez-1.0.jar";

    public static void uploadJar(Configuration conf) throws URISyntaxException, IOException {
        URI jarPath = parseAgentJarPath();
        Path dir = Paths.get(jarPath).getParent();
        Map<String, Path> jarNamePathMap = Files.list(dir)
                .filter(path -> hasSuffix(path, ".jar"))
                .collect(Collectors.toMap(path -> path.getFileName().toString(), Function.identity()));
        LOG.debug("LCC parse agent dir: {}", dir);

        if (!jarNamePathMap.containsKey(LCC_HOOK_AGENT_JAR)) {
            LOG.warn("LCC {} not found in jar path {}, skip install", LCC_HOOK_AGENT_JAR, dir);
            return;
        }

        String oldJars = conf.get("hive.aux.jars.path", "");
        LOG.debug("LCC original hive.aux.jars.path is: {}", oldJars);
        if (!oldJars.contains(LCC_HOOK_AGENT_JAR)) {
            String profileJarPath = jarNamePathMap.get(LCC_HOOK_AGENT_JAR).toUri().toString();
            String newJars = oldJars.isEmpty() ? profileJarPath : oldJars + "," + profileJarPath;
            conf.set("hive.aux.jars.path", newJars);
            LOG.info("LCC changed hive.aux.jars.path from {} to {}", oldJars, newJars);
        }
    }

    public static void addAmOpts(TezConfiguration tezConfiguration) {
        String serverUrl = getServerUrl();
        if (Strings.isNullOrEmpty(serverUrl)) {
            LOG.warn("LCC {} not found in system env, skip install", LCC_HBO_SERVER_URLS);
            return;
        }

        String oldOpts = tezConfiguration.get("tez.am.launch.cmd-opts", TezConfiguration.TEZ_AM_LAUNCH_CMD_OPTS_DEFAULT);
        LOG.debug("LCC original tez.am.launch.cmd-opts is: {}", oldOpts);
        if (!oldOpts.contains(LCC_HOOK_AGENT_JAR)) {
            String newOpts = oldOpts + " -javaagent:" + LCC_HOOK_AGENT_JAR + "=reporter=" + getServerUrl();
            tezConfiguration.set("tez.am.launch.cmd-opts", newOpts);
            LOG.info("LCC changed tez.am.launch.cmd-opts from {} to {}", oldOpts, tezConfiguration.get("tez.am.launch.cmd-opts"));
        }
    }

    private static String getServerUrl() {
        String serverUrl = System.getenv(LCC_HBO_SERVER_URLS);
        if (serverUrl != null && serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        return serverUrl;
    }

    private static URI parseAgentJarPath() throws URISyntaxException {
        return TezHookInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    }

    private static boolean hasSuffix(Path path, String suffix) {
        return path.getFileName().toString().endsWith(suffix);
    }
}
