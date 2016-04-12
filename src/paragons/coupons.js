
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

  const usageRulesShadow = {
    isExclusive: false,
    isUnlimitedPerCode: false,
    usesPerCode: 1,
    isUnlimitedPerCustomer: false,
    usesPerCustomer: 1,
  };

  const updatedForm = {...form, usageRules: usageRulesForm};
  const updatedShadow = {...shadow, usageRules: usageRulesShadow};

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
      usageRules: {},
    },
    shadow: {
      id: null,
      createdAt: null,
      attributes: {},
      usageRules: {},
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
  };

  denormalize(coupon);
  denormalize(coupon, 'usageRules');

  _.each(defaultAttrs, (type, label) => {
    const formAttribute = _.get(coupon, ['form', 'attributes', label]);
    if (formAttribute === void 0) {
      [form.attributes, shadow.attributes] = setAttribute(label, type, '', form.attributes, shadow.attributes);
    }
  });

  return coupon;
}
