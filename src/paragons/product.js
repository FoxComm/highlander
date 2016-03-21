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
        attributes: {},
        variants: {},
        createdAt: null,
      },
      skus: [],
    },
    shadow: {
      product: {
        id: null,
        productId: null,
        attributes: {},
        variants: null,
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

  return assoc(product,
    ['form', 'product', 'variants', 'default'], pseudoRandomCode,
    ['shadow', 'product', 'variants'], 'default',
    ['form', 'product', 'skus', 'default', pseudoRandomCode], {},
    ['shadow', 'product', 'skus'], 'default',
    ['form', 'skus'], [...product.form.skus, emptySkuForm],
    ['shadow', 'skus'], [...product.shadow.skus, emptySkuShadow]
  );
}

export function addNewVariant(product: FullProduct, variant: Variant): FullProduct {
  const variantName = variant.name;
  if (!variantName) throw new Error('Variant must have a name');

  const currentVariants: { [key:string]: Variant } = _.get(product, 'form.product.variants.default', {});
  const newVariants = {
    ...currentVariants,
    [variantName]: variant,
  };

  return assoc(product, ['form', 'product', 'variants', 'default'], newVariants);
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
  const newFormAttr = { [label]: { type: type, default: formValue } };
  const newShadowAttr = { [label]: 'default' };

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

  const form = getSkuForm(code, product);
  const path = ['attributes', label];
  const attribute = _.get(form, path);

  if (!attribute) {
    throw new Error(`Attribute ${label} for SKU ${code} not found.`);
  }

  let updatedSku = null;

  switch (attribute.type) {
    case 'price':
      updatedSku = assoc(form, [...path, 'default', 'value'], value);
      break;
    default:
      updatedSku = assoc(form, [...path, 'default'], value);
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
    const newFormAttr = { [label]: { type: type, default: formValue } };
    const newShadowAttr = { [label]: 'default' };

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
