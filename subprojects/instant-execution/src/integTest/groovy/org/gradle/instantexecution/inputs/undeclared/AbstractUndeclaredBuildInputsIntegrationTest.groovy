/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.instantexecution.inputs.undeclared

import org.gradle.instantexecution.AbstractInstantExecutionIntegrationTest
import spock.lang.Unroll

abstract class AbstractUndeclaredBuildInputsIntegrationTest extends AbstractInstantExecutionIntegrationTest {
    abstract void buildLogicApplication(SystemPropertyRead read)

    @Unroll
    def "reports undeclared system property read using #propertyRead.groovyExpression prior to task execution from plugin"() {
        buildLogicApplication(propertyRead)
        def fixture = newInstantExecutionFixture()

        when:
        instantFails("thing", "-DCI=$value")

        then:
        fixture.assertStateStored()
        // TODO - use problems fixture, need to be able to tweak the problem matching as build script class name is included in the message and this is generated
        failure.assertThatDescription(containsNormalizedString("- unknown location: read system property 'CI' from class '"))
        outputContains("apply = $value")
        outputContains("task = $value")

        when:
        problems.withDoNotFailOnProblems()
        instantRun("thing", "-DCI=$value")

        then:
        fixture.assertStateLoaded()
        problems.assertResultHasProblems(result)
        outputDoesNotContain("apply =")
        outputContains("task = $value")

        when:
        instantRun("thing", "-DCI=$newValue")

        then:
        fixture.assertStateLoaded() // undeclared properties are not considered build inputs, but probably should be
        problems.assertResultHasProblems(result)
        outputDoesNotContain("apply =")
        outputContains("task = $newValue")

        where:
        propertyRead                                                                  | value  | newValue
        SystemPropertyRead.systemGetProperty("CI")                                    | "true" | "false"
        SystemPropertyRead.systemGetPropertyWithDefault("CI", "default")              | "true" | "false"
        SystemPropertyRead.systemGetPropertiesGet("CI")                               | "true" | "false"
        SystemPropertyRead.systemGetPropertiesGetProperty("CI")                       | "true" | "false"
        SystemPropertyRead.systemGetPropertiesGetPropertyWithDefault("CI", "default") | "true" | "false"
        SystemPropertyRead.systemGetPropertiesFilterEntries("CI")                     | "true" | "false"
        SystemPropertyRead.integerGetInteger("CI")                                    | "12"   | "45"
        SystemPropertyRead.integerGetIntegerWithPrimitiveDefault("CI", 123)           | "12"   | "45"
        SystemPropertyRead.integerGetIntegerWithIntegerDefault("CI", 123)             | "12"   | "45"
        SystemPropertyRead.longGetLong("CI")                                          | "12"   | "45"
        SystemPropertyRead.longGetLongWithPrimitiveDefault("CI", 123)                 | "12"   | "45"
        SystemPropertyRead.longGetLongWithLongDefault("CI", 123)                      | "12"   | "45"
        SystemPropertyRead.booleanGetBoolean("CI")                                    | "true" | "false"
    }
}
