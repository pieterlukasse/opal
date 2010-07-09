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

import java.util.Arrays;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.obiba.opal.web.gwt.app.client.presenter.ErrorDialogPresenter.MessageDialogType;
import org.obiba.opal.web.gwt.app.client.widgets.presenter.FileSelectionPresenter;
import org.obiba.opal.web.gwt.app.client.widgets.presenter.FileSelectorPresenter.FileSelectionType;
import org.obiba.opal.web.gwt.rest.client.ResourceCallback;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
import org.obiba.opal.web.gwt.rest.client.ResponseCodeCallback;
import org.obiba.opal.web.model.client.DatasourceDto;
import org.obiba.opal.web.model.client.FunctionalUnitDto;
import org.obiba.opal.web.model.client.ImportCommandOptionsDto;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.inject.Inject;

public class DataImportPresenter extends WidgetPresenter<DataImportPresenter.Display> {

  public interface Display extends DataCommonPresenter.Display {

    String getSelectedFile();

    void setUnits(JsArray<FunctionalUnitDto> units);

    String getArchiveDirectory();

    String getSelectedUnit();

    void setFileWidgetDisplay(FileSelectionPresenter.Display display);

    void setArchiveWidgetDisplay(FileSelectionPresenter.Display display);
  }

  @Inject
  private ErrorDialogPresenter errorDialog;

  @Inject
  private FileSelectionPresenter fileSelectionPresenter;

  @Inject
  private FileSelectionPresenter archiveSelectionPresenter;

  /**
   * @param display
   * @param eventBus
   */
  @Inject
  public DataImportPresenter(final Display display, final EventBus eventBus) {
    super(display, eventBus);
  }

  public DataImportPresenter(final Display display, final EventBus eventBus, FileSelectionPresenter fileSelectionPresenter, FileSelectionPresenter archiveSelectionPresenter) {
    this(display, eventBus);
    this.fileSelectionPresenter = fileSelectionPresenter;
    this.archiveSelectionPresenter = archiveSelectionPresenter;
  }

  @Override
  public Place getPlace() {
    return null;
  }

  @Override
  protected void onBind() {
    initDisplayComponents();
    addEventHandlers();
  }

  protected void addEventHandlers() {
    getDisplay().getSubmit().addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        ImportCommandOptionsDto dto = ImportCommandOptionsDto.create();
        dto.setDestination(getDisplay().getSelectedDatasource());
        dto.setArchive(getDisplay().getArchiveDirectory());
        JsArrayString selectedFiles = JavaScriptObject.createArray().cast();
        selectedFiles.push(getDisplay().getSelectedFile());
        dto.setFilesArray(selectedFiles);
        dto.setUnit(getDisplay().getSelectedUnit());
        ResourceRequestBuilderFactory.newBuilder().forResource("/shell/import").post().withResourceBody(ImportCommandOptionsDto.stringify(dto)).withCallback(400, new ResponseCodeCallback() {

          @Override
          public void onResponseCode(Request request, Response response) {
            errorDialog.bind();
            errorDialog.setErrors(Arrays.asList(new String[] { response.getText() }));
            errorDialog.revealDisplay();
          }
        }).withCallback(201, new ResponseCodeCallback() {

          @Override
          public void onResponseCode(Request request, Response response) {
            String location = response.getHeader("Location");
            String jobId = location.substring(location.lastIndexOf('/') + 1);

            errorDialog.bind();
            errorDialog.setMessageDialogType(MessageDialogType.INFO);
            errorDialog.setErrors(Arrays.asList(new String[] { "The 'import' job has been launched with ID#" + jobId + "." }));
            errorDialog.revealDisplay();
          }
        }).send();
      }

    });
  }

  protected void initDisplayComponents() {
    fileSelectionPresenter.setFileSelectionType(FileSelectionType.EXISTING_FILE);
    fileSelectionPresenter.bind();
    getDisplay().setFileWidgetDisplay(fileSelectionPresenter.getDisplay());

    archiveSelectionPresenter.setFileSelectionType(FileSelectionType.FOLDER);
    archiveSelectionPresenter.bind();
    getDisplay().setArchiveWidgetDisplay(archiveSelectionPresenter.getDisplay());

    ResourceRequestBuilderFactory.<JsArray<DatasourceDto>> newBuilder().forResource("/datasources").get().withCallback(new ResourceCallback<JsArray<DatasourceDto>>() {
      @Override
      public void onResource(Response response, JsArray<DatasourceDto> datasources) {
        getDisplay().setDatasources(datasources);
      }
    }).send();

    ResourceRequestBuilderFactory.<JsArray<FunctionalUnitDto>> newBuilder().forResource("/functional-units").get().withCallback(new ResourceCallback<JsArray<FunctionalUnitDto>>() {
      @Override
      public void onResource(Response response, JsArray<FunctionalUnitDto> units) {
        getDisplay().setUnits(units);
      }
    }).send();

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
