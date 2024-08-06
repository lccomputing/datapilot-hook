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
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lccomputing.profiling;

import com.lccomputing.datapilot.hook.profiling.Arguments;
import com.lccomputing.datapilot.hook.profiling.Profiler;
import com.lccomputing.datapilot.hook.profiling.ProfilerRunner;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfilerRunnableTest {
    @Test
    public void invokeRunnable() {
        final AtomicInteger i = new AtomicInteger(10);
        String args = "";
        Arguments arguments = new Arguments(args);

        ProfilerRunner profilerRunnable = new ProfilerRunner(arguments, new Profiler() {
            @Override
            public String getName() {
                return "TestProfiler";
            }

            @Override
            public Map<String, Object> getMetrics() {
                return new HashMap<>();
            }

            @Override
            public void terminate() {

            }

            @Override
            public void profile() {
                i.incrementAndGet();
            }
        });

        profilerRunnable.run();

        Assert.assertEquals(11, i.get());
    }
}
