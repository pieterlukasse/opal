/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.cfg;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.util.FS;
import org.obiba.core.util.FileUtil;
import org.obiba.core.util.StreamUtil;
import org.obiba.magma.Datasource;
import org.obiba.magma.MagmaEngine;
import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;
import org.obiba.magma.views.View;
import org.obiba.magma.views.ViewPersistenceStrategy;
import org.obiba.magma.xstream.MagmaXStreamExtension;
import org.obiba.opal.audit.OpalUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;

/**
 * This implementation of the {@link ViewPersistenceStrategy} serializes and de-serializes {@link View}s to XML files.
 * Each XML file is named after a {@link Datasource} and contains all the Views that are associated with that
 * Datasource.
 */
public class OpalViewPersistenceStrategy implements ViewPersistenceStrategy {

  private static final Logger log = LoggerFactory.getLogger(OpalViewPersistenceStrategy.class);

  public final static String OPAL_HOME_SYSTEM_PROPERTY_NAME = "OPAL_HOME";

  public static final String CONF_DIRECTORY_NAME = "conf";

  public static final String VIEWS_DIRECTORY_NAME = "views";

  public static final String GIT_DIRECTORY_NAME = "git";

  public static final String VIEW_FILE_NAME = "View.xml";

  private final OpalUserProvider opalUserProvider;

  private final File viewsDirectory;

  private final File gitViewsDirectory;

  private final ReadWriteLock rwl = new ReentrantReadWriteLock(true);

  private final Lock r = rwl.readLock();

  private final Lock w = rwl.writeLock();

  public OpalViewPersistenceStrategy() {
    this(null);
  }

  public OpalViewPersistenceStrategy(OpalUserProvider opalUserProvider) {
    this.opalUserProvider = opalUserProvider;
    String viewsDirectoryName = System.getProperty(OPAL_HOME_SYSTEM_PROPERTY_NAME) + File.separator //
        + CONF_DIRECTORY_NAME + File.separator + VIEWS_DIRECTORY_NAME;
    viewsDirectory = new File(viewsDirectoryName);
    gitViewsDirectory = new File(System.getProperty(OPAL_HOME_SYSTEM_PROPERTY_NAME),
        GIT_DIRECTORY_NAME + File.separator + VIEWS_DIRECTORY_NAME);
  }

  @Override
  public void initialise() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public void writeViews(@NotNull String datasourceName, @NotNull Set<View> views, @Nullable String comment) {
    w.lock();
    try {
      doWriteGitViews(datasourceName, views, comment);
    } finally {
      w.unlock();
    }
  }

  @Override
  public void writeView(@NotNull String datasourceName, @NotNull View view, @Nullable String comment) {
    w.lock();
    try {
      doWriteGitViews(datasourceName, ImmutableSet.of(view), comment);
    } finally {
      w.unlock();
    }
  }

  @Override
  public void removeView(@NotNull String datasourceName, @NotNull String viewName) {
    w.lock();
    try {
      doRemoveGitView(datasourceName, viewName);
    } finally {
      w.unlock();
    }
  }

  @Override
  public void removeViews(String datasourceName) {
    w.lock();
    try {
      File targetDir = getDatasourceViewsGit(datasourceName);
      FileUtil.delete(targetDir);
    } catch(IOException e) {
      throw new RuntimeException("Failed deleting views in git for datasource: " + datasourceName, e);
    } finally {
      w.unlock();
    }
  }

  @Override
  public Set<View> readViews(@NotNull String datasourceName) {
    r.lock();
    try {
      File targetDir = getDatasourceViewsGit(datasourceName);
      return targetDir.exists() ? doReadGitViews(datasourceName) : doReadViews(datasourceName);
    } finally {
      r.unlock();
    }
  }

  private void doCommitPush(Git git, String message) throws GitAPIException {
    git.commit().setAuthor(getAuthorName(), "opal@obiba.org").setCommitter("opal", "opal@obiba.org").setMessage(message)
        .call();
    git.push().setPushAll().setRemote("origin").call();
  }

  private void doWriteGitViews(@NotNull String datasourceName, @NotNull Iterable<View> views,
      @Nullable String comment) {
    File localRepo = null;

    try {
      // Fetch or clone a tmp working directory
      localRepo = cloneDatasourceViewsGit(datasourceName);

      // Serialize all view files in the repo
      List<String> varFilesToRemove = Lists.newArrayList();
      StringBuilder message = new StringBuilder();
      for(View view : views) {
        doWriteGitView(localRepo, view, varFilesToRemove);
        if(message.length() > 0) {
          message.append(", ");
        }
        message.append(view.getName());
      }

      // Push changes
      Git git = new Git(new FileRepository(new File(localRepo, ".git")));
      for(String toRemove : varFilesToRemove) {
        git.rm().addFilepattern(toRemove).call();
      }
      git.add().addFilepattern(".").call();
      doCommitPush(git, Strings.isNullOrEmpty(comment) ? "Update " + message : comment);

    } catch(Exception e) {
      throw new RuntimeException("Failed writing views in git for datasource: " + datasourceName, e);
    } finally {
      deleteLocalRepo(localRepo);
    }
  }

