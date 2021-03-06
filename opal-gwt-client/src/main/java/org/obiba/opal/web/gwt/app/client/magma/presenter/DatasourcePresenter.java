/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.magma.presenter;

import javax.annotation.Nullable;

import org.obiba.opal.web.gwt.app.client.fs.event.FileDownloadRequestEvent;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.magma.copydata.presenter.DataCopyPresenter;
import org.obiba.opal.web.gwt.app.client.magma.createview.presenter.CreateViewStepPresenter;
import org.obiba.opal.web.gwt.app.client.magma.event.DatasourceSelectionChangeEvent;
import org.obiba.opal.web.gwt.app.client.magma.event.TableSelectionChangeEvent;
import org.obiba.opal.web.gwt.app.client.magma.exportdata.presenter.DataExportPresenter;
import org.obiba.opal.web.gwt.app.client.magma.importdata.presenter.DataImportPresenter;
import org.obiba.opal.web.gwt.app.client.magma.importvariables.presenter.VariablesImportPresenter;
import org.obiba.opal.web.gwt.app.client.magma.table.presenter.TablePropertiesModalPresenter;
import org.obiba.opal.web.gwt.app.client.presenter.ModalProvider;
import org.obiba.opal.web.gwt.app.client.ui.wizard.event.WizardRequiredEvent;
import org.obiba.opal.web.gwt.rest.client.HttpMethod;
import org.obiba.opal.web.gwt.rest.client.ResourceAuthorizationRequestBuilderFactory;
import org.obiba.opal.web.gwt.rest.client.ResourceCallback;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
import org.obiba.opal.web.gwt.rest.client.UriBuilder;
import org.obiba.opal.web.gwt.rest.client.UriBuilders;
import org.obiba.opal.web.gwt.rest.client.authorization.CascadingAuthorizer;
import org.obiba.opal.web.gwt.rest.client.authorization.HasAuthorization;
import org.obiba.opal.web.model.client.magma.DatasourceDto;
import org.obiba.opal.web.model.client.magma.TableDto;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Response;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.View;

