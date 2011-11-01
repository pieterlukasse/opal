/*******************************************************************************
 * Copyright (c) 2011 OBiBa. All rights reserved.
 *  
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.wizard.derive.presenter;

import java.util.ArrayList;
import java.util.List;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.obiba.opal.web.gwt.app.client.event.NotificationEvent;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.widgets.presenter.SummaryTabPresenter;
import org.obiba.opal.web.gwt.app.client.wizard.DefaultWizardStepController;
import org.obiba.opal.web.gwt.app.client.wizard.Wizard;
import org.obiba.opal.web.gwt.app.client.wizard.WizardStepController.StepInHandler;
import org.obiba.opal.web.gwt.app.client.wizard.WizardType;
import org.obiba.opal.web.gwt.app.client.wizard.event.WizardRequiredEvent;
import org.obiba.opal.web.gwt.rest.client.ResourceCallback;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
import org.obiba.opal.web.gwt.rest.client.ResponseCodeCallback;
import org.obiba.opal.web.model.client.magma.CategoryDto;
import org.obiba.opal.web.model.client.magma.DatasourceDto;
import org.obiba.opal.web.model.client.magma.TableDto;
import org.obiba.opal.web.model.client.magma.ValueDto;
import org.obiba.opal.web.model.client.magma.VariableDto;
import org.obiba.opal.web.model.client.magma.ViewDto;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.inject.Inject;

/**
 *
 */
public class DeriveVariablePresenter extends WidgetPresenter<DeriveVariablePresenter.Display> implements Wizard {

  public static final int PAGE_SIZE = 12;

  @Inject
  private DeriveCategoricalVariableStepPresenter categoricalPresenter;

  @Inject
  private DeriveNumericalVariableStepPresenter numericalPresenter;

  @Inject
  private SummaryTabPresenter summaryTabPresenter;

  private VariableDto variable;

  private TableDto table;

  private JsArray<DatasourceDto> datasources;

  private DerivationPresenter<?> derivationPresenter;

  private int currentOffset;

  private String currentScript;

  //
  // Constructors
  //

  @Inject
  public DeriveVariablePresenter(final Display display, final EventBus eventBus) {
    super(display, eventBus);
  }

  //
  // Wizard Methods
  //

  @Override
  public void onWizardRequired(WizardRequiredEvent event) {
    if(event.getEventParameters().length != 1) {
      throw new IllegalArgumentException("Variable DTO is expected as first wizard argument.");
    }

    if(!(event.getEventParameters()[0] instanceof VariableDto)) {
      throw new IllegalArgumentException("unexpected event parameter type (expected VariableDto)");
    }

    variable = (VariableDto) event.getEventParameters()[0];
    getDisplay().setDefaultDerivedName(variable.getName());

    ResourceRequestBuilderFactory.<TableDto> newBuilder().forResource(variable.getParentLink().getLink()).get().withCallback(new ResourceCallback<TableDto>() {
      @Override
      public void onResource(Response response, TableDto resource) {
        table = resource;
      }

    }).send();

    updateDatasources();

    if(event.getWizardType() == WizardType.DERIVE_CATEGORIZE_VARIABLE) {
      // TODO
      String valueType = variable.getValueType();
      derivationPresenter = null;
      if(valueType.equals("integer") || valueType.equals("decimal")) {
        derivationPresenter = numericalPresenter;
      } else if(valueType.equals("text") && variable.getCategoriesArray() != null && variable.getCategoriesArray().length() > 0) {
        derivationPresenter = categoricalPresenter;
      }

      if(derivationPresenter != null) {
        derivationPresenter.setOriginalVariable(variable);
        getDisplay().appendWizardSteps(derivationPresenter.getWizardSteps());
      }
    } else {
      // TODO
    }
  }

  private void updateDatasources() {
    ResourceRequestBuilderFactory.<JsArray<DatasourceDto>> newBuilder().forResource("/datasources").get().withCallback(new DatasourcesCallback()).send();
  }

