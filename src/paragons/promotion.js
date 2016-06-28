
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { denormalize, setAttribute, addAttribute } from './form-shadow-object';

function addEmptyDiscount(promotion) {
  const { form, shadow } = promotion;

  const discountForm = {
    id: null,
    createdAt: null,
    attributes: {},
  };

  const discountShadow = {
    id: null,
    createdAt: null,
    attributes: {},
  };

  const attrs = {
    'qualifier': {
      value: {orderAny: {}},
      label: 'qualifier'
    },
    'offer': {
      value: {orderPercentOff: {}},
      label: 'offer'
    }
  };

  _.each(attrs, ({value, label}, type) => {
    [discountForm.attributes, discountShadow.attributes] =
      addAttribute(label, type, value, discountForm.attributes, discountShadow.attributes);
  });

  form.discounts.push(discountForm);
  shadow.discounts.push(discountShadow);

  return promotion;
}


export function createEmptyPromotion() {
  const promotion = {
    form: {
      id: null,
      createdAt: null,
      attributes: {},
      discounts: [],
    },
    shadow: {
      id: null,
      createdAt: null,
      attributes: {},
      discounts: [],
    },
  };

  return addEmptyDiscount(configurePromotion(promotion));
}

export function configurePromotion(promotion) {
  const { form, shadow } = promotion;

  const defaultAttrs = {
    name: 'string',
    storefrontName: 'richText',
    description: 'richText',
    details: 'richText',
  };

  denormalize(promotion);
  denormalize(promotion, 'discounts');

  _.each(defaultAttrs, (type, label) => {
    const formAttribute = _.get(promotion, ['form', 'attributes', label]);
    if (formAttribute === void 0) {
      [form.attributes, shadow.attributes] = setAttribute(label, type, '', form.attributes, shadow.attributes);
    }
  });

  return promotion;
}

export function setDiscountAttr(promotion, label, type, value) {
  const formAttr = _.get(promotion, 'form.discounts.0.attributes', {});
  const shadowAttrs = _.get(promotion, 'shadow.discounts.0.attributes', {});

  const [newForm, newShadow] = setAttribute(label, type, value, formAttr, shadowAttrs);

  return assoc(promotion,
    ['form', 'discounts', 0, 'attributes'], newForm,
    ['shadow', 'discounts', 0, 'attributes'], newShadow
  );
}
