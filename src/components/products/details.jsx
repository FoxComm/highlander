/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { dispatch } from 'redux';
import _ from 'lodash';

// helpers
import { getProductAttributes } from '../../paragons/product';

// actions
import * as DetailsActions from '../../modules/products/details';

// components
import ContentBox from '../content-box/content-box';

// types
import type { DetailsParams } from './types';
import type { ProductAttribute, ProductAttributes } from '../../paragons/product';
import type { ProductDetailsState } from '../../modules/products/details';

type DetailsProps = {
  details: ProductDetailsState,
  params: DetailsParams,
};

type ProductState = {
  illuminatedProduct: ProductAttributes,
  generalAttributes: ProductAttributes,
  pricingAttributes: ProductAttributes,
  seoAttributes: ProductAttributes,
};

const miscAttributeKeys = ['images'];
const pricingAttributeKeys = ['price', 'retailPrice', 'salePrice'];
const seoAttributeKeys = ['url', 'metaTitle', 'metaDescription'];

function getReactStateFromProps(props: DetailsProps): ProductState {
  const illuminatedProduct = props.details.product
    ? getProductAttributes(1, props.details.product)
    : {};

  const generalAttributes = _.omit(illuminatedProduct, [
    ...miscAttributeKeys,
    ...pricingAttributeKeys,
    ...seoAttributeKeys
  ]);
  const pricingAttributes = _.pick(illuminatedProduct, pricingAttributeKeys);
  const seoAttributes = _.pick(illuminatedProduct, seoAttributeKeys);

  return {
    illuminatedProduct,
    generalAttributes,
    pricingAttributes,
    seoAttributes,
  };
}

export class ProductDetails extends Component<void, DetailsProps, ProductState> {
  static propTypes = {
    details: PropTypes.shape({
      product: PropTypes.object,
    }),
  };

  state: ProductState;

  constructor(props: DetailsProps) {
    super(props);
    this.state = getReactStateFromProps(props);
  }

  componentWillReceiveProps(nextProps: DetailsProps) {
    this.setState(getReactStateFromProps(nextProps));
  }

  renderAttribute(attribute: ProductAttribute): Element {
    return (
      <div className="fc-product-details__attribute">
        <div className="fc-product-details__label">
          {attribute.label}
        </div>
        <div className="fc-product-details__value">
          {attribute.value}
        </div>
      </div>
    );
  }

  get generalContentBox(): Element {
    const attrs = _.map(this.state.generalAttributes, a => this.renderAttribute(a));
    return <ContentBox title="General" viewContent={attrs} />;
  }

  render() {
    return (
      <div className="fc-grid">
        <div className="fc-col-md-2-3">
          {this.generalContentBox}
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
