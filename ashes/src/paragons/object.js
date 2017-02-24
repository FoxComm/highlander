/* @flow */

import _ from 'lodash';

export function guessType(value: any): string {
  const typeOf = typeof value;
  switch (typeOf) {
    case 'string':
    case 'number':
      return typeOf;
    case 'boolean':
      return 'bool';
    default:
      return 'string';
  }
}

function supressor(object: Object): Object {
  if (object.attributes) {
    object.attributes = _.reduce(object.attributes, (acc, value, key) => {
      acc[key] = _.get(value, 'v', value);
      return acc;
    }, {});
  }
  return object;
}

function handleObject(object: Object, handler: Function): Object {
  if (!_.isPlainObject(object)) return object;
  object = handler(object);

  _.each(object, (value, key) => {
    if (_.isArray(value)) {
      _.set(object, key, _.map(value, item => handleObject(item, handler)));
    } else if (_.isPlainObject(value)) {
      _.set(object, key, handleObject(value, handler));
    }
  });

  return object;
}

export function supressTV(object: Object): Object {
  return handleObject(_.cloneDeep(object), supressor);
}
