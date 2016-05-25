import * as dsl from './dsl';
import { addNativeFilters } from './common';

export default function processQuery({ entityType, entityId }, query) {
  switch (entityType) {
    case 'sku':
      return addNativeFilters(query, [
        dsl.termFilter('referenceType', entityType),
        dsl.nestedTermFilter('skuItem.sku', entityId),
        dsl.existsFilter('deletedAt', 'missing'),
      ]);
    default:
      return addNativeFilters(query, [
        dsl.termFilter('referenceType', entityType),
        dsl.termFilter('referenceId', entityId),
        dsl.existsFilter('deletedAt', 'missing'),
      ]);
  }

  return query;
}
