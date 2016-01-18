
// libs
import React from 'react';
import types, { derivedTypes } from '../base/types';

// components
import OrderTarget from '../base/order-target';
import GiftCardLink from '../base/gift-card-link';

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

const actionTitleByType = {
  [types.ORDER_LINE_ITEMS_ADDED_GIFT_CARD]: 'added',
  [types.ORDER_LINE_ITEMS_DELETED_GIFT_CARD]: 'deleted',
  [types.ORDER_LINE_ITEMS_UPDATED_GIFT_CARD]: 'updated',
};

const orderLineItemsGcDesc = {
  title: (data, {kind}) => {
    const actionTitle = actionTitleByType[kind];

    return (
      <span>
        <strong>{actionTitle} gift card</strong> <GiftCardLink {...data.gc} /> to <OrderTarget order={data.order} />.
      </span>
    );
  },
};

const representatives = {
  [derivedTypes.ORDER_LINE_ITEMS_ADDED_SKU]: orderLineItemsChangedDesc,
  [derivedTypes.ORDER_LINE_ITEMS_REMOVED_SKU]: orderLineItemsChangedDesc,
  [types.ORDER_LINE_ITEMS_ADDED_GIFT_CARD]: orderLineItemsGcDesc,
  [types.ORDER_LINE_ITEMS_DELETED_GIFT_CARD]: orderLineItemsGcDesc,
  [types.ORDER_LINE_ITEMS_UPDATED_GIFT_CARD]: orderLineItemsGcDesc,
};

export default representatives;
