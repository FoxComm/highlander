/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as actions from 'modules/products';
import { categoryNameFromUrl } from 'paragons/categories';

// components
import ProductsList, { LoadingBehaviors } from 'components/products-list/products-list';
import Breadcrumbs from 'components/breadcrumbs/breadcrumbs';
import ErrorAlerts from 'ui/alerts/error-alerts';

// styles
import styles from './products.css';

// types
import { Route } from 'types';

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
  fetchState: AsyncState,
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
  return {
    ...state.products,
    fetchState: _.get(state.asyncActions, 'products', {}),
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
    this.props.fetch([categoryName, subCategory, leafCategory], sorting).catch(_.noop);
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

  get body(): Element<*> {
    const { err, finished } = this.props.fetchState;
    if (err) {
      return <ErrorAlerts styleName="products-error" error={err} />;
    }
    return (
      <ProductsList
        sorting={this.state.sorting}
        changeSorting={this.changeSorting}
        list={this.props.list}
        isLoading={!finished}
        loadingBehavior={LoadingBehaviors.ShowWrapper}
      />
    );
  }

  render(): Element<*> {
    return (
      <section styleName="catalog">
        {this.renderHeader()}
        {this.body}
      </section>
    );
  }
}

export default connect(mapStateToProps, actions)(Products);
