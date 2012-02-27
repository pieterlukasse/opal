/*******************************************************************************
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *  
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.rest.client.magma;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.obiba.magma.Value;
import org.obiba.magma.ValueTableWriter;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableEntity;
import org.obiba.opal.web.magma.Dtos;
import org.obiba.opal.web.model.Magma.ValueSetDto;
import org.obiba.opal.web.model.Magma.VariableDto;
import org.obiba.opal.web.model.Magma.VariableDto.Builder;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

class RestValueTableWriter implements ValueTableWriter {

  private final RestValueTable restValueTable;

  RestValueTableWriter(RestValueTable restValueTable) {
    this.restValueTable = restValueTable;
  }

  @Override
  public void close() throws IOException {

  }

  @Override
  public VariableWriter writeVariables() {
    return new VariableWriter() {

      private final List<Variable> variables = Lists.newArrayList();

      @Override
      public void close() throws IOException {
        URI variablesResource = restValueTable.newReference("variables");
        Iterable<VariableDto> variableDtos = Iterables.transform(variables, Functions.compose(new Function<VariableDto.Builder, VariableDto>() {

          @Override
          public VariableDto apply(Builder input) {
            return input.build();
          }
        }, Dtos.asDtoFunc(null)));

        checkResponse(restValueTable.getOpalClient().post(variablesResource, variableDtos));
        restValueTable.refresh();
      }

      @Override
      public void writeVariable(Variable variable) {
        variables.add(variable);
      }

    };
  }

  @Override
  public ValueSetWriter writeValueSet(final VariableEntity entity) {

    return new ValueSetWriter() {

      private ValueSetDto.Builder valueSetDtoBuilder = ValueSetDto.newBuilder().setEntity(Dtos.asDto(entity));

      @Override
      public void close() throws IOException {
        URI valueSetUri = restValueTable.newReference("valueSet");
        checkResponse(restValueTable.getOpalClient().post(valueSetUri, valueSetDtoBuilder.build()));
      }

      @Override
      public void writeValue(Variable variable, Value value) {
        valueSetDtoBuilder.addVariables(variable.getName()).addValues(Dtos.asDto(value));
      }
    };
  }

  private void checkResponse(HttpResponse response) throws IOException {
    EntityUtils.consume(response.getEntity());
    if(response.getStatusLine().getStatusCode() >= 400) {
      throw new IOException(response.getStatusLine().getReasonPhrase());
    }
  }
}