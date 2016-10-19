/* @flow weak */

import { singularize, pluralize } from 'fleck';

export function numberize(entity, count) {
  return count === 1 ? singularize(entity) : pluralize(entity);
}

export function prefix(prefix) {
  return function (suffix) {
    return suffix ? prefix + '__' + suffix : prefix;
  };
}

export function stripTags(input: string): string {
  return input.replace(/<(?:.|\n)*?>/gm, '');
}
