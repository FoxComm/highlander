/**
 * @flow
 */
import _ from 'lodash';
import { assoc } from 'sprout-data';

export type FormShadowAttrsPair = [FormAttributes, ShadowAttributes];

export function copyShadowAttributes(form: FormAttributes, shadow: ShadowAttributes) {
  _.forEach(shadow, (s, label) => {
    // update form
    form[label] = form[s.ref];
    // update shadow
    s.ref = label;
  });
}

function addAttribute(label: string,
                      type: string,
                      value: any,
                      form: FormAttributes,
                      shadow: ShadowAttributes): FormShadowAttrsPair {
  if (shadow[label]) {
    return [ form, shadow ];
  }

  const formValue = type == 'price' ? { currency: 'USD', value: value } : value;
  const newFormAttr = { [label]: formValue };
  const newShadowAttr = { [label]: { type: type, ref: label } };

  return [
    { ...form, ...newFormAttr },
    { ...shadow, ...newShadowAttr },
  ];
}

export function setAttribute(label: string,
                             type: string,
                             value: any,
                             form: FormAttributes,
                             shadow: ShadowAttributes): FormShadowAttrsPair {
  const shadowAttribute = _.get(shadow, label);
  if (!shadowAttribute) {
    return addAttribute(label, type, value, form, shadow);
  }

  const formValue = type == 'price'
    ? { currency: 'USD', value: value }
    : value;

  const newForm = assoc(form, label, formValue);
  const newShadow = assoc(shadow, [label, 'ref'], label);

  return [newForm, newShadow];
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
