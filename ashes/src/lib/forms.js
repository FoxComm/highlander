
import _ from 'lodash';

export function idGenerator(prefix) {
  let nextIdCounter = 0;
  let lastNextId = null;

  return function() {
    if (nextIdCounter++ % 2) {
      return lastNextId;
    }

    return lastNextId = _.uniqueId(prefix);
  };
}
