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

import org.apache.spark.deploy.SparkSubmitArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparkDriverAdjuster {
    private static final Logger LOG = LoggerFactory.getLogger(SparkDriverAdjuster.class);
    private static final Pattern LCC_AGENT_PATTERN = Pattern.compile("-javaagent:lcc-jvm-profiler\\S+");

    public static void adjust(SparkSubmitArguments args) {
        String lccAgentDefine = getExecutorAgentDefine(args);
        LOG.debug("LCC javaagent define found: {}", lccAgentDefine);

        if (isClientMode(args)) {
            String oldOpts = getValue(args.sparkProperties().get("spark.yarn.am.extraJavaOptions"), "");
            LOG.debug("LCC original yarnAmExtraJavaOptions is: {}", oldOpts);

            if (oldOpts != null && oldOpts.contains(lccAgentDefine)) {
                return;
            }

            String newOpts = oldOpts != null ? lccAgentDefine + " " + oldOpts : lccAgentDefine;
            LOG.debug("LCC adjusted yarnAmExtraJavaOptions is: {}", newOpts);

            args.sparkProperties().put("spark.yarn.am.extraJavaOptions", newOpts);
            LOG.info("LCC changed yarnAmExtraJavaOptions from {} to {}", oldOpts, newOpts);
        } else {
            String oldOpts = args.driverExtraJavaOptions();
            LOG.debug("LCC original driverExtraJavaOptions is: {}", oldOpts);

            if (oldOpts != null && oldOpts.contains(lccAgentDefine)) {
                return;
            }

            String newOpts = oldOpts != null ? lccAgentDefine + " " + oldOpts : lccAgentDefine;
            LOG.debug("LCC adjusted driverExtraJavaOptions is: {}", newOpts);

            args.driverExtraJavaOptions_$eq(newOpts);
            args.sparkProperties().put("spark.driver.extraJavaOptions", newOpts);
            LOG.info("LCC changed driverExtraJavaOptions from {} to {}", oldOpts, newOpts);
        }
    }

    private static boolean isClientMode(SparkSubmitArguments args) {
        return args.master() == null ||
                "client".equals(args.deployMode()) ||
                (!args.master().equals("yarn-cluster") && args.deployMode() == null);
    }

    private static String getExecutorAgentDefine(SparkSubmitArguments args) {
        String opts = getValue(args.sparkProperties().get("spark.executor.extraJavaOptions"), "");
        Matcher matcher = LCC_AGENT_PATTERN.matcher(opts);
        if (matcher.find()) {
            return matcher.group(0);
        } else {
            return "";
        }
    }

    private static String getValue(Option<String> opt, String defaultValue) {
        return opt.nonEmpty() ? opt.get() : defaultValue;
    }
}
