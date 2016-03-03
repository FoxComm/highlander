
// libs
import React from 'react';
import types, { derivedTypes } from '../base/types';

// components
import OrderTarget from '../base/order-target';
import GiftCardLink from '../base/gift-card-link';
import Title from '../base/title';

const orderLineItemsChangedDesc = {
  title: (data, activity) => {
    const title = activity.kind == derivedTypes.ORDER_LINE_ITEMS_ADDED_SKU ? 'added' : 'removed';

    return (
      <Title activity={activity}>
        <strong>{title} {data.difference} of {data.skuName}</strong> on <OrderTarget order={data.order} />
      </Title>
    );
  },
};

const actionTitleByType = {
  [types.ORDER_LINE_ITEMS_ADDED_GIFT_CARD]: ['added', 'to'],
  [types.ORDER_LINE_ITEMS_DELETED_GIFT_CARD]: ['deleted', 'from'],
  [types.ORDER_LINE_ITEMS_UPDATED_GIFT_CARD]: ['updated', 'on'],
};

const orderLineItemsGcDesc = {
  title: (data, activity) => {
    const [actionTitle, pretext] = actionTitleByType[activity.kind];

    return (
      <Title activity={activity}>
        <strong>{actionTitle} gift card</strong>
        &nbsp;<GiftCardLink {...data.gc} /> {pretext} <OrderTarget order={data.order} />
      </Title>
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
