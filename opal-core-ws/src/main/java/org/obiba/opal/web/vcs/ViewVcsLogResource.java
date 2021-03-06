/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.vcs;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.obiba.opal.core.vcs.CommitInfo;
import org.obiba.opal.core.vcs.OpalGitException;
import org.obiba.opal.core.vcs.OpalVersionControlSystem;
import org.obiba.opal.core.vcs.support.OpalGitUtils;
import org.obiba.opal.web.Dtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.Nullable;

@Component
@Scope("request")
@Path("/datasource/{datasourceName}/view/{viewName}/vcs")
public class ViewVcsLogResource {

  @Autowired
  private OpalVersionControlSystem vcs;

  @PathParam("datasourceName")
  private String datasource;

  @PathParam("viewName")
  private String view;

  @GET
  @Path("/commits")
  public Response getCommitsInfo() {
    try {
      List<CommitInfo> commitInfos = vcs.getCommitsInfo(datasource, OpalGitUtils.getViewFilePath(view));
      return Response.ok().entity(Dtos.asDto(commitInfos)).build();
    } catch(OpalGitException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity("FailedToRetrieveViewCommitInfos").build();
    }
  }

  @GET
  @Path("/variable/{variableName}/commits")
  public Response getVariableCommitsInfo(@NotNull @PathParam("variableName") String variableName) {
    try {
      List<CommitInfo> commitInfos = vcs
          .getCommitsInfo(datasource, OpalGitUtils.getVariableFilePath(view, variableName));
      return Response.ok().entity(Dtos.asDto(commitInfos)).build();
    } catch(OpalGitException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity("FailedToRetrieveVariableCommitInfos").build();
    }
  }

  @GET
  @Path("/variable/{variableName}/commit/{commitId}")
  public Response getVariableCommitInfo(@NotNull @PathParam("variableName") String variableName,
      @NotNull @PathParam("commitId") String commitId) {

    try {
      String path = OpalGitUtils.getVariableFilePath(view, variableName);
      return Response.ok().entity(
          Dtos.asDto(getVariableDiffInternal(vcs.getCommitInfo(datasource, path, commitId), path, commitId, null)))
          .build();
    } catch(OpalGitException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity("FailedToRetrieveVariableCommitInfo").build();
    }
  }

  @GET
  @Path("/variable/{variableName}/commit/head/{commitId}")
  public Response getVariableCommitInfoFromHead(@NotNull @PathParam("variableName") String variableName,
      @NotNull @PathParam("commitId") String commitId) {

    try {
      String path = OpalGitUtils.getVariableFilePath(view, variableName);
      return Response.ok().entity(Dtos.asDto(
          getVariableDiffInternal(vcs.getCommitInfo(datasource, path, commitId), path, OpalGitUtils.HEAD_COMMIT_ID,
              commitId))).build();
    } catch(OpalGitException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity("FailedToRetrieveVariableCommitInfo").build();
    }
  }

  @GET
  @Path("/variable/{variableName}/blob/{commitId}")
  public Response getVariableContent(@NotNull @PathParam("variableName") String variableName,
      @NotNull @PathParam("commitId") String commitId) {
    try {
      String blob = vcs.getBlob(datasource, OpalGitUtils.getVariableFilePath(view, variableName), commitId);
      String path = OpalGitUtils.getVariableFilePath(view, variableName);
      CommitInfo commitInfo = CommitInfo.Builder.createFromObject(vcs.getCommitInfo(datasource, path, commitId))
          .setBlob(blob).build();
      return Response.ok().entity(Dtos.asDto(commitInfo)).build();
    } catch(OpalGitException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity("FailedToRetrieveVariableCommitInfo").build();
    }
  }

  private CommitInfo getVariableDiffInternal(@NotNull CommitInfo commitInfo, @NotNull String path,
      @NotNull String commitId, @Nullable String prevCommitId) {
    List<String> diffEntries = vcs.getDiffEntries(datasource, commitId, prevCommitId, path);
    return CommitInfo.Builder.createFromObject(commitInfo).setDiffEntries(diffEntries).build();
  }
}
