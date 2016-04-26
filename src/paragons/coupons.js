
import _ from 'lodash';
import { denormalize, setAttribute, addAttribute } from './form-shadow-object';

function addEmptyUsageRules(coupon) {
  const { form, shadow } = coupon;

  const usageRulesForm = {
    isExclusive: false,
    isUnlimitedPerCode: false,
    usesPerCode: 1,
    isUnlimitedPerCustomer: false,
    usesPerCustomer: 1,
  };

  const updatedForm = {...form};
  const updatedShadow = {...shadow};

  return {
    ...coupon,
    form: updatedForm,
    shadow: updatedShadow
  };
}

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

  return addEmptyUsageRules(configureCoupon(coupon));
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
