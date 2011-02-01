/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.r;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.obiba.opal.r.StringAssignROperation;
import org.obiba.opal.r.service.OpalRSession;
import org.obiba.opal.r.service.OpalRSessionManager;
import org.obiba.opal.web.model.OpalR;

/**
 * Handles web services on a particular R session of the invoking Opal user.
 */
public class OpalRSessionResource extends AbstractOpalRSessionResource {

  private OpalRSessionManager opalRSessionManager;

  private OpalRSession rSession;

  public OpalRSessionResource(OpalRSessionManager opalRSessionManager, OpalRSession rSession) {
    super();
    this.opalRSessionManager = opalRSessionManager;
    this.rSession = rSession;
  }

  @GET
  public OpalR.RSessionDto getRSession() {
    return Dtos.asDto(rSession);
  }

  @DELETE
  public Response removeRSession() {
    opalRSessionManager.removeSubjectRSession(rSession.getId());
    return Response.ok().build();
  }

  @PUT
  @Path("/current")
  public Response setCurrentRSession() {
    opalRSessionManager.setSubjectCurrentRSession(rSession.getId());
    return Response.ok().build();
  }

  @GET
  @Path("/query")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response query(@QueryParam("script") String script) {
    return executeScript(rSession, script);
  }

  @GET
  @Path("/symbols")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response ls() {
    return executeScript(rSession, "base::ls()");
  }

  @POST
  @Path("/symbols")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response assign(MultivaluedMap<String, String> symbols) {
    rSession.execute(new StringAssignROperation(symbols));
    return ls();
  }

  @Path("/symbol/{name}")
  public RSymbolResource getRSymbolResource(@PathParam("name") String name) {
    return new RSymbolResource(rSession, name);
  }

  protected OpalRSession getOpalRSession() {
    return rSession;
  }

}