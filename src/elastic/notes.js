
import * as dsl from './dsl';
import { addNativeFilters } from './common';

export default function processQuery({entityType, entityId}, query) {
  switch (entityType) {
    case 'customer':
      return addNativeFilters(query, [{
        bool: {
          should: [
            dsl.nestedTermFilter('order.customerId', entityId),
            dsl.nestedTermFilter('customer.id', entityId),
          ],
          minimum_number_should_match: 1
        }
      }]);
    case 'order':
      return addNativeFilters(query, [
        dsl.nestedMatchFilter('order.referenceNumber', entityId),
      ]);
    case 'gift-card':
      return addNativeFilters(query, [
        dsl.nestedTermFilter('giftCard.code', entityId),
      ]);
    case 'inventory-item':
      return addNativeFilters(query, [
        dsl.nestedTermFilter('inventoryItem.code', entityId),
      ]);
    default:
      return addNativeFilters(query, [
        dsl.nestedTermFilter(`${entityType}.id`, entityId),
      ]);
  }

  return query;
}
