/* @flow */

import _ from 'lodash';
import type { Dictionary } from './types';
import { isDefined } from 'lib/lang';

export type Attribute = { t: string, v: any };
export type Attributes = Dictionary<Attribute>;

type Constraint = {
  label?: string,
  required?: boolean,
}

type ObjectConstraints = {
  [name: string]: Constraint
}

type Object = {
  attributes: Attributes,
}

export function isSatisfied(object: Object, constraints: ObjectConstraints) {
  return _.reduce(constraints, (res, val, key) => {
    const isRequired: boolean = _.get(val, 'required', false);
    if (!isRequired) {
      return res;
    }

    const attr = _.get(object, ['attributes', key, 'v']);
    return res && isDefined(attr);
  }, true);
}
