// @flow
// lang utils

import _ from 'lodash';

// this function can be useful for business logic that determines passed value is defined or not
// it returns false only for two values: '' and undefined.
// in other cases value considered as defined
// isDefined('') = false
// isDefined(undefined) = false
// isDefined(true) = true
// isDefined(1) = true
// isDefined(null) = true
// isDefined(0) = true
export function isDefined(value: any): boolean {
  return value !== void 0 && (!_.isString(value) || value);
}
