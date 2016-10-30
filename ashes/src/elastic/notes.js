import _ from 'lodash';
import * as dsl from './dsl';
import { addNativeFilters } from './common';

export default function processQuery({ entityType, entityId }, query) {
  /** convert to camelCased types as in ES index */
  const type = _.camelCase(entityType);

  query = addNativeFilters(query,[
    dsl.termFilter('referenceType', type),
    dsl.existsFilter('deletedAt', 'missing'),
  ]);

  switch (entityType) {
    case 'order':
      return addNativeFilters(query, [
        dsl.nestedTermFilter('order.referenceNumber', entityId),
      ]);
    case 'gift-card':
      return addNativeFilters(query, [
        dsl.nestedTermFilter('giftCard.code', entityId),
      ]);
    default:
      return addNativeFilters(query, [
        dsl.termFilter('referenceId', entityId),
      ]);
  }
}
