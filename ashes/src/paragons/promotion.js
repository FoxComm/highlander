
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
          orderPercentOff: {}
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
    isExclusive: true,
    createdAt: null,
    qualifyAll: true,
    qualifiedCustomerGroupIds: [],
    attributes: {},
    discounts: [],
  };

  return addEmptyDiscount(promotion);
}

export function setDiscountAttr(promotion, label, value) {
  return assoc(promotion,
    ['discounts', 0, 'attributes', label, 'v'], value,
  );
}
