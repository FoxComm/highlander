// libs
import _ from 'lodash';

export function getStorePath(storePath, entity, scope, ...args) {
  const path = storePath ? [storePath] : [];

  if (_.isObject(entity)) {
    path.push(entity.entityType, scope, entity.entityId);
  } else {
    path.push(entity, scope);
  }

  return path.concat(args);
}
