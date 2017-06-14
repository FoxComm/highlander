
import { assoc } from 'sprout-data';

function addEmptyDiscount(contentType) {
  const discount = {
    id: null,
    createdAt: null,
    attributes: {
      qualifier: {
        t: 'qualifier',
        v: {
          orderAny: {}
        }
      },
      offer: {
        t: 'offer',
        v: {
          orderPercentOff: {}
        }
      }
    },
  };

  contentType.discounts.push(discount);
  return contentType;
}

export function createEmptyContentType() {
  const contentType = {
    id: null,
    applyType: 'auto',
    isExclusive: true,
    createdAt: null,
    attributes: {
      storefrontName: {
        t: 'richText',
        v: 'Storefront name'
      },
      customerGroupIds: {
        t: 'tock673sjgmqbi5zlfx43o4px6jnxi7absotzjvxwir7jo2v',
        v: null,
      },
    },
    discounts: [],
  };

  return addEmptyDiscount(contentType);
}

export function setDiscountAttr(contentType, label, value) {
  return assoc(contentType,
    ['discounts', 0, 'attributes', label, 'v'], value,
  );
}
