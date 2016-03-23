/**
 *
 * @flow
 */

import _ from 'lodash';
import { assoc, merge } from 'sprout-data';
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
          variants: {type: "variants", ref: "variants" },
          skus: {type: "skus", ref: "skus" },
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
    attributes: {},
    createdAt: null,
  };

  const emptySkuShadow: SkuShadow = {
    code: pseudoRandomCode,
    attributes: {},
    createdAt: null,
  };

  const variantKey = _.get('shadow.product.attributes.variants.ref');
  const skusKey = _.get('shadow.product.attributes.skus.ref');

  return assoc(product,
    ['form', 'product', 'attributes', variantKey], pseudoRandomCode,
    ['shadow', 'product', 'attributs', 'variants'], {type: "variants", ref:variantKey},
    ['form', 'product', 'attributes', skusKey] , pseudoRandomCode, {},
    ['shadow', 'product', 'attributes', 'skus'], {type: "skus", ref: skusKey},
    ['form', 'skus'], [...product.form.attributes.skus, emptySkuForm],
    ['shadow', 'skus'], [...product.shadow.attributes.skus, emptySkuShadow]
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

export function copyShadowAttributes(form, shadow) {
  //update form
  _.forEach(shadow,  (s, label) => {
      const attribute = form[s.ref];
      form[label] = attribute;
   }); 

  //update shadow
  //TODO: Finish
  shadow = _.transform(shadow, (result, value, key) => { value.ref = key; }, {});
}

/**
 * Takes the FullProduct response from the API and ensures that default attributes
 * have been included.
 * @param {FullProduct} product The full product response from Phoenix.
 * @return {FullProduct} Copy of the input that includes all default attributes.
 */
export function configureProduct(product: FullProduct): FullProduct {

  //copy product attributes
  copyShadowAttributes(
    product.form.product.attributes, 
    product.shadow.product.attributes);

  //copy sku attributes
  for(var i = 0; i < product.form.skus.length; i++) {
    copyShadowAttributes(
      product.form.skus[i].attributes, 
      product.shadow.skus[i].attributes);
  }

  const defaultAttrs = {
    title: 'string',
    description: 'string',
    retailPrice: 'price',
    salePrice: 'price',
    url: 'string',
    metaTitle: 'string',
    metaDescription: 'string',
  };

  const defaultSkuAttrs = {
    price: 'price',
    upc: 'string',
  };

  const newProduct: FullProduct = _.reduce(defaultAttrs, (res, val, key) => {
    return addProductAttribute(res, key, val);
  }, product);

  const newProdWithSku: FullProduct = _.reduce(defaultSkuAttrs, (res, val, key) => {
    return addSkuAttribute(res, key, val);
  }, newProduct);

  return newProdWithSku;
}

export function addProductAttribute(product: FullProduct,
                                    label: string,
                                    type: string): FullProduct {

  const formValue = type == 'price' ? { currency: 'USD', value: null } : null;
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
                                    value: any): FullProduct {

  if (label == 'skus') {
    return setSkuAttribute(product, value.code, value.label, value.value);
  }

  const shadowPath = ['shadow', 'product', 'attributes', label];
  const shadow = _.get(product, shadowPath);
  const path = ['form', 'product', 'attributes', shadow.ref];
  const attribute = _.get(product, path);

  if (!attribute) {
    throw new Error(`Attribute=${label} for product id=${product.id} not found.`);
  }

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

  const shadow = getSkuShadow(code, product);
  const form = getSkuForm(code, product);
  const shadowPath = ['attributes', label];
  const shadowAttr = _.get(shadow, shadowPath);
  const path = ['attributes', shadowAttr.ref];
  const attribute = _.get(form, path);

  if (!attribute) {
    throw new Error(`Attribute ${label} for SKU ${code} not found.`);
  }

  let updatedSku = null;

  switch (shadowAttr.type) {
    case 'price':
      updatedSku = assoc(form, [...path, 'value'], value);
      break;
    default:
      updatedSku = assoc(form, [...path], value);
      break;
  }

  return assoc(product, ['form', 'skus'], [...product.form.skus, updatedSku]);
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
