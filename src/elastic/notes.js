import * as dsl from './dsl';
import { addNativeFilters } from './common';

export default function processQuery({ entityType, entityId }, query) {
  switch (entityType) {
    case 'customer':
      return addNativeFilters(query, [
        dsl.nestedTermFilter('customer.id', entityId),
        dsl.existsFilter('deletedAt', 'missing'),
      ]);
    case 'order':
      return addNativeFilters(query, [
        dsl.nestedMatchFilter('order.referenceNumber', entityId),
        dsl.existsFilter('deletedAt', 'missing'),
      ]);
    case 'gift-card':
      return addNativeFilters(query, [
        dsl.nestedTermFilter('giftCard.code', entityId),
        dsl.existsFilter('deletedAt', 'missing'),
      ]);
    case 'inventory-item':
      return addNativeFilters(query, [
        dsl.nestedTermFilter('inventoryItem.code', entityId),
        dsl.existsFilter('deletedAt', 'missing'),
      ]);
    default:
      return addNativeFilters(query, [
        dsl.nestedTermFilter(`${entityType}.id`, entityId),
        dsl.existsFilter('deletedAt', 'missing'),
      ]);
  }

  return query;
}
