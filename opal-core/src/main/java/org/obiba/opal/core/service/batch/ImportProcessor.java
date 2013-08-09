package org.obiba.opal.core.service.batch;

import java.io.IOException;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.obiba.opal.core.domain.batch.BatchImportConfig;
import org.obiba.opal.core.runtime.OpalRuntime;
import org.obiba.opal.core.service.ImportService;
import org.obiba.opal.core.service.NoSuchFunctionalUnitException;
import org.obiba.opal.core.unit.FunctionalUnitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Strings;

public class ImportProcessor implements ItemProcessor<BatchImportConfig, Void> {

  private static final Logger log = LoggerFactory.getLogger(ImportProcessor.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Override
  public Void process(BatchImportConfig importConfig) throws Exception {

    log.info(">>> applicationContext: {}", applicationContext);

    String unit = importConfig.getUnit();
    if(!Strings.isNullOrEmpty(unit) && !getFunctionalUnitService().hasFunctionalUnit(unit)) {
      throw new NoSuchFunctionalUnitException(unit);
    }

    String file = importConfig.getFile();
    FileObject fileObject = Strings.isNullOrEmpty(file) ? null : resolveFile(file, unit);

    if(!Strings.isNullOrEmpty(importConfig.getSource())) {
      log.info(">>> importFromDatasource");
      importFromDatasource(importConfig, fileObject);
    } else if(!Strings.isNullOrEmpty(importConfig.getTable())) {
      log.info(">>> importFromTable");
      importFromTable(importConfig, fileObject);
    } else if(fileObject != null) {
      log.info(">>> importFiles");
      importFiles(importConfig, fileObject);
    }
    return null;
  }

  private void importFromDatasource(BatchImportConfig importConfig, @Nullable FileObject file)
      throws IOException, InterruptedException {
    log.info("Importing datasource: {}", importConfig.getSource());
    getImportService()
        .importData(importConfig.getSource(), importConfig.getDestination(), importConfig.isForceUnknownIdCreation(),
            importConfig.isIgnoreUnknownIdentifier());
    if(file != null) archive(file, importConfig);
  }

  private void importFromTable(BatchImportConfig importConfig, @Nullable FileObject file)
      throws IOException, InterruptedException {
    log.info("Importing table: {}", importConfig.getTable());
    getImportService().importDataFromTable(importConfig.getTable(), importConfig.getDestination(),
        importConfig.isForceUnknownIdCreation(), importConfig.isIgnoreUnknownIdentifier());
    if(file != null) archive(file, importConfig);
  }

  private void importFiles(BatchImportConfig importConfig, FileObject file) throws IOException, InterruptedException {
    log.info("Importing file: {}", file.getName().getPath());
    getImportService().importData(importConfig.getUnit(), file, importConfig.getDestination(),
        importConfig.isForceUnknownIdCreation(), importConfig.isIgnoreUnknownIdentifier());
    archive(file, importConfig);
  }

  private void archive(FileObject file, BatchImportConfig importConfig) throws IOException {
    String archiveDir = importConfig.getArchiveDir();
    if(Strings.isNullOrEmpty(archiveDir)) {
      return;
    }

    try {
      FileObject archiveFile = isRelativeFilePath(archiveDir) ? getFileInUnitDirectory(archiveDir,
          importConfig.getUnit()) : getFile(archiveDir);
      if(archiveFile == null) {
        throw new IOException(
            "Cannot archive file " + file.getName().getPath() + ". Archive directory is null: " + archiveDir);
      }
      archiveFile.createFolder();
      file.moveTo(archiveFile.resolveFile(file.getName().getBaseName()));
    } catch(FileSystemException ex) {
      throw new IOException("Failed to archive file " + file.getName().getPath() + " to " + archiveDir);
    }
  }

  private FileObject resolveFile(String filePath, String unit) {
    try {
      FileObject file = getFileToImport(filePath, unit);
      if(file == null || !file.exists()) {
        log.warn("File {} does not exist, skipping import...", filePath);
        return null;
      }
      return file;
    } catch(FileSystemException e) {
      //noinspection StringConcatenationArgumentToLogCall
      log.warn("Cannot resolve the following path: " + filePath + ", skipping import...", e);
      return null;
    }
  }

  @Nullable
  private FileObject getFileToImport(String filePath, String unit) throws FileSystemException {
    return isRelativeFilePath(filePath) ? getFileInUnitDirectory(filePath, unit) : getFile(filePath);
  }

  @Nullable
  private FileObject getFileInUnitDirectory(String filePath, String unit) throws FileSystemException {
    if(!Strings.isNullOrEmpty(unit)) {
      FileObject unitDir = getFunctionalUnitService().getUnitDirectory(unit);
      return unitDir.resolveFile(filePath);
    }
    return null;
  }

  private boolean isRelativeFilePath(String filePath) {
    return !filePath.startsWith("/");
  }

  private FileObject getFile(String path) throws FileSystemException {
    return getOpalRuntime().getFileSystem().getRoot().resolveFile(path);
  }

  private OpalRuntime getOpalRuntime() {
    return applicationContext.getBean(OpalRuntime.class);
  }

  private FunctionalUnitService getFunctionalUnitService() {
    return applicationContext.getBean(FunctionalUnitService.class);
  }

  private ImportService getImportService() {
    return applicationContext.getBean(ImportService.class);
  }

}