public class DatasourcePresenter extends PresenterWidget<DatasourcePresenter.Display>
    implements DatasourceUiHandlers, DatasourceSelectionChangeEvent.Handler {

  private final ModalProvider<TablePropertiesModalPresenter> tablePropertiesModalProvider;

  private String datasourceName;

  private JsArray<TableDto> tables;

  private DatasourceDto datasource;

  @Inject
  public DatasourcePresenter(Display display, EventBus eventBus, ModalProvider<TablePropertiesModalPresenter> tablePropertiesModalProvider) {
    super(eventBus, display);
    this.tablePropertiesModalProvider = tablePropertiesModalProvider.setContainer(this);
    getView().setUiHandlers(this);
  }

  @Override
  protected void onBind() {
    super.onBind();
    addRegisteredHandler(DatasourceSelectionChangeEvent.getType(), this);
  }

  private int getTableIndex(String tableName) {
    int tableIndex = 0;
    for(int i = 0; i < tables.length(); i++) {
      if(tables.get(i).getName().equals(tableName)) {
        tableIndex = i;
        break;
      }
    }
    return tableIndex;
  }

  private void selectTable(String tableName) {
    if(tableName != null) {
      int index = getTableIndex(tableName);
      getView().setTableSelection(tables.get(index), index);
    }
  }

  private void downloadMetadata() {
    fireEvent(new FileDownloadRequestEvent("/datasource/" + datasourceName + "/tables/excel"));
  }

  private void initDatasource() {
    // rely on 304
    ResourceRequestBuilderFactory.<DatasourceDto>newBuilder() //
        .forResource(UriBuilders.DATASOURCE.create().build(datasourceName)) //
        .withCallback(new InitResourceCallback()) //
        .get().send();
  }

  @Override
  public void onDatasourceSelectionChanged(DatasourceSelectionChangeEvent event) {
    datasourceName = event.getSelection();
    initDatasource();
  }

  @Override
  public void onImportData() {
    fireEvent(new WizardRequiredEvent(DataImportPresenter.WizardType, datasourceName));
  }

  @Override
  public void onExportData() {
    fireEvent(new WizardRequiredEvent(DataExportPresenter.WizardType, datasourceName));
  }

  @Override
  public void onCopyData() {
    fireEvent(new WizardRequiredEvent(DataCopyPresenter.WizardType, datasourceName));
  }

  @Override
  public void onAddTable() {
    TablePropertiesModalPresenter p = tablePropertiesModalProvider.get();
    p.initialize(datasource);
  }

  @Override
  public void onAddUpdateTables() {
    fireEvent(new WizardRequiredEvent(VariablesImportPresenter.WIZARD_TYPE, datasourceName));
  }

  @Override
  public void onAddView() {
    fireEvent(new WizardRequiredEvent(CreateViewStepPresenter.WizardType, datasourceName));
  }

  @Override
  public void onDownloadDictionary() {
    downloadMetadata();
  }

  //
  // Interfaces and classes
  //

  private class InitResourceCallback implements ResourceCallback<DatasourceDto> {
    @Override
    public void onResource(Response response, DatasourceDto resource) {
      datasource = resource;
      displayDatasource(datasource, null);
    }

    private void displayDatasource(DatasourceDto datasourceDto, @Nullable String table) {
      getView().setDatasource(datasourceDto);
      updateTable(table);
      authorize();

      if(table == null) {
        getView().afterRenderRows();
      } else {
        selectTable(table);
      }
    }

    private void updateTable(@Nullable String tableName) {
      UriBuilder ub = UriBuilders.DATASOURCE_TABLES.create().query("counts", "true");
      ResourceRequestBuilderFactory.<JsArray<TableDto>>newBuilder().forResource(ub.build(datasourceName)).get()
          .withCallback(new TablesResourceCallback(datasourceName, tableName)).send();
    }

    private void authorize() {
      authorizeDatasource();
      authorizeProject();
    }

    private void authorizeDatasource() {
      String datasourceUri = UriBuilder.create().segment("datasource", datasourceName).build();
      // create tables
      ResourceAuthorizationRequestBuilderFactory.newBuilder() //
          .forResource(datasourceUri + "/tables") //
          .authorize(getView().getAddUpdateTablesAuthorizer()) //
          .post().send();
      // create views
      ResourceAuthorizationRequestBuilderFactory.newBuilder() //
          .forResource(datasourceUri + "/views") //
          .authorize(getView().getAddViewAuthorizer()) //
          .post().send();
      // export variables in excel
      ResourceAuthorizationRequestBuilderFactory.newBuilder() //
          .forResource(datasourceUri + "/tables/excel") //
          .authorize(getView().getExcelDownloadAuthorizer()) //
          .get().send();
    }

    private void authorizeProject() {
      String projectUri = UriBuilder.create().segment("project", datasourceName).build();
      // export data
      ResourceAuthorizationRequestBuilderFactory.newBuilder() //
          .forResource(projectUri + "/commands/_copy") //
          .authorize(CascadingAuthorizer.newBuilder() //
              .and("/functional-units", HttpMethod.GET) //
              .and("/functional-units/entities/table", HttpMethod.GET) //
              .authorize(getView().getExportDataAuthorizer()).build()) //
          .post().send();
      // copy data
      ResourceAuthorizationRequestBuilderFactory.newBuilder() //
          .forResource(projectUri + "/commands/_copy") //
          .authorize(getView().getCopyDataAuthorizer()) //
          .post().send();
      // import data
      ResourceAuthorizationRequestBuilderFactory.newBuilder() //
          .forResource(projectUri + "/commands/_import") //
          .authorize(CascadingAuthorizer.newBuilder() //
              .and("/functional-units", HttpMethod.GET) //
              .and("/functional-units/entities/table", HttpMethod.GET) //
              .authorize(getView().getImportDataAuthorizer()).build()) //
          .post().send();
    }
  }

  private final class TablesResourceCallback implements ResourceCallback<JsArray<TableDto>> {

    private final String datasourceName;

    private final String selectTableName;

    private TablesResourceCallback(String datasourceName, String selectTableName) {
      this.datasourceName = datasourceName;
      this.selectTableName = selectTableName;
      getView().beforeRenderRows();
    }

    @Override
    public void onResource(Response response, JsArray<TableDto> resource) {
      if(datasourceName.equals(DatasourcePresenter.this.datasourceName)) {
        tables = JsArrays.toSafeArray(resource);
        getView().renderRows(resource);
        selectTable(selectTableName);
        getView().afterRenderRows();
      }
    }
  }

  public interface Display extends View, HasUiHandlers<DatasourceUiHandlers> {

    void setTableSelection(TableDto variable, int index);

    void beforeRenderRows();

    void renderRows(JsArray<TableDto> rows);

    void afterRenderRows();

    void setDatasource(DatasourceDto dto);

    HasAuthorization getAddUpdateTablesAuthorizer();

    HasAuthorization getAddViewAuthorizer();

    HasAuthorization getImportDataAuthorizer();

    HasAuthorization getExportDataAuthorizer();

    HasAuthorization getCopyDataAuthorizer();

    HasAuthorization getExcelDownloadAuthorizer();
  }
}
