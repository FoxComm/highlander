/**
 * @flow
 */

import _ from 'lodash';
import { assoc, merge } from 'sprout-data';
import { copyShadowAttributes } from './form-shadow-object';
import { generateSkuCode } from './sku';

import type { SkuForm, SkuShadow } from './sku';

import type {
  Product,
  Attribute,
  Attributes,
  ShadowAttributes,
  Variant,
} from '../modules/products/details';

export type IlluminatedAttribute = {
  label: string,
  type: string,
  value: string,
};

export type IlluminatedAttributes = { [key:string]: Attribute };

export type IlluminatedSku = {
  code: string,
  attributes: IlluminatedAttributes,
  createdAt: ?string,
};

export function getAttribute(formAttrs: Attributes, shadowAttrs: ShadowAttributes,
  label: string): IlluminatedAttribute {

  const shadow = shadowAttrs[label];
  if(!shadow) return shadow;

  const attribute = formAttrs[shadow.ref];

  const res = {
    label: label,
    type: shadow.type,
    value: attribute,
  };

  return res;
}

//export function getAttributes(formAttrs: Attributes, shadowAttrs: ShadowAttributes): IlluminatedAttributes {
  //const illuminated: IlluminatedAttributes = _.reduce(shadowAttrs, (res, shadow, label) => {
    //const attribute = formAttrs[shadow.ref];

    //res[label] = {
      //label: label,
      //type: shadow.type,
      //value: attribute,
    //};

    //return res;
  //}, {});

  //return illuminated;
//}

//export function getIlluminatedSkus(product: FullProduct): Array<IlluminatedSku> {
  //return _.map(product.form.skus, (form: SkuForm) => {
    //if (form.code) {
      //const shadow = getSkuShadow(form.code, product);

      //return {
        //code: form.code,
        //attributes: getAttributes(form.attributes, shadow.attributes),
        //createdAt: form.createdAt,
      //};
    //}
  //});
//}

export function createEmptyProduct(): Product {
  const product = {
    id: null,
    attributes: {},
    skus: [],
  };

  return configureProduct(addEmptySku(product));
}

export function addEmptySku(product: Product): Product {
  const pseudoRandomCode = generateSkuCode();

  const emptyPrice = {
    t: 'price',
    v: { currency: 'USD', value: 0 },
  };

  const emptySku = {
    code: pseudoRandomCode,
    attributes: {
      title: {
        t: 'string',
        v: '',
      },
      retailPrice: emptyPrice,
      salePrice: emptyPrice,
    },
  };

  const newSkus = [emptySku, ...product.skus];
  return assoc(product, 'skus', newSkus);
}

/**
 * Takes the FullProduct response from the API and ensures that default attributes
 * have been included.
 * @param {FullProduct} product The full product response from Phoenix.
 * @return {FullProduct} Copy of the input that includes all default attributes.
 */
export function configureProduct(product: Product): Product {
  const defaultAttrs = {
    title: 'string',
    description: 'richText',
    url: 'string',
    metaTitle: 'string',
    metaDescription: 'string',
  };

  const defaultSkuAttrs = {
    retailPrice: 'price',
    salePrice: 'price',
    upc: 'string',
  };

  const newProduct: Product = _.reduce(defaultAttrs, (res, val, key) => {
    if (_.get(res, ['attributes', key])) {
      return res;
    }

    const attr = val == 'price'
      ? { t: val, v: { currency: 'USD', value: null } }
      : { t: val, v: null };

    return assoc(res, 'attributes', {
      [key]: attr,
      ...res.attributes,
    });
  }, product);

  return newProduct;
}

export function setSkuAttribute(product: Product,
                                code: string,
                                label: string,
                                value: string): Product {
  // TODO: Re-enable this after refactoring
  //const updateCode = sku => {
    //if (sku.code == code) {
      //return { ...sku, code: value };
    //}

    //return sku;
  //};

  //if (label == 'code') {
    //const newForms = product.form.skus.map(sku => updateCode(sku));
    //const newShadows = product.shadow.skus.map(sku => updateCode(sku));

    //return assoc(product,
      //['form', 'skus'], newForms,
      //['shadow', 'skus'], newShadows,
    //);
  //}

  //const shadow = getSkuShadow(code, product);
  //const formIndex = getSkuFormIndex(code, product);
  //const form = product.form.skus[formIndex];
  //const shadowPath = ['attributes', label];

  //const shadowAttr = _.get(shadow, shadowPath);
  //if (!shadowAttr) {
    //throw new Error(`Attribute ${label} for SKU ${code} not found.`);
  //}

  //const path = ['attributes', shadowAttr.ref];


  //let updatedSku = null;

  //switch (shadowAttr.type) {
    //case 'price':
      //updatedSku = assoc(form, [...path, 'value'], value);
      //break;
    //default:
      //updatedSku = assoc(form, [...path], value);
      //break;
  //}

  //product.form.skus[formIndex] = updatedSku;
  return product;
}
