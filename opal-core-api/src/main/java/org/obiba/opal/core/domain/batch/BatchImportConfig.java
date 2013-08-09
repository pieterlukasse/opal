package org.obiba.opal.core.domain.batch;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@SuppressWarnings("ParameterHidesMemberVariable")
@Entity
@Table(name = "batch_import_config")
public class BatchImportConfig implements Serializable {

  private static final long serialVersionUID = 1923224550722152371L;

  @Id
  @GeneratedValue
  private Long id;

  @Nonnull
  private String jobId;

  /**
   * User that started this job
   */
  @Nonnull
  @Column(name = "username")
  private String user;

  /**
   * The functional unit. If supplied, imported identifiers must exist in opal, otherwise use 'forceUnknownIdCreation' option.
   * If no unit is supplied, identifiers are imported as is.
   */
  private String unit;

  /**
   * The destination datasource into which the variable catalogue and the participants data will be imported.
   */
  @Nonnull
  private String destination;

  /**
   * Import specified file.
   */
  private String file;

  /**
   * Archive directory. If a relative path is given, it is relative to the functional unit's directory.
   */
  private String archiveDir;

  /**
   * Copy all tables from this datasource.
   */
  private String source;

  /**
   * Forces participant creation when an unknown participant's identifier is encountered in a functional unit.
   * Ignored when no functional unit is specified.
   */
  private boolean forceUnknownIdCreation;

  /**
   * Import specified table.
   */
  @Column(name = "table_to_import")
  private String table;

  /**
   * Ignore participants having an unknown identifier in a functional unit.
   * Ignored when no functional unit is specified or when participant creation is not allowed.
   */
  private boolean ignoreUnknownIdentifier;

  /**
   * Use incremental import
   */
  private boolean incremental;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Nonnull
  public String getJobId() {
    return jobId;
  }

  public void setJobId(@Nonnull String jobId) {
    this.jobId = jobId;
  }

  @Nonnull
  public String getUser() {
    return user;
  }

  public void setUser(@Nonnull String user) {
    this.user = user;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  @Nonnull
  public String getDestination() {
    return destination;
  }

  public void setDestination(@Nonnull String destination) {
    this.destination = destination;
  }

  public String getArchiveDir() {
    return archiveDir;
  }

  public void setArchiveDir(String archiveDir) {
    this.archiveDir = archiveDir;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public boolean isForceUnknownIdCreation() {
    return forceUnknownIdCreation;
  }

  public void setForceUnknownIdCreation(boolean forceUnknownIdCreation) {
    this.forceUnknownIdCreation = forceUnknownIdCreation;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public boolean isIgnoreUnknownIdentifier() {
    return ignoreUnknownIdentifier;
  }

  public void setIgnoreUnknownIdentifier(boolean ignoreUnknownIdentifier) {
    this.ignoreUnknownIdentifier = ignoreUnknownIdentifier;
  }

  public boolean isIncremental() {
    return incremental;
  }

  public void setIncremental(boolean incremental) {
    this.incremental = incremental;
  }

  public BatchImportConfig withJobId(String jobId) {
    this.jobId = jobId;
    return this;
  }

  public BatchImportConfig withUser(String user) {
    this.user = user;
    return this;
  }

  public BatchImportConfig withUnit(String unit) {
    this.unit = unit;
    return this;
  }

  public BatchImportConfig withDestination(String destination) {
    this.destination = destination;
    return this;
  }

  public BatchImportConfig withFile(String file) {
    this.file = file;
    return this;
  }

  public BatchImportConfig withArchiveDir(String archiveDir) {
    this.archiveDir = archiveDir;
    return this;
  }

  public BatchImportConfig withSource(String source) {
    this.source = source;
    return this;
  }

  public BatchImportConfig withForceUnknownIdCreation(boolean forceUnknownIdCreation) {
    this.forceUnknownIdCreation = forceUnknownIdCreation;
    return this;
  }

  public BatchImportConfig withTable(String table) {
    this.table = table;
    return this;
  }

  public BatchImportConfig withIgnoreUnknownIdentifier(boolean ignoreUnknownIdentifier) {
    this.ignoreUnknownIdentifier = ignoreUnknownIdentifier;
    return this;
  }

  public BatchImportConfig withIncremental(boolean incremental) {
    this.incremental = incremental;
    return this;
  }

}
