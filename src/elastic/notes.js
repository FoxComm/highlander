
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
        dsl.nestedTermFilter('order.referenceNumber', entityId),
      ]);
    case 'gift-card':
      return addNativeFilters(query, [
        dsl.nestedTermFilter('giftCard.code', entityId),
      ]);
  }

  return query;
}
