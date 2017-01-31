/* @flow */

import { isDirectField, getNestedPath } from '../helpers';


export function wrapInheritedDirectLeave(aggregation: Object): Object {
  return {
    reverse_nested: {},
    aggs: {
      [this.name]: aggregation,
    },
  };
}

export function wrapInheritedDirectParent(aggregation: Object, aggregations: Object): Object {
  return {
    reverse_nested: {},
    aggs: {
      [this.name]: aggregation,
      aggregations,
    },
  };
}

export function wrapPlainDirectLeave(aggregation: Object): Object {
  return aggregation;
}

export function wrapPlainDirectParent(aggregation: Object, aggregations: Object): Object {
  return {
    ...aggregation,
    aggregations,
  };
}

export function wrapPlainIndirectLeave(aggregation: Object): Object {
  return {
    nested: {
      path: getNestedPath(this.field)
    },
    aggs: {
      [this.name]: aggregation,
    }
  };
}

export function wrapPlainIndirectParent(aggregation: Object, aggregations: Object): Object {
  return {
    nested: {
      path: getNestedPath(this.field)
    },
    aggs: {
      [this.name]: {
        ...aggregation,
        aggregations,
      },
    },
  };
}
