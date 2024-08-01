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

package com.lccomputing.datapilot.hook.profiling.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class Application {

    /**
     * This application could be used to test the java agent.
     * For example, you could run it with following argument:
     * -javaagent:target/lcc-jvm-profiler-1.0.jar=reporter=console,sampleInterval=10000
     */
    public static void main(String[] args) throws Throwable {
        long totalRunningMillis = 1 * 60 * 1000;
        long sleepMillis = 1000;

        if (args.length >= 1) {
            totalRunningMillis = Long.parseLong(args[0]);
        }

        if (args.length >= 2) {
            sleepMillis = Long.parseLong(args[1]);
        }

        long startMillis = System.currentTimeMillis();
        long lastPrintMillis = 0;

        Random random = new Random();

        List<byte[]> list = new ArrayList<>();

        while (System.currentTimeMillis() - startMillis < totalRunningMillis) {
            if (System.currentTimeMillis() - lastPrintMillis >= 10000) {
                System.out.println("Hello World " + System.currentTimeMillis());
                lastPrintMillis = System.currentTimeMillis();

                int size = 10 * 1024 * 1024;
                byte[] m = new byte[size];
                Arrays.fill(m, (byte) 7);
                list.add(m);
            }

            sleepMillis += random.nextInt(100);
            sleepMillis -= random.nextInt(100);

            privateSleepMethod(sleepMillis);

            AtomicLong atomicLong = new AtomicLong(sleepMillis);
            publicSleepMethod(atomicLong);
        }
    }

    private static void privateSleepMethod(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void publicSleepMethod(AtomicLong millis) {
        try {
            Thread.sleep(millis.get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
