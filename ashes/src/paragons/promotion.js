
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

  promotion.discounts1.push(discount);
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
    discounts1: [],
    discounts: [
     {
        id: null,
        createdAt: null,
        attributes: {
          qualifier: {
            t: 'qualifier',
            v: {
              discountType: 'order',
              qualifierType: 'noQualifier',
              widgetValue: 0,
              exGiftCardQual: true,
            }
          },
          offer: {
            t: 'offer',
            v: {
              offerType: 'orderPercentOff',
              exGiftCardOffer: true,
              widgetValue: 0,
            }
          }
        },
      },
    ]
  };

  return addEmptyDiscount(promotion);
}

export function setDiscountAttr(promotion, label, value) {
  return assoc(promotion,
    ['discounts', 0, 'attributes', label, 'v'], value,
  );
}
