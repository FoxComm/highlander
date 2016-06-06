
import _ from 'lodash';
import { denormalize, setAttribute, addAttribute } from './form-shadow-object';

export function createEmptyCoupon() {
  const usageRules = {
    isExclusive: false,
    isUnlimitedPerCode: false,
    usesPerCode: 1,
    isUnlimitedPerCustomer: false,
    usesPerCustomer: 1,
  };

  const coupon = {
    form: {
      id: null,
      createdAt: null,
      attributes: {
        usageRules
      },
    },
    shadow: {
      id: null,
      createdAt: null,
      attributes: {
        usageRules: { type: 'usageRules', ref: 'usageRules' },
      },
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
    usageRules: 'usageRules',
  };

  denormalize(coupon);

  _.each(defaultAttrs, (type, label) => {
    const formAttribute = _.get(coupon, ['form', 'attributes', label]);
    if (formAttribute === void 0) {
      [form.attributes, shadow.attributes] = setAttribute(label, type, '', form.attributes, shadow.attributes);
    }
  });

  return coupon;
}
