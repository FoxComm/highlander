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

// types
import type { DetailsParams } from './types';
import { FormField } from '../forms';
import type { Product, ProductDetailsState } from '../../modules/products/details';

type DetailsProps = {
  details: ProductDetailsState,
  params: DetailsParams,
};

type PriceAttribute = {
  currency: string,
  price: number,
};

type ProductAttributes = { [key:string]: any };

const miscAttributeKeys = ['images'];
const pricingAttributeKeys = ['retailPrice', 'salePrice'];
const seoAttributeKeys = ['URL', 'metaTitle', 'metaDescription'];

export class ProductDetails extends Component<void, DetailsProps, void> {
  static propTypes = {
    details: PropTypes.shape({
      product: PropTypes.object,
    }),
  };

  get generalContentBox(): Element {
    const attributes: ProductAttributes = _.get(this.props, 'details.product.attributes', {});

    const notGeneralKeys = [
      ...miscAttributeKeys,
      ...pricingAttributeKeys,
      ...seoAttributeKeys
    ];

    const generalAttrs: Array<Element> = _(attributes).omit(notGeneralKeys).map((attr, key) => {
      return this.renderField(key, attr);
    }).value();

    return <ContentBox title="General">{generalAttrs}</ContentBox>;
  }

  get pricingContentBox(): Element {
    const attributes: Array<Element> = _.map(pricingAttributeKeys, key => {
      const val = _.get(this.props, ['details.product.attributes', key, 'value']);
      return this.renderField(key, val);
    });

    return <ContentBox title="Pricing">{attributes}</ContentBox>;
  }

  get seoContentBox(): Element {
    const attributes: Array<Element> = _.map(seoAttributeKeys, key => {
      const val = _.get(this.props, ['details.product.attributes', key]);
      return this.renderField(key, val);
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

  renderField(label: string, value: string) {
    const formattedLbl = _.snakeCase(label).split('_').reduce((res, val) => {
      return `${res} ${_.capitalize(val)}`;
    });

    return (
      <FormField
        className="fc-product-details__field"
        label={formattedLbl}
        labelClassName="fc-product-details__field-label">
        <input
          className="fc-product-details__field-value"
          type="text"
          defaultValue={value} />
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
