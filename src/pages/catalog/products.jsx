/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import ProductsList from '../../components/products-list/products-list';
import ProductTypeSelector from 'ui/product-type-selector';

// styles
import styles from './products.css';

import * as actions from 'modules/products';

type Params = {
  categoryName: ?string,
  productType: ?string,
};

type Category = {
  name: string,
  id: number,
  description: string,
};

type Props = {
  params: Params,
  list: Array<Object>,
  categories: ?Array<Category>,
  isLoading: boolean,
  fetch: Function,
  location: any,
};

const productTypes = [
  'All',
  'Poultry',
  'Seafood',
  'Beef',
  'Vegetarian',
];

const defaultProductType = productTypes[0];

const mapStateToProps = state => {
  const async = state.asyncActions.products;

  return {
    ...state.products,
    isLoading: !!async ? async.inProgress : true,
    categories: state.categories.list,
  };
};

class Products extends Component {
  props: Props;

  componentWillMount() {
    const { categoryName, productType } = this.props.params;
    this.props.fetch(categoryName, productType);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { categoryName, productType } = this.props.params;
    const {
      categoryName: nextCategoryName,
      productType: nextProductType,
    } = nextProps.params;

    if ((categoryName !== nextCategoryName) || (productType !== nextProductType)) {
      this.props.fetch(nextCategoryName, nextProductType);
    }
  }

  categoryId(params: Params): ?number {
    const id = params.categoryName ? parseInt(params.categoryName, 10) : null;
    return isNaN(id) ? null : id;
  }

  @autobind
  onDropDownItemClick (productType = '') {
    const { categoryName = defaultProductType.toUpperCase() } = this.props.params;

    if (productType.toLowerCase() !== defaultProductType.toLowerCase()) {
      browserHistory.push(`/${categoryName}/${productType.toUpperCase()}`);
    } else {
      browserHistory.push(`/${categoryName}`);
    }
  }

  renderHeader() {
    const props = this.props;
    const { categories } = props;
    const { categoryName } = props.params;

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

  render(): HTMLElement {
    const { productType } = this.props.params;

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

        <ProductsList
          list={this.props.list}
          isLoading={this.props.isLoading}
        />
      </section>
    );
  }
}

export default connect(mapStateToProps, actions)(Products);
