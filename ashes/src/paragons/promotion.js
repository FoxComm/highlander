
import { assoc } from 'sprout-data';

function addEmptyDiscount(promotion) {
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
          orderPercentOff: {
            discount: 0,
          }
        }
      }
    },
  };

  promotion.discounts.push(discount);
  return promotion;
}

export function createEmptyPromotion() {
  const promotion = {
    id: null,
    applyType: 'auto',
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
      usageRules: {
        t: 'PromoUsageRules',
        v: {
          isExclusive: true,
        }
      }
    },
    discounts: [],
  };

  return addEmptyDiscount(promotion);
}

export function setDiscountAttr(promotion, label, value) {
  return assoc(promotion,
    ['discounts', 0, 'attributes', label, 'v'], value,
  );
}
