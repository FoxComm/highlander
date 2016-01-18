
import React from 'react';
import types, { derivedTypes } from '../base/types';
import OrderTarget from '../base/order-target';

[
  types.ORDER_LINE_ITEMS_ADDED_GIFT_CARD,
  types.ORDER_LINE_ITEMS_DELETED_GIFT_CARD,
  types.ORDER_LINE_ITEMS_UPDATED_GIFT_CARD,
  types.ORDER_LINE_ITEMS_UPDATED_QUANTITIES,
  types.ORDER_LINE_ITEMS_UPDATED_QUANTITIES_BY_CUSTOMER,
  derivedTypes.ORDER_LINE_ITEMS_ADDED_SKU,
  derivedTypes.ORDER_LINE_ITEMS_REMOVED_SKU
];

const orderLineItemsChangedDesc = {
  title: (data, {kind}) => {
    const title = kind == derivedTypes.ORDER_LINE_ITEMS_ADDED_SKU ? 'added' : 'removed';

    return (
      <span>
        <strong>{title} {data.difference} of {data.skuName}</strong> on <OrderTarget order={data.order} />.
      </span>
    );
  },
};


const representatives = {
  [derivedTypes.ORDER_LINE_ITEMS_ADDED_SKU]: orderLineItemsChangedDesc,
  [derivedTypes.ORDER_LINE_ITEMS_REMOVED_SKU]: orderLineItemsChangedDesc,
};

export default representatives;
