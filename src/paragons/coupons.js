
import _ from 'lodash';
import { copyShadowAttributes, setAttribute } from './form-shadow-object';

export function createEmptyCoupon() {
  const coupon = {
    form: {
      id: null,
      createdAt: null,
      attributes: {},
    },
    shadow: {
      id: null,
      createdAt: null,
      attributes: {},
    },
    promotion: null
  };

  return configureCoupon(coupon);
}

export function configureCoupon(coupon) {
  const { form, shadow } = coupon;

  const defaultAttrs = {
    name: 'string',
    storefrontName: 'richText',
    description: 'text',
    details: 'richText',
  };

  copyShadowAttributes(form.attributes, shadow.attributes);

  _.each(defaultAttrs, (type, label) => {
    const formAttribute = _.get(coupon, ['form', 'attributes', label]);
    if (formAttribute === void 0) {
      [form.attributes, shadow.attributes] = setAttribute(label, type, '', form.attributes, shadow.attributes);
    }
  });

  return coupon;
}
