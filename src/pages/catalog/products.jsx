/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as actions from 'modules/products';

// components
import ProductsList from '../../components/products-list/products-list';
import Select from 'ui/select/select';

// styles
import styles from './products.css';

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

// consts
const productTypes = [
  'All',
  'Poultry',
  'Seafood',
  'Beef',
  'Vegetarian',
];

const defaultProductType = productTypes[0];

const toLowerCaseAndCapitalize = (word: string) => {
  // Example: POULTRY -> Poultry
  const lowerCaseWord = word.toLowerCase();
  return lowerCaseWord.charAt(0).toUpperCase() + lowerCaseWord.slice(1);
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
      ? toLowerCaseAndCapitalize(productType)
      : productTypes[0];

    return (
      <section styleName="catalog">
        {this.renderHeader()}
        <div styleName="product-type-select">
          <Select
            inputProps={{
              type: 'text',
            }}
            getItemValue={item => item}
            items={productTypes}
            onSelect={this.onDropDownItemClick}
            selectedItem={type}
            sortItems={false}
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
