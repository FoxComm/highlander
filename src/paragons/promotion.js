
import _ from 'lodash';
import { copyShadowAttributes, setAttribute } from './form-shadow-object';

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

  return configurePromotion(promotion);
}

export function configurePromotion(promotion) {
  const { form, shadow } = promotion;

  const defaultAttrs = {
    name: 'string',
    storefrontName: 'richText',
    description: 'richText',
    details: 'richText',
  };

  copyShadowAttributes(form.attributes, shadow.attributes);

  _.each(defaultAttrs, (type, label) => {
    const formAttribute = _.get(promotion, ['form', 'attributes', label]);
    if (formAttribute === void 0) {
      [form.attributes, shadow.attributes] = setAttribute(label, type, '', form.attributes, shadow.attributes);
    }
  });

  return promotion;
}
