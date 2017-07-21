/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { isElementInViewport } from 'lib/dom-utils';

// styles
import styles from './related-products-list.css';

// components
import Loader from 'ui/loader';
/* import ListItem from 'components/products-item/list-item';*/
import ListItem from 'components/related-products-item/related-list-item';

// types
import type { HTMLElement } from 'types';

export const LoadingBehaviors = {
  ShowLoader: 0,
  ShowWrapper: 1,
};

type Props = {
  loadingBehavior?: 0|1,
  list: ?Array<Object>,
  productsOrder: ?Array<number>,
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

  get renderProducts() {
    const { list, productsOrder } = this.props;

    if (_.isEmpty(list)) return null;

    let sortedProductsList = [];
    _.forEach(productsOrder, function(productId) {
      sortedProductsList = _.concat(sortedProductsList, _.find(list, { productId }));
    });

    const avoidKeyCollision = 9999;

    return _.map(sortedProductsList, (item, index) => {
      return (
        <ListItem
          {...item}
          index={index}
          key={`product-${_.get(item, 'id', _.random(avoidKeyCollision))}`}
          ref={`product-${_.get(item, 'id', _.random(avoidKeyCollision))}`}
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

    if (_.isEmpty(list)) return null;

    return (
      <div styleName="list-wrapper">
        {this.loadingWrapper}
        <div styleName="related-title">
          {title}
        </div>
        <div styleName="list">
          {this.renderProducts}
        </div>
      </div>
    );
  }
}

export default RelatedProductsList;
