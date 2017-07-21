/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind, debounce } from 'core-decorators';
import { isElementInViewport } from 'lib/dom-utils';
import * as tracking from 'lib/analytics';

// styles
import styles from './products-list.css';

// components
import ListItem from '../products-item/list-item';
import Loader from 'ui/loader';
import SortPill from 'components/sort-pill/sort-pill';
import ActionLink from 'ui/action-link/action-link';
import Button from 'ui/buttons';

export const LoadingBehaviors = {
  ShowLoader: 0,
  ShowWrapper: 1,
};

type Props = {
  changeSorting: Function,
  loadingBehavior?: 0|1,
  list: ?Array<Object>,
  isLoading: ?boolean,
  sorting: {
    direction: number,
    field: string,
  },
  changeSorting: Function,
  fetchMoreProducts: Function,
  moreAvailable: boolean,
  filterFor?: string,
  filterOnClick?: Function,
};

type State = {
  shownProducts: {[productId: string]: number},
};

class ProductsList extends Component {
  props: Props;
  state: State = {
    shownProducts: {},
  };
  _willUnmount: boolean = false;

  componentDidMount() {
    window.addEventListener('scroll', this.handleScroll);
  }

  componentWillUnmount() {
    this._willUnmount = true;
    window.removeEventListener('scroll', this.handleScroll);
  }

  @autobind
  @debounce(100)
  handleScroll() {
    if (this._willUnmount) return;
    this.trackProductView();
  }

  get loadMoreButton() {
    if (!this.props.moreAvailable) return null;

    return (
      <ActionLink
        action={this.loadMoreProducts}
        title="Load more"
        styleName="load-more"
      />
    );
  }

  renderProducts() {
    const products = _.map(this.props.list, (item, index) => {
      return (
        <ListItem
          size={this.props.size}
          {...item}
          index={index}
          key={`product-${item.id}`}
          ref={`product-${item.id}`}
        />
      );
    });

    return (
      <div styleName="list" ref={this.handleListRendered}>
        {products}
      </div>
    );
  }

  @autobind
  loadMoreProducts() {
    this.props.fetchMoreProducts();
  }

  trackProductView() {
    if (this.props.isLoading) return;

    const visibleProducts = this.getNewVisibleProducts();
    const shownProducts = {};

    if (visibleProducts.length > 0) {
      _.each(visibleProducts, (item) => {
        shownProducts[item.id] = 1;
        tracking.addImpression(item, item.index);
      });
      tracking.sendImpressions();
      this.setState({
        shownProducts: {
          ...this.state.shownProducts,
          ...shownProducts,
        },
      });
    }
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
          visibleProducts = [...visibleProducts, {...item, index: i}];
        }
      }
    }

    return visibleProducts;
  }

  @autobind
  handleListRendered() {
    setTimeout(() => {
      if (!this._willUnmount) this.trackProductView();
    }, 250);
  }

  get loadingWrapper(): ?Element<*> {
    if (this.props.isLoading && _.isEmpty(this.props.list)) {
      return (
        <div styleName="loading-wrapper">
          <div styleName="loader">
            <Loader />
          </div>
        </div>
      );
    }
  }

  get sorting(): Element<*> {
    const { sorting, changeSorting, filterFor, filterOnClick } = this.props;
    const filterLabel = filterFor ?
      (<label htmlFor={filterFor} styleName="sidebar-mobile-dofilter" onClick={filterOnClick}>
          Filters
      </label>) : false;
    return (
      <div styleName="sorting">
        <SortPill
          field="price"
          direction={sorting.direction}
          isActive={sorting.field === 'salePrice'}
          onClick={() => changeSorting('salePrice')}
        />
        <SortPill
          field="name"
          direction={sorting.direction}
          isActive={sorting.field === 'title'}
          onClick={() => changeSorting('title')}
        />
        <div styleName="mobile-filter-spacer" />
        {filterLabel}
      </div>
    );
  }

  get loadMoreBlock() {
    if (!this.props.moreAvailable) return null;

    return (
      <div styleName="load-more-block">
        <Button styleName="load-more-btn" onClick={this.props.fetchMoreProducts}>
          Load More
        </Button>
      </div>
    );
  }

  render() : Element<any> {
    const { props } = this;
    const { loadingBehavior = LoadingBehaviors.ShowLoader } = props;

    if (loadingBehavior == LoadingBehaviors.ShowLoader && props.isLoading) {
      return <Loader />;
    }

    const items = props.list && props.list.length > 0
      ? this.renderProducts()
      : <div styleName="not-found">No products found.</div>;

    return (
      <div styleName="list-wrapper">
        {this.loadingWrapper}
        <div>
          {items}
        </div>
        {this.loadMoreBlock}
      </div>
    );
  }
}

export default ProductsList;
