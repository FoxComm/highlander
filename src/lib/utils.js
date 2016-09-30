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


// see http://stackoverflow.com/a/12628791
// cartesianProductOf([1, 2], [3, 4], ['a', 'b'])
// =>                 [[1,3,"a"],[1,3,"b"],[1,4,"a"],[1,4,"b"],[2,3,"a"],[2,3,"b"],[2,4,"a"],[2,4,"b"]]
export function cartesianProductOf(...args) {
  return _.reduce(args, (a, b) => {
    return _.flatten(_.map(a, x => {
      return _.map(b, y => {
        return x.concat([y]);
      });
    }), true);
  }, [ [] ]);
}
