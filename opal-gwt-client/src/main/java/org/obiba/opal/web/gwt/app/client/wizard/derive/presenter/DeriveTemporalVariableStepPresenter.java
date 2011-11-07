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

import org.obiba.opal.web.gwt.app.client.validator.ValidationHandler;
import org.obiba.opal.web.gwt.app.client.wizard.DefaultWizardStepController;
import org.obiba.opal.web.gwt.app.client.wizard.derive.helper.TemporalVariableDerivationHelper;
import org.obiba.opal.web.gwt.app.client.wizard.derive.view.ValueMapEntry;
import org.obiba.opal.web.model.client.magma.VariableDto;

import com.google.inject.Inject;

/**
 *
 */
public class DeriveTemporalVariableStepPresenter extends DerivationPresenter<DeriveTemporalVariableStepPresenter.Display> {

  private TemporalVariableDerivationHelper derivationHelper;

  @Inject
  public DeriveTemporalVariableStepPresenter(final Display display, final EventBus eventBus) {
    super(display, eventBus);
  }

  @Override
  void initialize(VariableDto variable) {
    super.initialize(variable);
    // TODO initialize when method is chosen

  }

  @Override
  public List<DefaultWizardStepController> getWizardSteps() {
    List<DefaultWizardStepController> stepCtrls = new ArrayList<DefaultWizardStepController>();

    stepCtrls.add(getDisplay().getMethodStepController().onValidate(new ValidationHandler() {

      @Override
      public boolean validate() {
        derivationHelper = new TemporalVariableDerivationHelper(originalVariable, getDisplay().getGroupMethod());
        getDisplay().populateValues(derivationHelper.getValueMapEntries());
        return true;
      }
    }).build());
    stepCtrls.add(getDisplay().getMapStepController().onValidate(new ValidationHandler() {

      @Override
      public boolean validate() {
        // TODO
        return true;
      }
    }).build());

    return stepCtrls;
  }

  @Override
  public VariableDto getDerivedVariable() {
    return derivationHelper.getDerivedVariable();
  }

  //
  // WidgetPresenter Methods
  //

  @Override
  public void refreshDisplay() {
  }

  @Override
  public void revealDisplay() {
  }

  @Override
  protected void onBind() {
  }

  @Override
  protected void onUnbind() {
  }

  @Override
  public Place getPlace() {
    return null;
  }

  @Override
  protected void onPlaceRequest(PlaceRequest request) {
  }

  //
  // Interfaces
  //

  public interface Display extends WidgetDisplay {

    DefaultWizardStepController.Builder getMethodStepController();

    DefaultWizardStepController.Builder getMapStepController();

    String getGroupMethod();

    void populateValues(List<ValueMapEntry> valuesMap);

  }

}