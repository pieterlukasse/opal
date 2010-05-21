/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.presenter;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.obiba.opal.web.gwt.app.client.event.SessionExpiredEvent;
import org.obiba.opal.web.gwt.app.client.event.WorkbenchChangeEvent;
import org.obiba.opal.web.gwt.rest.client.RequestCredentials;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 *
 */
public class ApplicationPresenter extends WidgetPresenter<ApplicationPresenter.Display> {

  public interface Display extends WidgetDisplay {

    HasClickHandlers getQuit();

    MenuItem getExploreVariables();

    void updateWorkbench(Widget workbench);

    MenuItem getDataImportItem();
  }

  private final RequestCredentials credentials;

  private final Provider<NavigatorPresenter> navigationPresenter;

  private final Provider<DataImportPresenter> dataImportPresenter;

  private WidgetPresenter<?> workbench;

  /**
   * @param display
   * @param eventBus
   */
  @Inject
  public ApplicationPresenter(final Display display, final EventBus eventBus, Provider<NavigatorPresenter> navigationPresenter, Provider<DataImportPresenter> dataImportPresenter, RequestCredentials credentials) {
    super(display, eventBus);
    this.navigationPresenter = navigationPresenter;
    this.credentials = credentials;
    this.dataImportPresenter = dataImportPresenter;
  }

  @Override
  public Place getPlace() {
    return null;
  }

  @Override
  protected void onBind() {

    getDisplay().getExploreVariables().setCommand(new Command() {

      @Override
      public void execute() {
        eventBus.fireEvent(new WorkbenchChangeEvent(navigationPresenter.get()));
      }
    });

    getDisplay().getDataImportItem().setCommand(new Command() {

      @Override
      public void execute() {
        DataImportPresenter presenter = dataImportPresenter.get();
        presenter.bind();
        presenter.revealDisplay();
      }
    });

    getDisplay().getQuit().addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        credentials.invalidate();
        if(workbench != null) {
          workbench.unbind();
          // Need to send to some type of no-workbench place.
        }
        eventBus.fireEvent(new SessionExpiredEvent());
      }
    });

    super.registerHandler(eventBus.addHandler(WorkbenchChangeEvent.getType(), new WorkbenchChangeEvent.Handler() {

      @Override
      public void onWorkbenchChanged(WorkbenchChangeEvent event) {
        if(workbench != null) {
          workbench.unbind();
        }
        workbench = event.getWorkbench();
        workbench.bind();
        WidgetDisplay wd = (WidgetDisplay) workbench.getDisplay();
        getDisplay().updateWorkbench(wd.asWidget());
        workbench.revealDisplay();
      }
    }));
  }

  @Override
  protected void onPlaceRequest(PlaceRequest request) {
  }

  @Override
  protected void onUnbind() {
  }

  @Override
  public void refreshDisplay() {
  }

  @Override
  public void revealDisplay() {
  }

}
