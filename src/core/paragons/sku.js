import _ from 'lodash';

const giftCardAttrs = ['message', 'senderName', 'recipientName', 'recipientEmail'];

export function isGiftCard(sku) {
  return _.every(giftCardAttrs, name => name in sku.attributes);
}
