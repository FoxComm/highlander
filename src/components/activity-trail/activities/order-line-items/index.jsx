
// libs
import React from 'react';
import types, { derivedTypes } from '../base/types';

// components
import CordTarget from '../base/cord-target';
import GiftCardLink from '../base/gift-card-link';
import Title from '../base/title';

const orderLineItemsChangedDesc = {
  title: (data, activity) => {
    const cord = data.order || data.cart;
    const title = activity.kind == derivedTypes.CART_LINE_ITEMS_ADDED_SKU ? 'added' : 'removed';

    return (
      <Title activity={activity}>
        <strong>{title} {data.difference} of {data.skuName}</strong> on <CordTarget cord={cord} />
      </Title>
    );
  },
};

const actionTitleByType = {
  [types.CART_LINE_ITEMS_ADDED_GIFT_CARD]: ['added', 'to'],
  [types.CART_LINE_ITEMS_DELETED_GIFT_CARD]: ['deleted', 'from'],
  [types.CART_LINE_ITEMS_UPDATED_GIFT_CARD]: ['updated', 'on'],
};

const orderLineItemsGcDesc = {
  title: (data, activity) => {
    const [actionTitle, pretext] = actionTitleByType[activity.kind];
    const cord = data.order || data.cart;

    return (
      <Title activity={activity}>
        <strong>{actionTitle} gift card</strong>
        &nbsp;<GiftCardLink {...data.gc} /> {pretext} <CordTarget cord={cord} />
      </Title>
    );
  },
};

const representatives = {
  [derivedTypes.CART_LINE_ITEMS_ADDED_SKU]: orderLineItemsChangedDesc,
  [derivedTypes.CART_LINE_ITEMS_REMOVED_SKU]: orderLineItemsChangedDesc,
  [types.CART_LINE_ITEMS_ADDED_GIFT_CARD]: orderLineItemsGcDesc,
  [types.CART_LINE_ITEMS_DELETED_GIFT_CARD]: orderLineItemsGcDesc,
  [types.CART_LINE_ITEMS_UPDATED_GIFT_CARD]: orderLineItemsGcDesc,
};

export default representatives;
