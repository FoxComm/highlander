/**
 * @flow
 */

import _ from 'lodash';
import { assoc, merge } from 'sprout-data';
import { stringToCurrency } from '../lib/format-currency';
import { copyShadowAttributes } from './form-shadow-object';

import type {
  FullProduct,
  Attribute,
  Attributes,
  ProductForm,
  ProductShadow,
  ShadowAttributes,
  SkuForm,
  SkuShadow,
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

function getSkuFormIndex(code: string, product: FullProduct): number {
  const forms: Array<SkuForm> = _.get(product, 'form.skus', []);
  const index = _.findIndex(forms, { code: code });

  if (index == -1) {
    throw new Error(`SKU form for code ${code} not found in FullProduct response.`);
  }

  return index;
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

export function getAttributes(formAttrs: Attributes, shadowAttrs: ShadowAttributes): IlluminatedAttributes {
  const illuminated: IlluminatedAttributes = _.reduce(shadowAttrs, (res, shadow, label) => {
    const attribute = formAttrs[shadow.ref];

    res[label] = {
      label: label,
      type: shadow.type,
      value: attribute,
    };

    return res;
  }, {});

  return illuminated;
}

export function getIlluminatedSkus(product: FullProduct): Array<IlluminatedSku> {
  return _.map(product.form.skus, (form: SkuForm) => {
    if (form.code) {
      const shadow = getSkuShadow(form.code, product);

      return {
        code: form.code,
        attributes: getAttributes(form.attributes, shadow.attributes),
        createdAt: form.createdAt,
      };
    }
  });
}

export function createEmptyProduct(): FullProduct {
  const product = {
    id: null,
    form: {
      product: {
        id: null,
        attributes: {
          variants: {},
          skus: {}
        },
        createdAt: null,
      },
      skus: [],
    },
    shadow: {
      product: {
        id: null,
        productId: null,
        attributes: {
          variants: {type: 'variants', ref: 'variants' },
          skus: {type: 'skus', ref: 'skus' },
        },
        createdAt: null,
      },
      skus: [],
    },
  };

  return configureProduct(addEmptySku(product));
}

export function addEmptySku(product: FullProduct): FullProduct {
  const pseudoRandomCode: string = Math.random().toString(36).substring(7);

  const emptySkuForm: SkuForm = {
    code: pseudoRandomCode,
    attributes: {
      title: '',
      retailPrice: {
        value: 0,
        currency: 'USD'
      },
      salePrice: {
        value: 0,
        currency: 'USD'
      }
    },
    createdAt: null,
  };

  const emptySkuShadow: SkuShadow = {
    code: pseudoRandomCode,
    attributes: {
      title: {type: 'string', ref: 'title'},
      retailPrice: {type: 'price', ref: 'retailPrice'},
      salePrice: {type: 'price', ref: 'salePrice'}
    },
    createdAt: null,
  };

  const variantKey = _.get(product, 'shadow.product.attributes.variants.ref');
  const skusKey = _.get(product, 'shadow.product.attributes.skus.ref');

  return assoc(product,
    ['form', 'product', 'attributes', variantKey], pseudoRandomCode,
    ['shadow', 'product', 'attributes', 'variants'], {type: 'variants', ref:variantKey},
    ['form', 'product', 'attributes', skusKey] , pseudoRandomCode, {},
    ['shadow', 'product', 'attributes', 'skus'], {type: 'skus', ref: skusKey},
    ['form', 'skus'], product.form.skus.push(emptySkuForm),
    ['shadow', 'skus'], product.shadow.skus.push(emptySkuShadow)
  );
}

export function addNewVariant(product: FullProduct, variant: Variant): FullProduct {
  const variantName = variant.name;
  if (!variantName) throw new Error('Variant must have a name');

  const variantKey = _.get('shadow.product.attributes.variants.ref');
  const currentVariants: { [key:string]: Variant } = _.get(product, 'form.product.attributes.'+variantKey, {});
  const newVariants = {
    ...currentVariants,
    [variantName]: variant,
  };

  return assoc(product, ['form', 'product', 'attributes', variantKey], newVariants);
}

/**
 * Takes the FullProduct response from the API and ensures that default attributes
 * have been included.
 * @param {FullProduct} product The full product response from Phoenix.
 * @return {FullProduct} Copy of the input that includes all default attributes.
 */
export function configureProduct(product: FullProduct): FullProduct {
  // copy product attributes
  copyShadowAttributes(
    product.form.product.attributes,
    product.shadow.product.attributes);

  // copy sku attributes
  for(var i = 0; i < product.form.skus.length; i++) {
    copyShadowAttributes(
      product.form.skus[i].attributes,
      product.shadow.skus[i].attributes);
  }

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

  const newProduct: FullProduct = _.reduce(defaultAttrs, (res, val, key) => {
    const formAttribute = _.get(res, ['form', 'product', 'attributes', key]);
    if (formAttribute) {
      return res;
    }

    return setProductAttribute(res, key, val, '');
  }, product);

  const newProdWithSku: FullProduct = _.reduce(defaultSkuAttrs, (res, val, key) => {
    return addSkuAttribute(res, key, val);
  }, newProduct);

  return newProdWithSku;
}

export function addProductAttribute(product: FullProduct,
                                    label: string,
                                    type: string,
                                    value: any): FullProduct {

  const formValue = type == 'price' ? { currency: 'USD', value: value } : value;
  const newFormAttr = { [label]: formValue };
  const newShadowAttr = { [label]: {type: type, ref: label }};

  const formAttrs = _.get(product, 'form.product.attributes', {});
  const shadowAttrs = _.get(product, 'shadow.product.attributes', {});

  return assoc(product,
    ['form', 'product', 'attributes'], { ...newFormAttr, ...formAttrs },
    ['shadow', 'product', 'attributes'], { ...newShadowAttr, ...shadowAttrs }
  );
}

export function setProductAttribute(product: FullProduct,
                                    label: string,
                                    type: string,
                                    value: any): FullProduct {

  if (label == 'skus') {
    return setSkuAttribute(product, value.code, value.label, value.value);
  }

  const shadowPath = ['shadow', 'product', 'attributes', label];

  const shadow = _.get(product, shadowPath);

  if (!shadow) {
    return addProductAttribute(product, label, type, value);
  }

  const path = ['form', 'product', 'attributes', shadow.ref];

  switch (shadow.type) {
    case 'price':
      return assoc(product, [...path, 'value'], value);
    default:
      return assoc(product, [...path], value);
  }
}

export function setSkuAttribute(product: FullProduct,
                                code: string,
                                label: string,
                                value: string): FullProduct {
  const updateCode = sku => {
    if (sku.code == code) {
      return { ...sku, code: value };
    }

    return sku;
  };

  if (label == 'code') {
    const newForms = product.form.skus.map(sku => updateCode(sku));
    const newShadows = product.shadow.skus.map(sku => updateCode(sku));

    return assoc(product,
      ['form', 'skus'], newForms,
      ['shadow', 'skus'], newShadows,
    );
  }

  const shadow = getSkuShadow(code, product);
  const formIndex = getSkuFormIndex(code, product);
  const form = product.form.skus[formIndex];
  const shadowPath = ['attributes', label];

  const shadowAttr = _.get(shadow, shadowPath);
  if (!shadowAttr) {
    throw new Error(`Attribute ${label} for SKU ${code} not found.`);
  }

  const path = ['attributes', shadowAttr.ref];


  let updatedSku = null;

  switch (shadowAttr.type) {
    case 'price':
      updatedSku = assoc(form, [...path, 'value'], value);
      break;
    default:
      updatedSku = assoc(form, [...path], value);
      break;
  }

  product.form.skus[formIndex] = updatedSku;
  return product;
}

export function addSkuAttribute(product: FullProduct,
                                label: string,
                                type: string,
                                code: ?string = null): FullProduct {

  // If no code is specified, add to all.
  const skuForms = code ? [_.find(product.form.skus, { code: code })] : product.form.skus;
  const newProduct = _.reduce(skuForms, (res, skuForm) => {
    const skuShadow = getSkuShadow(skuForm.code, res);

    const formValue = type == 'price' ? { currency: 'USD', value: null } : null;
    const newFormAttr = { [label]: formValue };
    const newShadowAttr = { [label]: {type: type, ref: label }};

    const formAttrs = _.get(skuForm, 'attributes', {});
    const shadowAttrs = _.get(skuShadow, 'attributes', {});

    const newSkuForm = assoc(skuForm, 'attributes', { ...newFormAttr, ...formAttrs });
    const newSkuShadow = assoc(skuShadow, 'attributes', { ...newShadowAttr, ...shadowAttrs });

    return assoc(res,
      ['form', 'skus'], merge(res.form.skus, [newSkuForm]),
      ['shadow', 'skus'], merge(res.shadow.skus, [newSkuShadow])
    );
  }, product);

  return newProduct;
}
