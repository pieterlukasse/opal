package org.obiba.opal.shell.commands.options;

import uk.co.flamingpenguin.jewel.cli.CommandLineInterface;
import uk.co.flamingpenguin.jewel.cli.Option;

/**
 * This interface declares the options that may be used with the "key" command.
 */
@CommandLineInterface(application = "keystore")
public interface KeyCommandOptions extends HelpOption {
  @Option(shortName = "u", description = "The functional unit. Defaults to 'OpalInstance'.")
  public String getUnit();

  public boolean isUnit();

  @Option(shortName = "a", description = "The alias name of the encryption key pair.")
  public String getAlias();

  @Option(shortName = "d", description = "Delete a key pair from the keystore.")
  public boolean isDelete();

  @Option(shortName = "g", longName = "algo", description = "The algorithm for creating the key pair. RSA is recommended.")
  public String getAlgorithm();

  public boolean isAlgorithm();

  @Option(shortName = "s", description = "The key size for creating the key pair.")
  public int getSize();

  public boolean isSize();

  @Option(shortName = "p", description = "Provides the private key file.")
  public String getPrivate();

  public boolean isPrivate();

  @Option(shortName = "c", description = "Provides the certified public key file that matches the private key file. If omitted the user is prompted to create one.")
  public String getCertificate();

  public boolean isCertificate();
}
