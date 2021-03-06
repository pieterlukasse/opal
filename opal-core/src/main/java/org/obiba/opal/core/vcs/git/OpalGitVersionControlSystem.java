/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.core.vcs.git;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.obiba.opal.core.vcs.CommitInfo;
import org.obiba.opal.core.vcs.OpalGitException;
import org.obiba.opal.core.vcs.OpalVersionControlSystem;
import org.obiba.opal.core.vcs.git.commands.OpalGitCommitLogCommand;
import org.obiba.opal.core.vcs.git.commands.OpalGitCommitsLogCommand;
import org.obiba.opal.core.vcs.git.commands.OpalGitDiffCommand;
import org.obiba.opal.core.vcs.git.commands.OpalGitFetchBlobCommand;
import org.obiba.opal.core.vcs.support.OpalGitUtils;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.Nullable;

@Component
public class OpalGitVersionControlSystem implements OpalVersionControlSystem {

  @Override
  public List<CommitInfo> getCommitsInfo(@NotNull String datasource, @NotNull String path) {
    OpalGitCommitsLogCommand command = new OpalGitCommitsLogCommand.Builder(getRepository(datasource)).addPath(path)
        .addDatasourceName(datasource).build();
    return command.execute();
  }

  @Override
  public CommitInfo getCommitInfo(@NotNull String datasource, @NotNull String path, @NotNull String commitId) {
    OpalGitCommitLogCommand command = new OpalGitCommitLogCommand.Builder(getRepository(datasource), path, commitId)
        .addDatasourceName(datasource).build();
    return command.execute();
  }

  @Override
  public String getBlob(@NotNull String datasource, @NotNull String path, @NotNull String commitId) {
    OpalGitFetchBlobCommand command = new OpalGitFetchBlobCommand.Builder(getRepository(datasource), path, commitId)
        .addDatasourceName(datasource).build();
    return command.execute();
  }

  @Override
  public List<String> getDiffEntries(@NotNull String datasource, @NotNull String commitId,
      @Nullable String prevCommitId, @Nullable String path) {
    OpalGitDiffCommand command = new OpalGitDiffCommand.Builder(getRepository(datasource), commitId).addPath(path)
        .addDatasourceName(datasource).addPreviousCommitId(prevCommitId).build();

    return command.execute();
  }

  protected Repository getRepository(String name) {
    File repo = OpalGitUtils.getGitDirectoryName(OpalGitUtils.buildOpalGitRootPath(), name);
    FileRepositoryBuilder builder = new FileRepositoryBuilder();

    try {
      return builder.setGitDir(repo).readEnvironment().findGitDir().build();
    } catch(IOException e) {
      throw new OpalGitException(e.getMessage(), e);
    }
  }

}

