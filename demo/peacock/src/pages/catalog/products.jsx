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

type State = {
  sorting: {
    direction: number,
    field: string,
  },
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
  state: State = {
    sorting: {
      direction: 1,
      field: 'salePrice',
    },
  };

  componentWillMount() {
    const { categoryName, productType } = this.props.params;
    const { sorting } = this.state;
    this.props.fetch(categoryName, productType, sorting);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { categoryName, productType } = this.props.params;
    const {
      categoryName: nextCategoryName,
      productType: nextProductType,
    } = nextProps.params;

    if ((categoryName !== nextCategoryName) || (productType !== nextProductType)) {
      this.props.fetch(nextCategoryName, nextProductType, this.state.sorting);
    }
  }


  @autobind
  changeSorting(field: string) {
    const { sorting } = this.state;
    const direction = sorting.field === field
      ? sorting.direction * (-1)
      : sorting.direction;

    const newState = {
      field,
      direction,
    };

    this.setState({sorting: newState}, () => {
      const { categoryName, productType } = this.props.params;
      this.props.fetch(categoryName, productType, newState);
    });
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

    const className = `header-${categoryName}`;

    const title = (category.showNameCatPage)
      ? <h1 styleName="title">{category.name}</h1>
      : <h1 styleName="title">{category.description}</h1>;

    return (
      <header styleName={className}>
            {title}
      </header>
    );
  }

  render(): HTMLElement {
    return (
      <section styleName="catalog">
        {this.renderHeader()}
        <ProductsList
          sorting={this.state.sorting}
          changeSorting={this.changeSorting}
          list={this.props.list}
          isLoading={this.props.isLoading}
          loadingBehavior={LoadingBehaviors.ShowWrapper}
        />
      </section>
    );
  }
}

export default connect(mapStateToProps, actions)(Products);
