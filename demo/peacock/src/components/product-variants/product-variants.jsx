/* @flow */

// libs
import _ from 'lodash';
import { assoc, dissoc } from 'sprout-data';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import Facets from 'components/facets/facets';

// styles
import styles from './product-variants.css';

// types
import type { TProductView } from 'pages/catalog/types';
import type { ProductResponse, ProductVariant, VariantValue, Sku } from 'modules/product-details';
import type { Facet as TFacet } from 'types/facets';

type Props = {
  productView: TProductView,
  product: ProductResponse,
  selectedSku: Sku,
  onSkuChange: (sku: ?Sku, exactMatch: boolean, unselectedFacets: Array<TFacet>) => void,
}

// variantType => VariantValue.id
type VariantValuesMap = {
  [variantType: string]: number,
}

type State = {
  selectedVariantValues: VariantValuesMap,
}

function getSkuCodesForVariantValue(product: ProductResponse, valueId: number, variantType: string): Array<string> {
  const variant: ProductVariant = _.find(product.variants, (v: ProductVariant) => v.attributes.type.v == variantType);
  const variantValue: VariantValue = _.find(variant.values, {id: valueId});

  return variantValue.skuCodes;
}

/* eslint-disable quote-props */

function sortSizes(sizes: { label: string }): string {
  const labelMap = {
    XS: '01_XS',
    S: '02_S',
    M: '03_M',
    L: '04_L',
    XL: '05_XL',
    '2XL': '06_2XL',
    '6.5': '06.5',
    '7': '07.0',
    '7.5': '07.5',
    '8': '08.0',
    '8.5': '08.5',
    '9': '09.0',
    '9.5': '09.5',
  };

  return _.sortBy(sizes, (size) => {
    return _.get(labelMap, size.label, size.label);
  });
}


class ProductVariants extends Component {
  props: Props;
  state: State = {
    selectedVariantValues: {},
  };
  _facets: Facets;

  getSkuByCode(product: ProductResponse, code: string): Sku {
    return _.find(product.skus, sku => sku.attributes.code.v == code);
  }

  flashUnselectedFacets(facets: Array<TFacet>) {
    this._facets.flashUnselectedFacets(facets);
  }

  @autobind
  handleSelectFacet(facet: string, value: Object, selected: boolean) {
    let { selectedVariantValues } = this.state;

    const { product } = this.props;

    if (selected) {
      // check if we should deselect conflicted variant
      const allowedSkuCodes = _.keyBy(getSkuCodesForVariantValue(product, value.valueId, value.variantType));
      const conflictVariants = _.flatMap(selectedVariantValues, (valueId: number, variantType: string) => {
        if (variantType == value.variantType) return [];
        const skuCodes = getSkuCodesForVariantValue(product, valueId, variantType);

        const someIntersection = _.some(skuCodes, skuCode => skuCode in allowedSkuCodes);
        return someIntersection ? [] : variantType;
      });
      selectedVariantValues = assoc(selectedVariantValues, value.variantType, value.valueId);
      if (conflictVariants.length) {
        selectedVariantValues = dissoc(selectedVariantValues, ...conflictVariants);
      }
    } else {
      selectedVariantValues = dissoc(selectedVariantValues, value.variantType);
    }

    this.setState({
      selectedVariantValues,
    }, () => {
      this.fireSkuChange();
    });
  }

  fireSkuChange(props: Props = this.props) {
    const { product } = props;
    if (!product) return;

    const [skuCode, exactMatch] = this.findClosestSku(props);
    const matchedSku = skuCode ? _.find(product.skus, sku => sku.attributes.code.v == skuCode) : null;
    const unselectedFacets = exactMatch ? [] : this.getUnselectedFacets(product);

    props.onSkuChange(matchedSku, exactMatch, unselectedFacets);
  }

  getFacets(product: ProductResponse): Array<TFacet> {
    if (product == null) return [];

    const { variants } = product;
    const { selectedVariantValues } = this.state;
    const allowedSkuCodes = _.flatMap(selectedVariantValues, (id, type) => {
      return getSkuCodesForVariantValue(product, id, type);
    });

    const facets = _.flatMap(variants, (variant: ProductVariant) => {
      const variantType = variant.attributes.type.v;
      let kind = variantType;
      if (kind === 'color') kind = 'image';
      else if (kind === 'size') kind = 'circle';

      const valuesLength = variant.values.length;

      const values = _.flatMap(variant.values, (value: VariantValue) => {
        const facetValue = {
          valueId: value.id,
          variantType,
        };

        const baseProps = {
          selected: valuesLength === 1 ? true : selectedVariantValues[variantType] == value.id,
          available: _.isEmpty(selectedVariantValues) || _.intersection(allowedSkuCodes, value.skuCodes).length,
        };

        if (variantType == 'color') {
          const skuCode = value.skuCodes[0];

          const sku = this.getSkuByCode(product, skuCode);
          return {
            ...baseProps,
            label: value.name,
            value: {
              value: facetValue,
              image: _.get(sku, 'albums.0.images.0.src', ''),
            },
          };
        } else if (variantType == 'size') {
          return {
            ...baseProps,
            value: facetValue,
            label: value.name,
          };
        }
        return [];
      });
      if (!values.length) return [];

      return {
        name: _.capitalize(variantType),
        key: variantType,
        kind,
        values: sortSizes(values),
      };
    });

    return _.sortBy(facets, facet => facet.name);
  }

  getUnselectedFacets(product: ProductResponse = this.props.product): Array<string> {
    const facets = this.getFacets(product);
    return _.filter(facets, (facet: TFacet) => {
      return _.every(facet.values, value => !value.selected);
    });
  }

  findClosestSku(props: Props = this.props): [?string, boolean] {
    const { product } = props;
    const facets = this.getFacets(product);

    const matchedSkuCodes =
      _.reduce(this.state.selectedVariantValues, (acc, variantValueId: number, variantType: string) => {
        const skuCodes = getSkuCodesForVariantValue(product, variantValueId, variantType);
        return acc.length ? _.intersection(acc, skuCodes) : skuCodes;
      }, []);

    // probably we could detect exact match by cheking matchedSkuCodes.length == 1
    // but is there guarantee that only one sku/variant match complete set of variants ?
    return [matchedSkuCodes[0], facets.length === _.size(this.state.selectedVariantValues)];
  }

  get facets(): Element<*> {
    const facets = this.getFacets(this.props.product);
    return (
      <Facets
        ref={(_ref) => { this._facets = _ref; }}
        styleName="facets"
        facets={facets}
        onSelect={this.handleSelectFacet}
        required
      />
    );
  }

  render(): Element<*> {
    return this.facets;
  }
}

export default ProductVariants;
