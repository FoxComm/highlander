
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { copyShadowAttributes, denormalize, setAttribute, addAttribute } from './form-shadow-object';

/*
format:
{
  form: {
    id: 228,
    attributes: FormAttributes,
    discounts: Array<Form>,
    createdAt: "2016-04-05T17:09:27.684Z"
  },
  shadow: {
    id: 228,
    formId: 228,
    attributes: ShadowAttributes,
    discounts: Array<Shadow>,
    createdAt: "2016-04-05T17:09:27.684Z"
  }
}
*/

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

  [discountForm.attributes, discountShadow.attributes] =
    addAttribute('qualifier', 'qualifier', {orderAny: {}}, discountForm.attributes, discountShadow.attributes);

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
