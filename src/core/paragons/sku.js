import _ from 'lodash';

const giftCardAttrs = ['message', 'senderName', 'recipientName', 'recipientEmail'];

export function isGiftCard(sku) {
  const attrs = sku.attributes;
  if (!attrs) {
    return false;
  }
  return _.every(giftCardAttrs, name => name in attrs);
}
