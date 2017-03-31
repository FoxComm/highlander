/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { isElementInViewport } from 'lib/dom-utils';

// styles
import styles from './related-products-list.css';

// components
import RelatedListItem from '../related-products-item/related-list-item';
import Loader from 'ui/loader';

// types
import type { HTMLElement } from 'types';

export const LoadingBehaviors = {
  ShowLoader: 0,
  ShowWrapper: 1,
};

type Props = {
  loadingBehavior?: 0|1,
  list: ?Array<Object>,
  isLoading: ?boolean,
  title: string,
};

type State = {
  shownProducts: {[productId: string]: number},
}

class RelatedProductsList extends Component {
  props: Props;
  state: State = {
    shownProducts: {},
  };
  _willUnmount: boolean = false;

  renderProducts() {
    return _.map(this.props.list, (item, index) => {
      return (
        <RelatedListItem
          {...item}
          index={index}
          key={`product-${item.id}`}
          ref={`product-${item.id}`}
        />
      );
    });
  }

  getNewVisibleProducts() {
    const { props } = this;
    let visibleProducts = [];
    const { shownProducts } = this.state;

    const products = _.get(props, 'list', []);

    for (let i = 0; i < products.length; i++) {
      const item = products[i];
      if (item.id in shownProducts) continue;

      const node = this.refs[`product-${item.id}`].getWrappedInstance().getImageNode();
      if (node) {
        if (isElementInViewport(node)) {
          visibleProducts = [...visibleProducts, { ...item, index: i }];
        }
      }
    }

    return visibleProducts;
  }

  get loadingWrapper(): ?HTMLElement {
    if (this.props.isLoading) {
      return (
        <div styleName="loading-wrapper">
          <div styleName="loader">
            <Loader />
          </div>
        </div>
      );
    }
  }

  render(): HTMLElement {
    const { loadingBehavior = LoadingBehaviors.ShowLoader, isLoading, list, title } = this.props;
    if (loadingBehavior == LoadingBehaviors.ShowLoader && isLoading) {
      return <Loader />;
    }
    const items = list && list.length > 0
      ? this.renderProducts()
      : false;

    return (
      <div styleName="list-wrapper">
        {this.loadingWrapper}
        <div styleName="related-title">
          {title}
        </div>
        <div styleName="list">
          {items}
        </div>
      </div>
    );
  }
}

export default RelatedProductsList;
