/*******************************************************************************
 * Copyright (c) 2011 OBiBa. All rights reserved.
 *  
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web;

import javax.ws.rs.core.Response.ResponseBuilder;

import com.google.common.base.Preconditions;

/**
 * Returns the response provided by {@code Request#evaluatePreconditions()} methods
 */
public class UnsatisfiedPreconditionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ResponseBuilder builder;

  public UnsatisfiedPreconditionException(ResponseBuilder builder) {
    Preconditions.checkArgument(builder != null, "builder cannot be null");
    this.builder = builder;
  }

  public ResponseBuilder getResponse() {
    return builder;
  }

}
