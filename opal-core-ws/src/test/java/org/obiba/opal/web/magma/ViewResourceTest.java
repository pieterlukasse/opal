/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.magma;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;
import org.obiba.magma.ValueTable;
import org.obiba.magma.views.View;
import org.obiba.opal.web.magma.view.JavaScriptViewDtoExtension;
import org.obiba.opal.web.magma.view.VariableListViewDtoExtension;
import org.obiba.opal.web.magma.view.ViewDtoExtension;
import org.obiba.opal.web.magma.view.ViewDtos;

import com.google.common.collect.ImmutableSet;

/**
 * Unit tests for {@link ViewResource}.
 */
public class ViewResourceTest {
  //
  // Test Methods
  //

  @Test
  public void testConstructor_CallsSuperConstructorWithViewArgument() {
    // Setup
    View view = new View();

    // Exercise
    ViewResource sut = new ViewResource(view, newViewDtos());

    // Verify
    assertEquals(view, sut.getValueTable());
  }

  @Test
  public void testGetFrom_ReturnsTableResourceForWrappedTable() {
    // Setup
    ValueTable fromTableMock = createMock(ValueTable.class);
    expect(fromTableMock.getName()).andReturn("fromTable").atLeastOnce();

    View view = new View("testView", fromTableMock);
    ViewResource sut = new ViewResource(view, newViewDtos(), ImmutableSet.of(new Locale("en")));

    replay(fromTableMock);

    // Exercise
    TableResource fromTableResource = sut.getFrom();

    // Verify state
    assertEquals("fromTable", fromTableResource.getValueTable().getName());
  }

  private ViewDtos newViewDtos() {
    return new ViewDtos(ImmutableSet.<ViewDtoExtension> of(new JavaScriptViewDtoExtension(), new VariableListViewDtoExtension()));
  }
}
