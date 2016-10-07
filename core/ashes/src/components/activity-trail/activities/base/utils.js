
import _ from 'lodash';

export function joinEntities(entities) {
  const result = _.slice(entities, 0, 1);
  for (let i = 1, len = entities.length; i < len; i++) {
    const sep = (i == entities.length - 1) ? ' and ' : ', ';
    result.push(sep, entities[i]);
  }

  return result;
}
