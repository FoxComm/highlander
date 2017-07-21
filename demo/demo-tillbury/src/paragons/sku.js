// @flow
import _ from 'lodash';

import type { Sku } from 'types/sku';

const giftCardAttrs = ['message', 'senderName', 'recipientName', 'recipientEmail'];

export function isGiftCard(sku: Sku): boolean {
  const attrs = _.get(sku.attributes, 'giftCard');
  if (!attrs) {
    return false;
  }
  return _.every(giftCardAttrs, name => name in attrs);
}
