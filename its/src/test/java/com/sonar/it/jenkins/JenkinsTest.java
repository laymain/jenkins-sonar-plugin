/*
 * Jenkins :: Integration Tests
 * Copyright (C) 2013 ${owner}
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.it.jenkins;

import com.sonar.it.jenkins.orchestrator.JenkinsOrchestrator;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SynchronousAnalyzer;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.URLLocation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.ResourceQuery;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeFalse;

public class JenkinsTest {

  @ClassRule
  public static Orchestrator orchestrator = JenkinsTestSuite.ORCHESTRATOR;

  @ClassRule
  public static JenkinsOrchestrator jenkins = JenkinsOrchestrator.builderEnv().build();

  @BeforeClass
  public static void setUpSonar() throws MalformedURLException {
    // Workaround for SONAR-4257
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.core.serverBaseURL", orchestrator.getServer().getUrl()));
  }

  @BeforeClass
  public static void setUpJenkins() throws MalformedURLException {
    Location sqJenkinsPluginLocation = FileLocation.of("../target/sonar.hpi");
    jenkins
      .installPlugin(URLLocation.create(new URL("http://mirrors.jenkins-ci.org/plugins/filesystem_scm/1.20/filesystem_scm.hpi")))
      .installPlugin(sqJenkinsPluginLocation)
      .configureMavenInstallation()
      .configureSonarRunner2_4Installation()
      .configureMsBuildSQRunner_installation()
      .configureSonarInstallation(orchestrator);
  }

  @Before
  public void resetData() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void testMavenJob() throws Exception {
    String jobName = "abacus-maven";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newMavenJobWithSonar(jobName, new File("projects", "abacus"), null)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testMavenJobWithBranch() throws Exception {
    String jobName = "abacus-maven-branch";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin:branch";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newMavenJobWithSonar(jobName, new File("projects", "abacus"), "branch")
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testVariableInjection() {
    String jobName = "abacus-freestyle-vars";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();

    jenkins.enableInjectionVars(true)
      .newFreestyleJobWithMaven(jobName, new File("projects", "abacus"), null)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithSonarMaven() throws Exception {
    String jobName = "abacus-freestyle";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newFreestyleJobWithSonar(jobName, new File("projects", "abacus"), null)
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithSonarMavenAndBranch() throws Exception {
    String jobName = "abacus-freestyle-branch";
    String projectKey = "org.codehaus.sonar-plugins:sonar-abacus-plugin:branch";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newFreestyleJobWithSonar(jobName, new File("projects", "abacus"), "branch")
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithSonarRunner() throws Exception {
    String jobName = "abacus-runner";
    String projectKey = "abacus-runner";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    jenkins
      .newFreestyleJobWithSonarRunner(jobName, "-X", new File("projects", "abacus"),
        "sonar.projectKey", projectKey,
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src/main/java")
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
  }

  @Test
  public void testFreestyleJobWithMsBuildSQRunner() {
    File toolPath = new File(jenkins.getServer().getHome().getAbsolutePath() + File.separator + "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation");
    String jobName = "abacus-msbuild-sq-runner";
    String projectKey = "abacus-msbuild-sq-runner";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithMsBuildSQRunner(jobName, null, new File("projects", "abacus"), projectKey, "Abacus with space", "1.0")
      .executeJobQuietly(jobName);

    assertThat(result.getLogs())
      .contains(
        "tools" + File.separator + "hudson.plugins.sonar.MsBuildSQRunnerInstallation" + File.separator + "SQ_runner" + File.separator
          + "MSBuild.SonarQube.Runner.exe begin /k:abacus-msbuild-sq-runner \"/n:Abacus with space\" /v:1.0 /d:sonar.host.url="
          + orchestrator.getServer().getUrl());

    assertThat(toolPath).isDirectory();
  }

  @Test
  public void testFreestyleJobWithSonarRunnerAndBranch() throws Exception {
    String jobName = "abacus-runner-branch";
    String projectKey = "abacus-runner:branch";
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNull();
    BuildResult result = jenkins
      .newFreestyleJobWithSonarRunner(jobName, "-X -Duseless=Y -e", new File("projects", "abacus"),
        "sonar.projectKey", "abacus-runner",
        "sonar.projectVersion", "1.0",
        "sonar.projectName", "Abacus",
        "sonar.sources", "src/main/java",
        "sonar.branch", "branch")
      .executeJob(jobName);
    waitForComputationOnSQServer();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(projectKey))).isNotNull();
    assertSonarUrlOnJob(jobName, projectKey);
    if (isWindows()) {
      assertThat(result.getLogs()).contains("sonar-runner.bat -X -Duseless=Y -e");
    } else {
      assertThat(result.getLogs()).contains("sonar-runner -X -Duseless=Y -e");
    }
  }

  // SONARJNKNS-214
  @Test
  public void testFreestyleJobWithTask() throws Exception {
    // Task concept was removed in 5.2
    assumeFalse(orchestrator.getServer().version().isGreaterThanOrEquals("5.2"));
    String jobName = "refresh-views";
    BuildResult result = jenkins
      .newFreestyleJobWithSonarRunner(jobName, null, new File("projects", "abacus"), "sonar.task", "views")
      .executeJobQuietly(jobName);
    // Since views is not installed
    assertThat(result.getLogs()).contains("Task views does not exist");
  }

  private void assertSonarUrlOnJob(String jobName, String projectKey) {
    // Computation of Sonar URL was not reliable before 2.1 & Sonar 3.6
    assertThat(jenkins.getSonarUrlOnJob(jobName)).startsWith(orchestrator.getServer().getUrl());
    if (orchestrator.getServer().version().isGreaterThanOrEquals("3.6")) {
      assertThat(jenkins.getSonarUrlOnJob(jobName)).endsWith(projectKey);
    }
  }

  private void waitForComputationOnSQServer() {
    new SynchronousAnalyzer(orchestrator.getServer()).waitForDone();
  }

  protected boolean isWindows() {
    return File.pathSeparatorChar == ';' || System.getProperty("os.name").startsWith("Windows");
  }

}
