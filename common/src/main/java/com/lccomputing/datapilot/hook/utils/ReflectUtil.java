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
package com.lccomputing.datapilot.hook.utils;

import java.lang.reflect.Field;

public class ReflectUtil {

    /**
     * 通过反射从指定的对象中获取指定的字段的值
     * @param obj 指定的对象
     * @param className 该对象所属的类名（如果字段在父类中，这里需要指定父类的类名）
     * @param fieldName 字段名
     * @return 字段的值
     * @throws ReflectiveOperationException 一切反射过程中出现的异常
     */
    public static <T> T extract(Object obj, String className, String fieldName) throws ReflectiveOperationException {
        Class clazz = Class.forName(className);
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }

}
