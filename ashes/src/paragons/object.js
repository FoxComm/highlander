/* @flow */

import _ from 'lodash';

export type AttrSchema = {
  type: string,
  title?: string,
  widget?: string,
  properties?: Object,
  disabled?: boolean,
};

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

export function setObjectAttr(obj: Object, key: string, attr: Attribute): Object {
  _.set(obj, ['attributes', key, 'v'], attr.v);
  _.set(obj, ['attributes', key, 't'], attr.t);
  return obj;
}

export function omitObjectAttr(obj: Object, key: string): Object {
  _.unset(obj, ['attributes', key]);
  return obj;
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
