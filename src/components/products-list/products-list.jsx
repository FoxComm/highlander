/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';

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
  'Vegitarian',
];

const defaultProductType = productTypes[0];

class ProductsList extends Component {
  props: Props;

  renderHeader() {
    const props = this.props;
    const { categoryName } = props;

    if (!categoryName ||
        (categoryName.toLowerCase() === defaultProductType.toLowerCase())) {
      return;
    }

    const categoryInfo = _.find(props.categories, {name: categoryName});
    const description = (categoryInfo && categoryInfo.description)
      ? <p styleName="description">{categoryInfo.description}</p>
      : '';

    const className = `header-${categoryName}`;

    return (
      <header styleName={className}>
        <div styleName="header-wrap">
          <div styleName="text-wrap">
            <span styleName="description">{description}</span>
            <h1 styleName="title">{categoryName}</h1>
          </div>
        </div>
      </header>
    );
  }

  getItemList() {
    return _.map(this.props.list, (item) => {
      return (
        <ListItem
          {...item}
          key={`product-${item.id}`}
          ref={`product-${item.id}`}
        />
      );
    });
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
      ? this.getItemList()
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
          <div styleName="list">
            {items}
          </div>}
      </section>
    );
  }
}

export default connect(mapStateToProps, {})(ProductsList);
