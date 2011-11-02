/*******************************************************************************
 * Copyright (c) 2011 OBiBa. All rights reserved.
 *  
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.wizard.derive.helper;

import org.obiba.opal.web.model.client.magma.VariableDto;

public class DuplicationHelper extends DerivationHelper {

  public DuplicationHelper(VariableDto originalVariable) {
    super(originalVariable);
  }

  @Override
  protected void initializeValueMapEntries() {

  }

  @Override
  public VariableDto getDerivedVariable() {
    VariableDto derived = copyVariable(originalVariable, true);
    setScript(derived, "$('" + originalVariable.getName() + "')");
    return derived;
  }

}