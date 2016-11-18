/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { browserHistory } from 'react-router';
import { autobind, debounce } from 'core-decorators';
import { isElementInViewport } from 'lib/dom-utils';
import * as tracking from 'lib/analythics';

// styles
import styles from './products-list.css';

// components
import ListItem from '../products-item/list-item';
import ProductTypeSelector from 'ui/product-type-selector';
import Loader from 'ui/loader';

// types
import type { HTMLElement } from 'types';
// import type { Product } from 'modules/products';

type Category = {
  name: string;
  id: number;
  description: string;
};

type Props = {
  list: ?Array<Object>,
  categories: ?Array<Category>,
  categoryName: ?string,
  productType: ?string,
  isLoading: ?boolean,
};

const mapStateToProps = state => ({
  categories: state.categories.list,
});

const productTypes = [
  'All',
  'Poultry',
  'Seafood',
  'Beef',
  'Vegetarian',
];

const defaultProductType = productTypes[0];

type State = {
  shownProducts: {[productId: string]: number},
}

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

  renderHeader() {
    const props = this.props;
    const { categoryName, categories } = props;

    const realCategoryName =
      decodeURIComponent(categoryName || '').toUpperCase().replace(/-/g, ' ');

    const category = _.find(categories, {
      name: realCategoryName,
    });

    if (!category || !categoryName ||
        (categoryName.toLowerCase() === defaultProductType.toLowerCase())) {
      return;
    }


    const description = (category && category.description)
      ? <p styleName="description">{category.description}</p>
      : '';
    const bgImageStyle = category.imageUrl ?
      { backgroundImage: `url(${category.imageUrl})` } : {};

    const className = `header-${categoryName}`;

    return (
      <header styleName={className}>
        <div styleName="header-wrap" style={bgImageStyle}>
          <div styleName="text-wrap">
            <span styleName="description">{description}</span>
            <h1 styleName="title">{category.name}</h1>
          </div>
        </div>
      </header>
    );
  }

  renderProducts() {
    return _.map(this.props.list, (item, index) => {
      return (
        <ListItem
          {...item}
          index={index}
          key={`product-${item.id}`}
          ref={`product-${item.id}`}
        />
      );
    });
  }

  trackProductView() {
    const visibleProducts = this.getNewVisibleProducts();
    const shownProducts = {};
    _.each(visibleProducts, item => {
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

  @autobind
  onDropDownItemClick (productType = '') {
    const { categoryName = defaultProductType.toUpperCase() } = this.props;

    if (productType.toLowerCase() !== defaultProductType.toLowerCase()) {
      browserHistory.push(`/${categoryName}/${productType.toUpperCase()}`);
    } else {
      browserHistory.push(`/${categoryName}`);
    }
  }

  render() : HTMLElement {
    const props = this.props;
    const { productType, isLoading } = props;
    const items = props.list && props.list.length > 0
      ? this.renderProducts()
      : <div styleName="not-found">No products found.</div>;

    const type = (productType && !_.isEmpty(productType))
      ? productType.toUpperCase()
      : productTypes[0];

    return (
      <section styleName="catalog">
        {this.renderHeader()}
        <div styleName="dropdown">
          <ProductTypeSelector
            items={productTypes}
            activeItem={type}
            onItemClick={this.onDropDownItemClick}
          />
        </div>

        {isLoading ?
          <Loader /> :
          <div styleName="list" ref={this.handleListRendered}>
            {items}
          </div>}
      </section>
    );
  }
}

export default connect(mapStateToProps, {})(ProductsList);
