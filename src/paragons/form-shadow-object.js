/**
 * @flow
 */
import _ from 'lodash';
import { assoc } from 'sprout-data';

export type FormShadowObject = {
  form: FormAttributes;
  shadow: ShadowAttributes;
};

export function addAttribute(label: string,
                             type: string,
                             value: any,
                             form: FormAttributes,
                             shadow: ShadowAttributes): FormShadowObject {
  if (shadow[label]) {
    return { form, shadow };
  }

  const formValue = type == 'price' ? { currency: 'USD', value: value } : value;
  const newFormAttr = { [label]: formValue };
  const newShadowAttr = { [label]: { type: type, ref: label } };

  return {
    form: { ...form, ...newFormAttr },
    shadow: { ...shadow, ...newShadowAttr },
  };
}

export function setAttribute(label: string,
                             type: string,
                             value: any,
                             form: FormAttributes,
                             shadow: ShadowAttributes): FormShadowObject {
  const shadowAttribute = _.get(shadow, label);
  if (!shadowAttribute) {
    return addAttribute(label, type, value, form, shadow);
  }

  const formValue = type == 'price'
    ? { currency: 'USD', value: value }
    : value;

  const newForm = assoc(form, label, formValue);
  const newShadow = assoc(shadow, [label, 'ref'], label);

  return { form: newForm, shadow: newShadow };
}

export function illuminateAttributes(form: FormAttributes,
                                     shadow: ShadowAttributes): IlluminatedAttributes {
  return _.reduce(shadow, (res, shadow, label) => {
    const attribute = form[shadow.ref];

    res[label] = {
      label: label,
      type: shadow.type,
      value: attribute,
    };

    return res;
  }, {});
}
