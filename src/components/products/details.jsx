/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { dispatch } from 'redux';
import _ from 'lodash';

// actions
import * as DetailsActions from '../../modules/products/details';

// components
import ContentBox from '../content-box/content-box';
import InlineField from '../inline-field/inline-field';
import SkuList from './sku-list';

// helpers
import { getProductAttributes } from '../../paragons/product';

// types
import { FormField } from '../forms';
import type { FullProduct, ProductAttribute, ProductDetailsState } from '../../modules/products/details';

type DetailsParams = {
  productId: number,
  product: FullProduct,
};

type DetailsProps = {
  details: ProductDetailsState,
  params: DetailsParams,
};

type PriceAttribute = {
  currency: string,
  price: number,
};

type ProductAttributes = { [key:string]: ProductAttribute };

const reqGeneralKeys = ['title', 'description'];
const miscAttributeKeys = ['images'];
const pricingAttributeKeys = ['retailPrice', 'salePrice'];
const seoAttributeKeys = ['url', 'metaTitle', 'metaDescription'];

export class ProductDetails extends Component<void, DetailsProps, void> {
  static propTypes = {
    details: PropTypes.shape({
      product: PropTypes.object,
    }),
  };

  get fullProduct(): ?FullProduct {
    return _.get(this.props, 'details.product');
  }

  get attributes(): ProductAttributes {
    const fullProduct = this.fullProduct;
    return fullProduct ? getProductAttributes(fullProduct) : {};
  }

  get generalContentBox(): Element {
    const customKeys = [
      ...reqGeneralKeys,
      ...miscAttributeKeys,
      ...pricingAttributeKeys,
      ...seoAttributeKeys
    ];

    const { title, description } = this.attributes;
    const customAttrs = _.omit(this.attributes, customKeys);
    const generalAttrs = {
      title: title,
      description: description,
      ...customAttrs,
    };

    const attributes = this.renderAttributes(generalAttrs);
    return <ContentBox title="General">{attributes}</ContentBox>;
  }

  get pricingContentBox(): Element {
    const attributes = pricingAttributeKeys.map(key => {
      return this.renderAttribute(this.attributes[key]);
    });

    return <ContentBox title="Pricing">{attributes}</ContentBox>;
  }

  get seoContentBox(): Element {
    const attributes = seoAttributeKeys.map(key => {
      return this.renderAttribute(this.attributes[key]);
    });
    return <ContentBox title="SEO">{attributes}</ContentBox>;
  }

  get skusContentBox(): Element {
    return (
      <ContentBox title="SKUs" indentContent={false}>
        <div className="fc-content-box__empty-text">
          This product does not have SKUs.
        </div>
      </ContentBox>
    );
  }

  get variantContentBox(): Element {
    return (
      <ContentBox title="Variants" indentContent={false}>
        <div className="fc-content-box__empty-text">
          This product does not have variants.
        </div>
      </ContentBox>
    );
  }

  renderAttributes(attributes: ProductAttributes): Array<Element> {
    return _.map(attributes, attr => this.renderAttribute(attr));
  }

  renderAttribute(attribute: ProductAttribute): Element {
    const { label, type, value } = attribute;
    const formattedLbl = _.snakeCase(label).split('_').reduce((res, val) => {
      return `${res} ${_.capitalize(val)}`;
    });

    const defaultValue = type == 'price' ? value.value : value;

    return (
      <FormField
        className="fc-product-details__field"
        label={formattedLbl}
        labelClassName="fc-product-details__field-label"
        key={`product-page-field-${label}`}>
        <input
          className="fc-product-details__field-value"
          type="text"
          name={label}
          defaultValue={defaultValue} />
      </FormField>
    );
  }

  render() {
    return (
      <div className="fc-product-details fc-grid">
        <div className="fc-col-md-2-3">
          {this.generalContentBox}
          {this.pricingContentBox}
          {this.variantContentBox}
          {this.skusContentBox}
          {this.seoContentBox}
        </div>
      </div>
    );
  }
}

type DetailsState = { details: ProductDetailsState };
function mapStateToProps(state: Object): DetailsState {
  return { details: state.products.details };
}

export default connect(mapStateToProps, DetailsActions)(ProductDetails);
