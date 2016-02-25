/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { dispatch } from 'redux';
import _ from 'lodash';

// helpers
import { getProductAttributes } from '../../paragons/product';

// actions
import * as Actions from '../../modules/products/details';

// components
import ContentBox from '../content-box/content-box';

// types
import type { ProductAttribute, ProductAttributes } from '../../paragons/product';
import type { ProductDetailsState } from '../../modules/products/details';


type DetailsProps = {
  actions: DetailsActions,
  params: { productId: string, product: Object },
  products: ProductDetailsState,
};

type DetailsState = {
  products: ProductDetailsState,
};

type DetailsActions = {
  fetchProduct: (id: number) => void,
};

type DetailsDispatch = {
  actions: DetailsActions,
};

function mapStateToProps(state: Object, props: DetailsProps): DetailsState {
  return {
    products: state.products.details,
  };    
}

function mapDispatchToProps(dispatch: dispatch, props: DetailsProps): DetailsDispatch {
  return {
    actions: bindActionCreators(Actions, dispatch),
  };
}

class ProductDetails extends Component {
  props: DetailsProps;

  componentDidMount() {
    this.props.actions.fetchProduct(this.productId);
  }

  get productId(): number {
    return parseInt(this.props.params.productId);
  }

  get productAttributes(): ProductAttributes {
    if (this.props.products.product) {
      return getProductAttributes(1, this.props.products.product);
    } else {
      return {};
    }
  }

  renderAttributes(p: ProductAttributes): Element {
    const attributes = _.map(p, (attr, key) => {
      return (
        <div>
          <span>{key}</span>
          <span>{attr.value}</span>
        </div>
      );
    });

    return (
      <div>
        {attributes}
      </div>
    );
  }

  render() {
    return (
      <div>
        Details for product {this.props.params.productId}
        {this.renderAttributes(this.productAttributes)}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ProductDetails);