  private boolean hasTableInFrom(ViewDto resource) {
    if(resource.getFromArray() == null) return false;
    JsArrayString froms = resource.getFromArray();
    for(int i = 0; i < froms.length(); i++) {
      String from = froms.get(i);
      if(from.equals(table.getDatasourceName() + "." + table.getName())) {
        return true;
      }
    }
    return false;
  }

  private void populateValues(final int offset) {
    currentOffset = offset;
    ResourceRequestBuilderFactory.<JsArray<ValueDto>> newBuilder() //
    .forResource(table.getLink() + "/variable/_transient/values?limit=" + PAGE_SIZE + "&offset=" + offset + "&script=" + URL.encodeQueryString(currentScript)).get() //
    .withCallback(new ResourceCallback<JsArray<ValueDto>>() {

      @Override
      public void onResource(Response response, JsArray<ValueDto> resource) {
        int high = offset + PAGE_SIZE;
        if(resource != null && resource.length() < high) {
          high = offset + resource.length();
        }
        getDisplay().setPageLimits(offset + 1, high, table.getValueSetCount());
        getDisplay().populateValues(resource);
      }

    })//
    .send();
  }

  //
  // WidgetPresenter Methods
  //

  @Override
  public void refreshDisplay() {
  }

  @Override
  public void revealDisplay() {
    getDisplay().showDialog();
  }

  @Override
  protected void onBind() {
    // TODO bind only the relevant one
    categoricalPresenter.bind();
    numericalPresenter.bind();

    summaryTabPresenter.bind();
    getDisplay().setSummaryTabWidget(summaryTabPresenter.getDisplay());
    getDisplay().setSummaryStepInHandler(new SummaryStepInHandler());
    addEventHandlers();
  }

  @Override
  protected void onUnbind() {
    // TODO unbind only the relevant one
    categoricalPresenter.unbind();
    numericalPresenter.unbind();

    summaryTabPresenter.unbind();
  }

  @Override
  public Place getPlace() {
    return null;
  }

  @Override
  protected void onPlaceRequest(PlaceRequest request) {
  }

  protected void addEventHandlers() {
    super.registerHandler(getDisplay().addCancelClickHandler(new CancelClickHandler()));
    super.registerHandler(getDisplay().addFinishClickHandler(new FinishClickHandler()));
    super.registerHandler(getDisplay().addNextPageClickHandler(new NextPageClickHandler()));
    super.registerHandler(getDisplay().addPreviousPageClickHandler(new PreviousPageClickHandler()));
  }

  //
  // Inner classes and Interfaces
  //

