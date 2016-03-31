/**
 * @flow
 */
import _ from 'lodash';
import { assoc } from 'sprout-data';

type FormShadowObject = {
  form: FormAttributes,
  shadow: ShadowAttributes,
};

type FormAttributes = { [key:string]: FormAttribute };
type FormAttribute = {
  type: string,
  [key:string]: any,
};

type ShadowAttributes = { [key:string]: ShadowAttribute };
type ShadowAttribute = {
  type: string,
  ref: string,
};

function addAttribute(label: string,
                      type: string,
                      value: any,
                      form: FormAttributes,
                      shadow: ShadowAttributes): FormShadowObject {
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

  const newForm = type == 'price'
    ? assoc(form, [shadowAttribute.ref, 'value'], value)
    : assoc(form, shadowAttribute.ref, value);  

  return { form: newForm, shadow: shadow };
}
