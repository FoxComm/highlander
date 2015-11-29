
global.importModule = function(modulePath, actionsToExport = []) {
  const path = `modules/${modulePath}`;
  return importSource(path, actionsToExport);
};
