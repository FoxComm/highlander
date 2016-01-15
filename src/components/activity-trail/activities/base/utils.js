
import _ from 'lodash';

function injectValue(array, value) {
  let withValues = _.slice(array, 0, 1);
  for (let i = 1, len = array.length; i < len; i++) {
    withValues.push(value, array[i]);
  }

  return withValues;
}

export function joinEntities(entities) {
  const withCommas = injectValue(_.initial(entities), ', ');
  return _.flatten(injectValue([withCommas, [_.last(entities)]], ' and '));
}
