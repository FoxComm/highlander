/**
 * @flow
 */
import _ from 'lodash';
import { assoc } from 'sprout-data';

type Attributes = { [key:string]: {
  t: string,
  v: any,
}};

export function copyShadowAttributes(form: FormAttributes, shadow: ShadowAttributes) {
  _.forEach(shadow, (s, label) => {
    // update form
    form[label] = form[s.ref];
    // update shadow
    s.ref = label;
  });
}

export function addAttribute(label: string,
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

export function addIlluminatedAttribute(label: string,
                                         type: string,
                                         value: any,
                                         attributes: Attributes): Attributes {
  if (attributes[label]) {
    return attributes;
  }

  const attrValue = type == 'price'
    ? { currency: 'USD', values: value }
    : value;
   const attribute = { t: type, v: attrValue };

   return {
     ...attributes,
     [label]: attribute,
   };
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

type AttributeSet = {
  [label: string]: {
    type: string;
    value: any;
  }
}

export function setAttributes(attrs: AttributeSet,
                              form: FormAttributes,
                              shadow: ShadowAttributes): FormShadowAttrsPair {
  let newForm = form;
  let newShadow = shadow;

  _.each(attrs, ({type, value}, label) => {
    [newForm, newShadow] = setAttribute(label, type, value, newForm, newShadow);
  });

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

export function denormalize(obj: FormShadowObject, path: ?string = null) {
  const formTarget = path ? _.get(obj.form, path, {}) : obj.form;
  const shadowTarget = path ? _.get(obj.shadow, path, {}) : obj.shadow;

  if (!_.isArray(formTarget)) {
    copyShadowAttributes(formTarget.attributes, shadowTarget.attributes);
  } else {
    const forms = _.indexBy(formTarget, 'id');
    const shadows = _.indexBy(shadowTarget, 'id');

    _.each(forms, (form, id) => {
      const shadow = shadows[id];

      copyShadowAttributes(form.attributes, shadow.attributes);
    });
  }
}