  private void doRemoveGitView(@NotNull String datasourceName, @NotNull String viewName) {
    File localRepo = null;
    try {
      localRepo = cloneDatasourceViewsGit(datasourceName);
      Git git = new Git(new FileRepository(new File(localRepo, ".git")));
      git.rm().addFilepattern(viewName).call();
      doCommitPush(git, "Remove " + viewName);
    } catch(Exception e) {
      throw new RuntimeException("Failed removing view '" + viewName + "' from git for datasource: " + datasourceName,
          e);
    } finally {
      deleteLocalRepo(localRepo);
    }
  }

  private void deleteLocalRepo(File localRepo) {
    if(localRepo == null) return;

    try {
      FileUtil.delete(localRepo);
    } catch(IOException e) {
      log.warn("Failed removing local git repository: {}", localRepo.getAbsolutePath(), e);
    }
  }

  private File[] listViewDirectories(File localRepo) {
    return localRepo.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory() && !pathname.getName().equals(".git") &&
            new File(pathname, VIEW_FILE_NAME).exists();
      }
    });
  }

  private File cloneDatasourceViewsGit(String datasourceName) throws Exception {
    File targetDir = getDatasourceViewsGit(datasourceName);

    // Create datasource views bare git repo
    Repository newRepo = new FileRepository(targetDir);
    if(!targetDir.exists()) {
      newRepo.create(true);
    }

    String remoteUrl = "file://" + targetDir.getAbsolutePath();
    File tmp = getTmpDirectory();
    File localRepo = new File(tmp, "opal-" + Long.toString(System.nanoTime()));
    cloneRepository(localRepo.getParentFile(), localRepo.getName(), remoteUrl);
    return localRepo;
  }

  private void doWriteGitView(File localRepo, ValueTable view, Collection<String> varFilesToRemove) throws IOException {
    File viewRepo = new File(localRepo, view.getName());
    viewRepo.mkdirs();

    // Write serialiazed view
    File viewFile = new File(viewRepo, VIEW_FILE_NAME);
    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(viewFile), Charsets.UTF_8);
    try {
      getXStream().toXML(view, writer);
    } finally {
      StreamUtil.silentSafeClose(writer);
    }

    // Write variable script files
    for(Variable variable : view.getVariables()) {
      doWriteGitViewVariable(viewRepo, variable);
    }
    // Detect removed variables

    for(File f : viewRepo.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.getName().endsWith(".js");
      }
    })) {
      String varName = f.getName().substring(0, f.getName().length() - 3);
      if(!view.hasVariable(varName)) {
        varFilesToRemove.add(f.getParentFile().getName() + "/" + f.getName());
      }
    }
  }

  private void doWriteGitViewVariable(File viewRepo, Variable variable) throws IOException {
    String script = variable.hasAttribute("script") ? variable.getAttributeStringValue("script") : "null";
    File variableFile = new File(viewRepo, variable.getName() + ".js");

    FileWriter fileWriter = new FileWriter(variableFile);
    try {
      fileWriter.append(script);
      fileWriter.flush();
    } finally {
      StreamUtil.silentSafeClose(fileWriter);
    }
  }

  private File getDatasourceViewsGit(String datasourceName) {
    return new File(gitViewsDirectory, datasourceName + ".git");
  }

  private String getAuthorName() {
    return opalUserProvider == null ? OpalUserProvider.UNKNOWN_USERNAME : opalUserProvider.getUsername();
  }

  private File getTmpDirectory() throws IOException {
    // for java 6 compatibility
    File tmp = File.createTempFile("temp", Long.toString(System.nanoTime()));
    tmp.delete();
    return tmp.getParentFile();
  }

  @SuppressWarnings("unchecked")
  @NotNull
  private Set<View> doReadViews(@NotNull String datasourceName) {
    Set<View> result = ImmutableSet.of();
    if(!viewsDirectory.isDirectory()) {
      log.info("The views directory '{}' does not exist.", viewsDirectory.getAbsolutePath());
      return result;
    }
    InputStreamReader reader = null;
    try {
      reader = new InputStreamReader(new FileInputStream(getDatasourceViewsFile(datasourceName)), Charsets.UTF_8);
      result = (Set<View>) getXStream().fromXML(reader);
    } catch(FileNotFoundException e) {
      return ImmutableSet.of();
    } finally {
      StreamUtil.silentSafeClose(reader);
    }
    return result;
  }

  @NotNull
  private Set<View> doReadGitViews(@NotNull String datasourceName) {
    ImmutableSet.Builder<View> builder = ImmutableSet.builder();

    File localRepo = null;
    try {
      localRepo = cloneDatasourceViewsGit(datasourceName);

      for(File f : listViewDirectories(localRepo)) {
        View view = doReadGitView(f);
        if(view != null) {
          builder.add(view);
        }
      }
    } catch(Exception e) {
      throw new RuntimeException("Failed reading views from git for datasource: " + datasourceName, e);
    } finally {
      deleteLocalRepo(localRepo);
    }

    return builder.build();
  }

  private View doReadGitView(File viewRepo) {
    View result = null;
    File viewFile = new File(viewRepo, VIEW_FILE_NAME);
    if(viewFile.exists()) {
      InputStreamReader reader = null;
      try {
        reader = new InputStreamReader(new FileInputStream(viewFile), Charsets.UTF_8);
        result = (View) getXStream().fromXML(reader);
      } catch(FileNotFoundException e) {
        // not supposed to happen
      } finally {
        StreamUtil.silentSafeClose(reader);
      }
    }
    return result;
  }

  protected XStream getXStream() {
    return MagmaEngine.get().getExtension(MagmaXStreamExtension.class).getXStreamFactory().createXStream();
  }

  private void createViewsDirectory() {
    if(!viewsDirectory.isDirectory()) {
      if(!viewsDirectory.mkdirs()) {
        throw new RuntimeException(
            "The views directory '" + viewsDirectory.getAbsolutePath() + "' could not be created.");
      }
    }
  }

  private String normalizeDatasourceName(@SuppressWarnings("TypeMayBeWeakened") String datasourceName) {
    Pattern escaper = Pattern.compile("([^a-zA-Z0-9-_. ])");
    return escaper.matcher(datasourceName).replaceAll("");
  }

  private File getDatasourceViewsFile(String datasourceName) {
    return new File(viewsDirectory, normalizeDatasourceName(datasourceName) + ".xml");
  }

  /**
   * Clone or Fetch a repository. If the local repository does not exist,
   * clone is called. If the repository does exist, fetch is called. By
   * default the clone/fetch retrieves the remote heads, tags, and notes.
   *
   * @param repositoriesFolder
   * @param name
   * @param fromUrl
   * @throws Exception
   */
  private void cloneRepository(File repositoriesFolder, String name, String fromUrl) throws Exception {
    // normal repository, strip .git suffix
    if(name.toLowerCase().endsWith(Constants.DOT_GIT_EXT)) {
      name = name.substring(0, name.indexOf(Constants.DOT_GIT_EXT));
    }

    File folder = new File(repositoriesFolder, name);
    if(folder.exists()) {
      File gitDir = RepositoryCache.FileKey.resolve(new File(repositoriesFolder, name), FS.DETECTED);
      Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).build();
      fetchRepository(repository);
      repository.close();
    } else {
      CloneCommand clone = new CloneCommand();
      clone.setBare(false);
      clone.setCloneAllBranches(true);
      clone.setURI(fromUrl);
      clone.setDirectory(folder);
      Repository repository = clone.call().getRepository();

      // Now we have to fetch because CloneCommand doesn't fetch
      // refs/notes nor does it allow manual RefSpec.
      fetchRepository(repository);
      repository.close();
    }
  }

  /**
   * Fetch updates from the remote repository. If refSpecs is unspecifed,
   * remote heads, tags, and notes are retrieved.
   *
   * @param credentialsProvider
   * @param repository
   * @param refSpecs
   * @return FetchResult
   * @throws Exception
   */
  private FetchResult fetchRepository(Repository repository, RefSpec... refSpecs) throws Exception {
    Git git = new Git(repository);
    FetchCommand fetch = git.fetch();
    List<RefSpec> specs = new ArrayList<RefSpec>();
    if (refSpecs == null || refSpecs.length == 0) {
      specs.add(new RefSpec("+refs/heads/*:refs/remotes/origin/*"));
      specs.add(new RefSpec("+refs/tags/*:refs/tags/*"));
      specs.add(new RefSpec("+refs/notes/*:refs/notes/*"));
    } else {
      specs.addAll(Arrays.asList(refSpecs));
    }
    fetch.setRefSpecs(specs);
    FetchResult fetchRes = fetch.call();
    return fetchRes;
  }
}