  public class PreviousPageClickHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      if(currentOffset > 0) {
        populateValues(currentOffset - PAGE_SIZE);
      }
    }

  }

  public class NextPageClickHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      if(currentOffset + PAGE_SIZE < table.getValueSetCount()) {
        populateValues(currentOffset + PAGE_SIZE);
      }
    }

  }

  private final class DatasourcesCallback implements ResourceCallback<JsArray<DatasourceDto>> {
    @Override
    public void onResource(Response response, JsArray<DatasourceDto> resources) {
      datasources = JsArrays.toSafeArray(resources);
      getDisplay().populateDatasources(datasources);
      for(DatasourceDto ds : JsArrays.toIterable(datasources)) {
        addViewSuggestions(ds);
      }
    }

    private void addViewSuggestions(final DatasourceDto ds) {
      JsArrayString array = ds.getViewArray();
      if(array != null) {
        for(int j = 0; j < array.length(); j++) {
          final String viewName = array.get(j);
          ResourceRequestBuilderFactory.<ViewDto> newBuilder().forResource("/datasource/" + ds.getName() + "/view/" + viewName).get().withCallback(new ResourceCallback<ViewDto>() {

            @Override
            public void onResource(Response response, ViewDto resource) {
              if(hasTableInFrom(resource)) {
                getDisplay().addViewSuggestion(ds, viewName);
              }
            }

          }).send();

        }
      }
    }
  }

  final class SummaryStepInHandler implements StepInHandler {
    @Override
    public void onStepIn() {
      VariableDto derived = derivationPresenter.getDerivedVariable();
      currentScript = derivationPresenter.getScript(derived);
      requestSummary(derived);
      populateValues(0);
    }

    private void requestSummary(final VariableDto derived) {
      StringBuilder summaryLink = new StringBuilder(table.getLink() + "/variable/_transient/summary");
      summaryLink.append("?valueType=" + derived.getValueType());
      summaryLink.append("&script=" + URL.encodeQueryString(currentScript));

      JsArray<CategoryDto> cats = derived.getCategoriesArray();
      if(cats != null) {
        for(int i = 0; i < cats.length(); i++) {
          summaryLink.append("&category=" + URL.encodeQueryString(cats.get(i).getName()));
        }
      }
      summaryTabPresenter.setResourceUri(summaryLink.toString());
      summaryTabPresenter.refreshDisplay();
    }

  }

  class CancelClickHandler implements ClickHandler {

    public void onClick(ClickEvent arg0) {
      getDisplay().hideDialog();
    }
  }

  class FinishClickHandler implements ClickHandler {

    public void onClick(ClickEvent arg0) {

      final String datasourceName = getDisplay().getDatasourceName();
      final String viewName = getDisplay().getViewName();

      // validation
      List<String> errorMessages = new ArrayList<String>();
      if(getDisplay().getDerivedName().isEmpty()) {
        // TODO translate
        errorMessages.add("Derived variable name is required.");
      }
      if(viewName.isEmpty()) {
        // TODO translate
        errorMessages.add("Destination view name is required.");
      } else {
        // if destination table exists, it must be a view
        for(DatasourceDto ds : JsArrays.toIterable(datasources)) {
          if(ds.getName().equals(datasourceName)) {
            for(int i = 0; i < ds.getTableArray().length(); i++) {
              String tName = ds.getTableArray().get(i);
              if(tName.equals(viewName)) {
                boolean isView = false;
                for(int j = 0; j < ds.getViewArray().length(); j++) {
                  String vName = ds.getViewArray().get(j);
                  if(vName.equals(viewName)) {
                    isView = true;
                  }
                }
                if(!isView) {
                  // TODO translate
                  errorMessages.add("A derived variable can only be added to a view.");
                }
                break;
              }
            }
          }
        }
      }

      if(errorMessages.size() > 0) {
        eventBus.fireEvent(NotificationEvent.newBuilder().error(errorMessages).build());
        return;
      }

      final VariableDto derived = derivationPresenter.getDerivedVariable();
      derived.setName(getDisplay().getDerivedName());

      ResourceRequestBuilderFactory.<ViewDto> newBuilder().forResource("/datasource/" + datasourceName + "/view/" + viewName).get().withCallback(new ResourceCallback<ViewDto>() {

        @Override
        public void onResource(Response response, ViewDto resource) {
          if(!hasTableInFrom(resource)) {
            // TODO translate
            eventBus.fireEvent(NotificationEvent.newBuilder().error("Not a valid destination view.").build());
          } else {
            GWT.log("add variable to view: " + viewName);
            getDisplay().hideDialog();
          }
        }

      }).withCallback(404, new ResponseCodeCallback() {

        @Override
        public void onResponseCode(Request request, Response response) {
          // TODO
          GWT.log("view to be created: " + viewName);
          getDisplay().hideDialog();
        }
      }).send();

    }
  }

  public interface Display extends WidgetDisplay {

    HandlerRegistration addCancelClickHandler(ClickHandler handler);

    void addViewSuggestion(DatasourceDto ds, String viewName);

    void populateDatasources(JsArray<DatasourceDto> datasources);

    HandlerRegistration addFinishClickHandler(ClickHandler handler);

    void showDialog();

    void hideDialog();

    void clear();

    void setSummaryTabWidget(WidgetDisplay widget);

    void setSummaryStepInHandler(StepInHandler handler);

    void appendWizardSteps(List<DefaultWizardStepController> stepCtrls);

    void populateValues(JsArray<ValueDto> values);

    void setDefaultDerivedName(String name);

    String getDerivedName();

    String getDatasourceName();

    String getViewName();

    HandlerRegistration addNextPageClickHandler(ClickHandler handler);

    HandlerRegistration addPreviousPageClickHandler(ClickHandler handler);

    void setPageLimits(int low, int high, int count);

    boolean isOpenEditorSelected();
  }

}
