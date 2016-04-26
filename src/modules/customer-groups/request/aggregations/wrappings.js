/* @flow */

import { isDirectField, getNestedPath } from '../helpers';


export function wrapInheritedDirectLeave(aggregation: Object): Object {
  console.log('wrapInheritedDirectLeave', aggregation);
  return {
    reverse_nested: {},
    aggregations: {
      [this.name]: aggregation,
    },
  };
}

export function wrapInheritedDirectParent(aggregation: Object, aggregations: Object): Object {
  console.log('wrapInheritedDirectParent', aggregation, aggregations);
  return {
    reverse_nested: {},
    aggregations: {
      [this.name]: aggregation,
      aggregations,
    },
  };
}

export function wrapPlainDirectLeave(aggregation: Object): Object {
  console.log('wrapPlainDirectLeave', aggregation);
  return aggregation;
}

export function wrapPlainDirectParent(aggregation: Object, aggregations: Object): Object {
  console.log('wrapPlainDirectParent', aggregation, aggregations);
  return {
    ...aggregation,
    aggregations,
  };
}

export function wrapPlainIndirectLeave(aggregation: Object): Object {
  console.log('wrapPlainIndirectLeave', aggregation);
  return {
    nested: {
      path: getNestedPath(this.field)
    },
    aggregations: {
      [this.name]: aggregation,
    }
  };
}

export function wrapPlainIndirectParent(aggregation: Object, aggregations: Object): Object {
  console.log('wrapPlainIndirectParent', aggregation, aggregations);
  return {
    nested: {
      path: getNestedPath(this.field)
    },
    aggregations: {
      [this.name]: {
        ...aggregation,
        aggregations,
      },
    },
  };
}
