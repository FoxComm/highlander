/* @flow */

// libs
import _ from 'lodash';
import { assoc, dissoc } from 'sprout-data';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import Currency from 'ui/currency';
import Facets from 'components/facets/facets';

// styles
import styles from './pdp.css';

// types
import type { TProductView } from './types';
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
  [variantType:string]: number,
}

type State = {
  selectedVariantValues: VariantValuesMap,
}

function getSkuCodesForVariantValue(product, valueId: number, variantType: string) {
  const variant: ProductVariant = _.find(product.variants, variant => variant.attributes.type.v == variantType);
  const variantValue: VariantValue = _.find(variant.values, {id: valueId});

  return variantValue.skuCodes;
}

class ProductDetails extends Component {
  props: Props;
  state: State = {
    selectedVariantValues: {},
  };

  getSkuByCode(product, code) {
    return _.find(product.skus, sku => sku.attributes.code.v == code);
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
      const [skuCode, exactMatch] = this.findClosestSku();
      let sku;
      if (skuCode) {
        sku = _.find(product.skus, sku => sku.attributes.code.v == skuCode);
      }
      let unselectedFacets = [];
      if (!exactMatch) {
        unselectedFacets = this.getUnselectedFacets();
      }

      this.props.onSkuChange(sku, exactMatch, unselectedFacets);
    });
  }

  getFacets(product: ?ProductResponse): Array<TFacet> {
    if (!product) return [];

    const { variants } = product;

    return _.flatMap(variants, (variant: ProductVariant) => {
      const variantType = variant.attributes.type.v;
      let kind = variantType;
      if (kind === 'color') kind = 'image';
      else if (kind === 'size') kind = 'circle';
      const values = _.flatMap(variant.values, (value: VariantValue) => {
        const facetValue = {
          valueId: value.id,
          variantType,
        };

        const baseProps = {
          selected: this.state.selectedVariantValues[variantType] == value.id,
        };

        if (variantType == 'color') {
          const skuCode = value.skuCodes[0];

          const sku = this.getSkuByCode(product, skuCode);
          return {
            ...baseProps,
            label: value.name,
            value: {
              value: facetValue,
              image: _.get(sku, 'albums.0.images.0.src', '')
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
        values,
      };
    });
  }

  getUnselectedFacets(): ?string {
    const facets = this.getFacets(this.props.product);
    return _.filter(facets, (facet: TFacet) => {
      return _.every(facet.values, value => !value.selected);
    });
  }

  findClosestSku(facets = this.getFacets(this.props.product)): [?string, boolean] {
    const { product } = this.props;

    const skuCodes = _.reduce(this.state.selectedVariantValues, (acc, variantValueId: number, variantType: string) => {
      const skuCodes = getSkuCodesForVariantValue(product, variantValueId, variantType);
      return acc.length ? _.intersection(acc, skuCodes) : skuCodes;
    }, []);

    // probably we could detect exact match by cheking skuCodes.length == 1
    // but is there guarantee that only one sku/variant match complete set of variants ?
    return [skuCodes[0], facets.length === _.size(this.state.selectedVariantValues)];
  }

  get facets(): Element<*> {
    const facets = this.getFacets(this.props.product);
    return <Facets facets={facets} onSelect={this.handleSelectFacet} />;
  }

  get productPrice(): Element<*> {
    const {
      currency,
      price,
      skus,
    } = this.props.productView;

    const salePrice = _.get(skus[0], 'attributes.salePrice.v.value', 0);
    const retailPrice = _.get(skus[0], 'attributes.retailPrice.v.value', 0);

    if (retailPrice > salePrice) {
      return (
        <div styleName="price">
          <Currency
            styleName="retail-price"
            value={retailPrice}
            currency={currency}
          />
          <Currency
            styleName="on-sale-price"
            value={salePrice}
            currency={currency}
          />
        </div>
      );
    }

    return (
      <div styleName="price">
        <Currency value={price} currency={currency} />
      </div>
    );
  }

  render(): Element<*> {
    return (
      <div>
        {this.productPrice}
        {this.facets}
      </div>
    );
  }
};

export default ProductDetails;
