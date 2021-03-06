/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.gwt.app.client.project.view;

import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.project.presenter.ProjectAdministrationPresenter;
import org.obiba.opal.web.gwt.app.client.project.presenter.ProjectAdministrationUiHandlers;
import org.obiba.opal.web.model.client.opal.ProjectDto;

import com.github.gwtbootstrap.client.ui.Paragraph;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class ProjectAdministrationView extends ViewWithUiHandlers<ProjectAdministrationUiHandlers>
    implements ProjectAdministrationPresenter.Display {

  interface Binder extends UiBinder<Widget, ProjectAdministrationView> {}

  private final Translations translations;

  @UiField
  Label name;

  @UiField
  Label title;

  @UiField
  Label description;

  @UiField
  Paragraph databaseText;

  @UiField
  Label dbName;

  @UiField
  Label dbType;

  @UiField
  Panel databasePanel;

  @UiField
  Panel noDatabasePanel;

  private ProjectDto project;

  @Inject
  public ProjectAdministrationView(Binder uiBinder, Translations translations) {
    initWidget(uiBinder.createAndBindUi(this));
    this.translations = translations;
  }

  @Override
  public void setProject(ProjectDto project) {
    this.project = project;
    name.setText(project.getName());
    title.setText(project.getTitle());
    description.setText(project.getDescription());
    noDatabasePanel.setVisible(!project.hasDatabase());
    databasePanel.setVisible(project.hasDatabase());
    if(project.hasDatabase()) {
      databaseText.setText(translations.projectDatabaseName());
      dbName.setText(project.getDatabase());
      dbType.setText(translations.datasourceTypeMap().get(project.getDatasource().getType()));
    } else {
      //noDatabasePanel.clear();
      //noDatabasePanel.add(new Label(translations.noProjectDatabase()));
    }
  }

  @Override
  public ProjectDto getProject() {
    return project;
  }

  @UiHandler("editProperties")
  void onEditProperties(ClickEvent event) {
    getUiHandlers().onEdit();
  }

  @UiHandler("deleteProject")
  void onDeleteProject(ClickEvent event) {
    getUiHandlers().onDelete();
  }

}
