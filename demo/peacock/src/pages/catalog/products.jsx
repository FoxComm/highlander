/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as actions from 'modules/products';
import { categoryNameFromUrl } from 'paragons/categories';
import { PAGE_SIZE } from 'modules/products';

// components
import ProductsList, { LoadingBehaviors } from 'components/products-list/products-list';
import Facets from 'components/facets/facets';
import Breadcrumbs from 'components/breadcrumbs/breadcrumbs';
import ErrorAlerts from 'ui/alerts/error-alerts';

// styles
import styles from './products.css';

// types
import type { Route } from 'types';
import type { Facet } from 'types/facets';
import type { AbortablePromise } from 'types/promise';

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
  facets: Array<Facet>,
  total: number,
};

type State = {
  sorting: {
    direction: number,
    field: string,
  },
  toLoad: number,
  selectedFacets: {},
};

// redux
const mapStateToProps = (state) => {
  return {
    ...state.products,
    fetchState: _.get(state.asyncActions, 'products', {}),
    categories: state.categories.list,
  };
};

const facetWhitelist = [
  'GENDER', 'CATEGORY', 'COLOR', 'BRAND', 'SPORT',
];

class Products extends Component {
  props: Props;
  state: State = {
    sorting: {
      direction: 1,
      field: 'salePrice',
    },
    toLoad: PAGE_SIZE,
    selectedFacets: {},
  };
  lastFetch: ?AbortablePromise<*>;

  fetch(...args): void {
    if (this.lastFetch && this.lastFetch.abort) {
      this.lastFetch.abort();
      this.lastFetch = null;
    }
    this.lastFetch = this.props.fetch(...args);
    this.lastFetch.catch(_.noop);
  }

  componentWillMount() {
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const { sorting, selectedFacets, toLoad } = this.state;
    this.fetch([categoryName, subCategory, leafCategory], sorting, selectedFacets, toLoad);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const {
      categoryName: nextCategoryName,
      subCategory: nextSubCategory,
      leafCategory: nextLeafCategory,
    } = nextProps.params;

    const mustInvalidate = (categoryName !== nextCategoryName) ||
      (subCategory !== nextSubCategory) ||
      (leafCategory !== nextLeafCategory);

    if (mustInvalidate) {
      this.fetch(
        [nextCategoryName, nextSubCategory, nextLeafCategory],
        this.state.sorting,
        this.state.selectedFacets,
        this.state.toLoad
      );
    }
  }


  @autobind
  changeSorting(field: string) {
    const { sorting, selectedFacets } = this.state;
    const direction = sorting.field === field
      ? sorting.direction * (-1)
      : sorting.direction;

    const newState = {
      field,
      direction,
    };

    this.setState({selectedFacets, sorting: newState, toLoad: PAGE_SIZE}, () => {
      const { categoryName, subCategory, leafCategory } = this.props.params;
      this.props.fetch([categoryName, subCategory, leafCategory], newState, selectedFacets, PAGE_SIZE);
    });
  }

  @autobind
  fetchMoreProducts() {
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const { sorting, selectedFacets, toLoad } = this.state;

    const nextToLoad = toLoad + PAGE_SIZE;
    this.setState({ toLoad: nextToLoad }, () => {
      this.props.fetch([categoryName, subCategory, leafCategory], sorting, selectedFacets, nextToLoad);
    });
  }

  @autobind
  categoryName(categoryName: string) {
    return categoryNameFromUrl(categoryName);
  }

  @autobind
  onSelectFacet(facet, value, selected) {
    const newSelection = this.state.selectedFacets;
    if (selected) {
      if (facet in newSelection) {
        newSelection[facet].push(value);
      } else {
        newSelection[facet] = [value];
      }
    } else if (facet in newSelection) {
      const values = newSelection[facet];
      newSelection[facet] = _.filter(values, (v) => {
        return v != value;
      });
    }

    this.setState({selectedFacets: newSelection, sorting: this.state.sorting, toLoad: PAGE_SIZE}, () => {
      const { categoryName, subCategory, leafCategory } = this.props.params;
      this.fetch([categoryName, subCategory, leafCategory], this.state.sorting, newSelection, PAGE_SIZE);
    });
  }

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

  get navBar(): ?Element<*> {
    return <div />;
  }

  renderSidebar() {
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
      <div styleName="sidebar">
        <div styleName="crumbs">
          <Breadcrumbs
            routes={this.props.routes}
            params={this.props.routerParams}
          />
        </div>
        <div styleName="title">
          {realCategoryName}
        </div>
        <Facets facets={this.props.facets} whitelist={facetWhitelist} onSelect={this.onSelectFacet} />
      </div>
    );
  }

  get body(): Element<any> {
    const { err, finished } = this.props.fetchState;
    if (err) {
      return <ErrorAlerts styleName="products-error" error={err} />;
    }

    const moreAvailable = this.props.list.length !== this.props.total;
    return (
      <div>
        <div styleName="dropDown">
          {this.navBar}
        </div>
        <div styleName="facetted-container">
          {this.renderSidebar()}
          <div styleName="content">
            <ProductsList
              sorting={this.state.sorting}
              changeSorting={this.changeSorting}
              list={this.props.list}
              isLoading={!finished}
              loadingBehavior={LoadingBehaviors.ShowWrapper}
              fetchMoreProducts={this.fetchMoreProducts}
              moreAvailable={moreAvailable}
            />
          </div>
        </div>
      </div>
    );
  }

  render(): Element<*> {
    return (
      <section styleName="catalog">
        {this.body}
      </section>
    );
  }
}

export default connect(mapStateToProps, actions)(Products);
