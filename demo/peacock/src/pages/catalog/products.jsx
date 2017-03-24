/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as actions from 'modules/products';
import { categoryNameFromUrl } from 'paragons/categories';

// components
import ProductsList, { LoadingBehaviors } from 'components/products-list/products-list';
import Breadcrumbs from 'components/breadcrumbs/breadcrumbs';

// styles
import styles from './products.css';

// types
import { Element, Route } from 'types';

type Params = {
  categoryName: ?string,
  subCategory: ?string,
  leafCategory: ?string,
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
  routes: Array<Route>,
  routerParams: Object,
};

type State = {
  sorting: {
    direction: number,
    field: string,
  },
};

// redux
const mapStateToProps = (state) => {
  const async = state.asyncActions.products;

  return {
    ...state.products,
    isLoading: async ? async.inProgress : true,
    categories: state.categories.list,
  };
};

class Products extends Component {
  props: Props;
  state: State = {
    sorting: {
      direction: 1,
      field: 'salePrice',
    },
  };

  componentWillMount() {
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const { sorting } = this.state;
    this.props.fetch([categoryName, subCategory, leafCategory], sorting);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const {
      categoryName: nextCategoryName,
      subCategory: nextSubCategory,
      leafCategory: nextLeafCategory,
    } = nextProps.params;

    if ((categoryName !== nextCategoryName) ||
        (subCategory !== nextSubCategory) ||
        (leafCategory !== nextLeafCategory)) {
      this.props.fetch([nextCategoryName, nextSubCategory, nextLeafCategory], this.state.sorting);
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
      const { categoryName, subCategory, leafCategory } = this.props.params;
      this.props.fetch([categoryName, subCategory, leafCategory], newState);
    });
  }

  @autobind
  categoryName(categoryName: string) {
    return categoryNameFromUrl(categoryName);
  }

  @autobind
  renderHeader() {
    const { params } = this.props;
    const { categoryName, subCategory, leafCategory } = params;

    let realCategoryName = '';
    if (leafCategory) {
      realCategoryName = this.categoryName(leafCategory);
    } else if (subCategory) {
      realCategoryName = this.categoryName(subCategory);
    } else if (categoryName) {
      realCategoryName = this.categoryName(categoryName);
    }

    return (
      <header styleName="header">
        <div styleName="crumbs">
          <Breadcrumbs
            routes={this.props.routes}
            params={this.props.routerParams}
          />
        </div>
        <div>
          <h1 styleName="title">{realCategoryName}</h1>
        </div>
      </header>
    );
  }

  render(): Element<*> {
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
