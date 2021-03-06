/*
 * Jenkins Plugin for SonarQube, open source software quality management tool.
 * mailto:contact AT sonarsource DOT com
 *
 * Jenkins Plugin for SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Jenkins Plugin for SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/*
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package hudson.plugins.sonar.configurationslicing;

import hudson.model.FreeStyleProject;
import hudson.plugins.sonar.SonarRunnerBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarRunnerBuilderSQRunnerSlicerTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void availableProjectsWithSonarBuildStep() throws IOException {
    final FreeStyleProject project = j.createFreeStyleProject();
    assertThat(new SonarRunnerBuilderSQRunnerSlicer().getWorkDomain().size()).isZero();
    project.getBuildersList().add(new SonarRunnerBuilder(null, null, null, null, null, null, null, null));
    assertThat(new SonarRunnerBuilderSQRunnerSlicer().getWorkDomain().size()).isEqualTo(1);
  }

  @Test
  public void changeJobAdditionalProperties() throws Exception {
    final FreeStyleProject project = j.createFreeStyleProject();
    final SonarRunnerBuilder mySonar = new SonarRunnerBuilder("MySonar", "SQ Runner 2.3", null, null, null, null, null, null);
    project.getBuildersList().add(mySonar);

    final SonarRunnerBuilderSQRunnerSlicer.SonarRunnerBuilderSQRunnerSlicerSpec spec = new SonarRunnerBuilderSQRunnerSlicer.SonarRunnerBuilderSQRunnerSlicerSpec();
    final List<String> values = spec.getValues(project);
    assertThat(values.get(0)).isEqualTo("SQ Runner 2.3");
    final List<String> newValues = new ArrayList<String>();
    newValues.add("SQ Runner 2.4");
    spec.setValues(project, newValues);

    assertThat(mySonar.getSonarRunnerName()).isEqualTo("SQ Runner 2.4");
  }

}
