/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.initialization

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import spock.lang.Issue

class DistributionPropertiesLoaderIntegrationTest extends AbstractIntegrationSpec {

    @Issue('https://github.com/gradle/gradle/issues/11173')
    def "System properties defined in gradle.properties are available in buildSrc and in included builds"() {
        given:
        requireIsolatedGradleDistribution()
        
        settingsFile << 'includeBuild "includedBuild"'
        buildFile << '''
            println("system_property_available in root:                    ${System.getProperty('system_property_available', 'false')} ")
            println("project_property_available in root:                   ${project.findProperty('project_property_available') ?: 'false'} ")
            task hello { }
        '''
        file('buildSrc/build.gradle') << '''
            println("system_property_available in buildSrc:                ${System.getProperty('system_property_available', 'false')} ")
            println("project_property_available in buildSrc:               ${project.findProperty('project_property_available') ?: 'false'} ")
        '''
        file('includedBuild/build.gradle') << '''
            println("system_property_available in includedBuild root:      ${System.getProperty('system_property_available', 'false')} ")
            println("project_property_available in includedBuild root:     ${project.findProperty('project_property_available') ?: 'false'} ")
        '''
        file('includedBuild/buildSrc/build.gradle') << '''
            println("system_property_available in includedBuild buildSrc:  ${System.getProperty('system_property_available', 'false')} ")
            println("project_property_available in includedBuild buildSrc: ${project.findProperty('project_property_available') ?: 'false'} ")
        '''
        file('gradle.properties') << '''
            systemProp.system_property_available=true
            project_property_available=true
        '''.stripIndent()


        when:
        succeeds 'hello'

        then:
        outputContains('system_property_available in buildSrc:                true')
        outputContains('system_property_available in buildSrc:                true')
        outputContains('project_property_available in buildSrc:               false')
        outputContains('system_property_available in includedBuild buildSrc:  true')
        outputContains('project_property_available in includedBuild buildSrc: false')
        outputContains('system_property_available in includedBuild root:      true')
        outputContains('project_property_available in includedBuild root:     false')
        outputContains('system_property_available in root:                    true')
        outputContains('project_property_available in root:                   true')

        cleanup:
        executer.withArguments("--stop", "--info").run()
    }
}