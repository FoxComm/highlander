// @flow

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import styles from './products.css';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import { connect } from 'react-redux';

// components
import ProductRow from './product-row';
import ProductVariantRow from './product-variant-row';
import WaitAnimation from 'components/common/wait-animation';

// data
import productVariantsReducer, { fetchProductVariants } from 'modules/product-variants/list';

// types
import type { Product } from 'paragons/product';
import type { ProductVariant } from 'modules/product-variants/list';

type Props = {
  product: Product,
  columns?: Array<Object>,
  params: Object,
  // connected
  fetchState: AsyncState,
  productVariants: Array<ProductVariant>,
  fetchProductVariants: (productId: number) => Promise,
}

type State = {
  expanded: boolean,
  allowAnimation: boolean,
}

function mapLocalState(state) {
  return {
    fetchState: _.get(state.asyncActions, 'fetchProductVariants', {}),
    productVariants: state.list,
  };
}

class ProductWithVariantsRow extends Component {
  props: Props;
  _waitTimer: number|void;

  state: State = {
    expanded: false,
    allowAnimation: false,
  };

  @autobind
  toggleExpanded(event) {
    event.stopPropagation();
    event.preventDefault();

    const newState = !this.state.expanded;

    this.setState({
      expanded: newState,
    });

    if (newState) {
      this.props.fetchProductVariants(this.props.product.id);
      this._waitTimer = setTimeout(this.enableWaitAnimation, 450);
    } else {
      this.setState({
        allowAnimation: false,
      });
    }
  }

  @autobind
  enableWaitAnimation() {
    this.setState({
      allowAnimation: true,
    });
  }

  componentWillUnmount() {
    clearTimeout(this._waitTimer);
  }

  get toggleIcon(): Element {
    const direction = this.state.expanded ? 'up' : 'down';
    const title = this.state.expanded ? 'Hide Product Variants' : 'Show Product Variants';
    return (
      <i
        title={title}
        className={`icon-chevron-${direction}`}
        styleName="toggle-icon"
        onClick={this.toggleExpanded}
      />
    );
  }

  get productVariants(): Array<Element>|Element|void {
    if (!this.state.expanded) return void 0;
    if (this.props.fetchState.inProgress && this.state.allowAnimation) {
      return (
        <tr className="fc-table-tr">
          <td className="row-head-left" styleName="wait-td-left" />
          <td colSpan={this.props.columns.length - 1} styleName="wait-td">
            <WaitAnimation/>
          </td>
        </tr>
      );
    }
    if (!_.isEmpty(this.props.productVariants)) {
      return _.map(this.props.productVariants, pv => {
        return (
          <ProductVariantRow
            productVariant={pv}
            columns={this.props.columns}
            params={this.props.params}
          />
        );
      });
    }
  }

  render() {
    const { product, columns, params } = this.props;

    return (
      <tbody key={`group-${product.id}`} className="fc-table-body">
        <ProductRow
          key={`row-${product.id}`}
          product={product}
          columns={columns}
          params={params}
          toggleIcon={this.toggleIcon}
        />
        {this.productVariants}
      </tbody>
    );
  }
}

export default _.flowRight(
  makeLocalStore(addAsyncReducer(productVariantsReducer)),
  connect(mapLocalState, { fetchProductVariants })
)(ProductWithVariantsRow);
