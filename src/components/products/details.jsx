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

export class ProductDetails extends Component<void, DetailsProps, void> {
  static propTypes = {
    details: PropTypes.shape({
      product: PropTypes.object,
    }),
  };

  get productAttributes(): ProductAttributes {
    if (this.props.details.product) {
      return getProductAttributes(1, this.props.details.product);
    } else {
      return {};
    }
  }

  get renderedAttributes(): Element {
    const attributes = _.map(this.productAttributes, attribute => {
      return this.renderAttribute(attribute);
    });

    return <div className="fc-product-details__attributes">{attributes}</div>;
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

  render() {
    return (
      <div className="fc-grid">
        <div className="fc-col-md-2-3">
          <ContentBox title="General" />
          {this.renderedAttributes}
        </div>
      </div>
    );
  }
}

type DetailsState = { details: ProductDetailsState };
function mapStateToProps(state: Object): DetailsState {
  return { pdetails: state.products.details };
}

export default connect(mapStateToProps, DetailsActions)(ProductDetails);
