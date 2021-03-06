package org.obiba.opal.core.upgrade.database;

import org.obiba.opal.core.upgrade.AbstractConfigurationUpgradeStep;
import org.obiba.runtime.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class CopyVersionToOpalConfigurationUpgradeStep extends AbstractConfigurationUpgradeStep {

  /*
    <version>
      <major>2</major>
      <minor>0</minor>
      <micro>0</micro>
      <qualifier />
    </version>
  */
  @Override
  protected void doWithConfig(Document opalConfig) {

    Node major = opalConfig.createElement("major");
    major.setTextContent("2");
    Node minor = opalConfig.createElement("minor");
    minor.setTextContent("0");
    Node micro = opalConfig.createElement("micro");
    micro.setTextContent("0");
    Node qualifier = opalConfig.createElement("qualifier");
    qualifier.setTextContent("");

    Node version = opalConfig.createElement("version");
    version.appendChild(major);
    version.appendChild(minor);
    version.appendChild(micro);
    version.appendChild(qualifier);

    opalConfig.getFirstChild().appendChild(version);
  }

  @Override
  public String getDescription() {
    return "Drop version table from previous opal-data database because it's now moved to opal-config database";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(2, 0, 0);
  }
}
