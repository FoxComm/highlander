/**
 * @flow
 */

import _ from 'lodash';
import { assoc } from 'sprout-data';
import { stringToCurrency } from '../lib/format-currency';

import type {
  FullProduct,
  Attribute,
  Attributes,
  ProductForm,
  ProductShadow,
  ShadowAttributes,
  SkuForm,
  SkuShadow,
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
};

function getProductForm(product: FullProduct): ProductForm {
  const form: ?ProductForm = _.get(product, 'form.product');
  if (!form) throw new Error('Product form not found in FullProduct response.');
  return form;
}

function getProductShadow(product: FullProduct): ProductShadow {
  const shadow: ?ProductShadow = _.get(product, 'shadow.product');
  if (!shadow) throw new Error('Product shadow not found in FullProduct response.');
  return shadow;
}

function getSkuForm(code: string, product: FullProduct): SkuForm {
  const forms: Array<SkuForm> = _.get(product, 'form.skus', []);
  const form: ?SkuForm = _.find(forms, { code: code });

  if (!form) {
    throw new Error(`SKU form for code ${code} not found in FullProduct response.`);
  }

  return form;
}

function getSkuShadow(code: string, product: FullProduct): SkuShadow {
  const shadows: Array<SkuShadow> = _.get(product, 'shadow.skus', []);
  const shadow: ?SkuShadow = _.find(shadows, { code: code });

  if (!shadow) {
    throw new Error(`SKU shadow for code ${code} not found in FullProduct response.`);
  }

  return shadow;
}

/**
 * Extracts all of the attributes from a FullProduct by combining the ProductForm
 * and ProductShadow. This is essentially the client-side view of the illuminated
 * product.
 * @param {FullProduct} product The full product response from Phoenix.
 * @return {IlluminatedAttributes} Illuminated list of attributes.
 */
export function getProductAttributes(product: FullProduct): IlluminatedAttributes {
  const form = getProductForm(product);
  const shadow = getProductShadow(product);
  return getAttributes(form.attributes, shadow.attributes);
}

function getAttributes(formAttrs: Attributes, shadowAttrs: ShadowAttributes): IlluminatedAttributes {
  const illuminated: IlluminatedAttributes = _.reduce(shadowAttrs, (res, formKey, label) => {
    const attribute = formAttrs[label];

    res[label] = {
      label: label,
      type: attribute.type,
      value: attribute[formKey],
    };

    return res;
  }, {});

  return illuminated;
}

export function getIlluminatedSkus(product: FullProduct): Array<IlluminatedSku> {
  return _.map(product.form.skus, (form: SkuForm) => {
    const shadow = getSkuShadow(form.code, product);

    return {
      code: form.code,
      attributes: getAttributes(form.attributes, shadow.attributes),
    };
  });
}

/**
 * Takes the FullProduct response from the API and ensures that default attributes
 * have been included.
 * @param {FullProduct} product The full product response from Phoenix.
 * @return {FullProduct} Copy of the input that includes all default attributes.
 */
export function configureProduct(product: FullProduct): FullProduct {
  const defaultAttrs = {
    title: 'string',
    description: 'string',
    retailPrice: 'price',
    salePrice: 'price',
    url: 'string',
    metaTitle: 'string',
    metaDescription: 'string',
  };

  const newProduct: FullProduct = _.reduce(defaultAttrs, (res, val, key) => {
    return addProductAttribute(res, key, val);
  }, product);

  return newProduct;
}

export function addProductAttribute(product: FullProduct,
                                    label: string,
                                    type: string,
                                    context: string = 'default'): FullProduct {

  const formValue = type == 'price' ? { currency: 'USD', value: null } : null;
  const newFormAttr = { [label]: { type: type, default: formValue } };
  const newShadowAttr = { [label]: 'default' };

  const formAttrs = _.get(product, 'form.product.attributes', {});
  const shadowAttrs = _.get(product, 'shadow.product.attributes', {});

  return assoc(product,
    ['form', 'product', 'attributes'], { ...newFormAttr, ...formAttrs },
    ['shadow', 'product', 'attributes'], { ...newShadowAttr, ...shadowAttrs }
  );
}

//export function addSkuAttribute(product: FullProduct,
                                //sku: string,
                                //label: string,
                                //context: string = 'default'): FullProduct {


//}




export function setProductAttribute(product: FullProduct, label: string, value: string): FullProduct {
  const path = ['form', 'product', 'attributes', label];
  const attribute = _.get(product, path);

  if (!attribute) {
    throw new Error(`Attribute=${label} for product id=${product.id} not found.`);
  }

  switch (attribute.type) {
    case 'price':
      const price: number = stringToCurrency(value);
      return assoc(product, [...path, 'default', 'value'], price);
    default:
      return assoc(product, [...path, 'default'], value);
  }
}
