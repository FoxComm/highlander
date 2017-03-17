/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import { browserHistory } from 'lib/history';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as actions from 'modules/products';
import { assetsUrl } from 'lib/env';

// components
import ProductsList, { LoadingBehaviors } from 'components/products-list/products-list';
import Facets from 'components/facets/facets';
import ProductTypeSelector from 'ui/product-type-selector';

// styles
import styles from './products.css';

// constants
import { productTypes } from 'modules/categories';

// types
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

// redux
const mapStateToProps = state => {
  const async = state.asyncActions.products;

  return {
    ...state.products,
    isLoading: !!async ? async.inProgress : true,
    categories: state.categories.list,
  };
};

const defaultProductType = productTypes[0];

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

    const description = (category && category.description && category.showNameCatPage)
      ? <p styleName="description">{category.description}</p>
      : '';

    const bgImageStyle = category.imageUrl ?
    { backgroundImage: `url(${assetsUrl(category.imageUrl)})` } : {};

    const className = `header-${categoryName}`;

    const title = (category.showNameCatPage)
      ? <h1 styleName="title">{category.name}</h1>
      : <h1 styleName="title">{category.description}</h1>;

    return (
      <header styleName={className}>
        <div styleName="header-wrap" style={bgImageStyle}>
          <div styleName="text-wrap">
            <span styleName="description">{description}</span>
            {title}
          </div>
        </div>
      </header>
    );
  }

  get navBar() {
    const { categoryName, productType } = this.props.params;

    const type = (productType && !_.isEmpty(productType))
      ? _.capitalize(productType)
      : productTypes[0];

    if (categoryName == 'ENTRÉES') {
      return (
        <ProductTypeSelector
          items={productTypes}
          activeItem={type}
          onItemClick={this.onDropDownItemClick}
        />
      );
    }
    return null;
  }

  render(): HTMLElement {
    return (
      <section styleName="catalog">
        {this.renderHeader()}
        <div styleName="dropDown">
          {this.navBar}
        </div>
        <div styleName="facetted-container">
          <div styleName="sidebar">
            <Facets facets={[]} />
          </div>
          <div styleName="content">
            <ProductsList
              list={this.props.list}
              isLoading={this.props.isLoading}
              loadingBehavior={LoadingBehaviors.ShowWrapper}
            />
          </div>
        </div>
      </section>
    );
  }
}

export default connect(mapStateToProps, actions)(Products);
