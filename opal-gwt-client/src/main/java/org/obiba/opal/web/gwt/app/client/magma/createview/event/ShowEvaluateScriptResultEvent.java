/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.magma.createview.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ShowEvaluateScriptResultEvent extends GwtEvent<ShowEvaluateScriptResultEvent.Handler> {

  private static final Type<Handler> TYPE = new Type<Handler>();

  private final boolean showResults;

  public ShowEvaluateScriptResultEvent(boolean showResults) {
    this.showResults = showResults;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onShowEvaluateScriptResultEvent(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return getType();
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  public interface Handler extends EventHandler {
    void onShowEvaluateScriptResultEvent(ShowEvaluateScriptResultEvent event);
  }

  public boolean isResultsShown() {
    return showResults;
  }

}
