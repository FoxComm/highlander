
import path from 'path';
import rewire from 'rewire';

global.localStorage = require('localStorage');

const modulesCache = {};

global.importModule = function(modulePath, actionsToExport = []) {
  const finalPath = path.resolve(`./src/modules/${modulePath}`);
  if (!(finalPath in modulesCache)) {
    const importedModule = rewire(finalPath);
    importedModule.reducer = importedModule['default'];

    actionsToExport.forEach(action => {
      importedModule[action] = importedModule.__get__(action);
    });

    modulesCache[finalPath] = importedModule;
  }

  return modulesCache[finalPath];
};
