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
import type { Route } from 'src/components/catalog/types';
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
  saveProductsFilters: Function,
  fetch: Function,
  location: any,
  routes: Array<Route>,
  routerParams: Object,
  facets: Array<Facet>,
  total: number,
  filters: {
    sorting: {
      direction: number,
      field: string,
    },
    toLoad: number,
    selectedFacets: {},
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

const facetWhitelist = [
  'GENDER', 'CATEGORY', 'COLOR', 'BRAND', 'SPORT',
];

class Products extends Component {
  props: Props;
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
    const categoryNames = [categoryName, subCategory, leafCategory];
    const { sorting, selectedFacets, toLoad } = this.props.filters;

    this.fetch(categoryNames, sorting, selectedFacets, toLoad);
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
      const categoryNames = [nextCategoryName, nextSubCategory, nextLeafCategory];
      const initialFilters = {
        sorting: {
          direction: 1,
          field: 'salePrice',
        },
        selectedFacets: {},
        toLoad: PAGE_SIZE,
      };
      const { sorting, selectedFacets, toLoad } = initialFilters;

      this.props.saveProductsFilters({
        sorting,
        selectedFacets,
        toLoad,
      });

      this.fetch(categoryNames, sorting, selectedFacets, toLoad, {
        pushHistory: true,
      });
    }
  }


  @autobind
  changeSorting(field: string) {
    const { sorting, selectedFacets } = this.props.filters;
    const direction = sorting.field === field
      ? sorting.direction * (-1)
      : sorting.direction;

    const newState = {
      field,
      direction,
    };

    const { categoryName, subCategory, leafCategory } = this.props.params;
    const categoryNames = [categoryName, subCategory, leafCategory];

    this.props.saveProductsFilters({
      sorting: newState,
      selectedFacets,
      toLoad: PAGE_SIZE,
    });

    this.fetch(categoryNames, newState, selectedFacets, PAGE_SIZE, {
      pushHistory: true,
    });
  }

  @autobind
  fetchMoreProducts() {
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const { sorting, selectedFacets, toLoad } = this.props.filters;

    const nextToLoad = toLoad + PAGE_SIZE;
    const categoryNames = [categoryName, subCategory, leafCategory];

    this.props.saveProductsFilters({
      sorting,
      selectedFacets,
      toLoad: nextToLoad,
    });

    this.fetch(categoryNames, sorting, selectedFacets, nextToLoad, {
      pushHistory: true,
    });
  }

  @autobind
  categoryName(categoryName: string) {
    return categoryNameFromUrl(categoryName);
  }

  @autobind
  newFacetSelectState(facet: string, value: string, selected: boolean) {
    const newSelection = this.props.filters.selectedFacets;
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
    return newSelection;
  }

  @autobind
  onSelectFacet(facet: string, value: string, selected: boolean) {
    const selectedFacets = this.newFacetSelectState(facet, value, selected);
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const categoryNames = [categoryName, subCategory, leafCategory];
    const { sorting, toLoad } = this.props.filters;

    this.props.saveProductsFilters({
      sorting,
      selectedFacets,
      toLoad,
    });

    this.fetch(categoryNames, sorting, selectedFacets, toLoad, {
      pushHistory: true,
    });
  }

  @autobind
  onSelectMobileFacet(facet, value, selected) {
    const selectedFacets = this.newFacetSelectState(facet, value, selected);
    const { sorting, toLoad } = this.props.filters;

    this.props.saveProductsFilters({
      sorting,
      selectedFacets,
      toLoad,
    });
  }

  @autobind
  hideMenuBar() {
    const header = document.getElementById('header');
    if (header) header.style.display = 'none';
  }

  @autobind
  showMenuBar() {
    const header = document.getElementById('header');
    if (header) header.style.display = 'block';
  }

  @autobind
  applyMobileFilters() {
    this.showMenuBar();
    const { categoryName, subCategory, leafCategory } = this.props.params;
    const categoryNames = [categoryName, subCategory, leafCategory];
    const { sorting, selectedFacets, toLoad } = this.props.filters;

    this.fetch(categoryNames, sorting, selectedFacets, toLoad, {
      pushHistory: true,
    });
  }

  get navBar(): ?Element<*> {
    return <div />;
  }

  determineRealCategoryName() {
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

    return realCategoryName;
  }

  renderSidebar() {
    const realCategoryName = this.determineRealCategoryName();

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
        <Facets
          prefix={'big'}
          facets={this.props.facets}
          whitelist={facetWhitelist}
          onSelect={this.onSelectFacet}
        />
      </div>
    );
  }

  renderMobileSidebar() {
    return (
      <div styleName="sidebar-mobile">
        <div styleName="sidebar-mobile-filter-header">
          <div styleName="sidebar-mobile-filters">Filters</div>
          <label
            htmlFor={'sidebar-mobile-checkbox'}
            styleName="sidebar-mobile-close"
            onClick={this.showMenuBar}
          >
            Close
          </label>
        </div>
        <Facets
          styleName="sidebar-mobile-facets"
          prefix={'mobile'}
          facets={this.props.facets}
          whitelist={facetWhitelist}
          onSelect={this.onSelectMobileFacet}
        />
        <div styleName="sidebar-mobile-footer">
          <label
            htmlFor={'sidebar-mobile-checkbox'}
            styleName="sidebar-mobile-apply"
            onClick={this.applyMobileFilters}
          >
            Apply Filters
          </label>
        </div>
      </div>
    );
  }

  renderContent() {
    const { finished } = this.props.fetchState;
    const moreAvailable = this.props.list.length !== this.props.total;

    return (
      <div styleName="content">
        <ProductsList
          sorting={this.props.filters.sorting}
          changeSorting={this.changeSorting}
          list={this.props.list}
          isLoading={!finished}
          loadingBehavior={LoadingBehaviors.ShowWrapper}
          fetchMoreProducts={this.fetchMoreProducts}
          moreAvailable={moreAvailable}
        />
      </div>);
  }

  renderMobileContent() {
    const realCategoryName = this.determineRealCategoryName();
    const moreAvailable = this.props.list.length !== this.props.total;

    const { finished } = this.props.fetchState;
    return (
      <div styleName="content-mobile">
        <div styleName="crumbs">
          <Breadcrumbs
            routes={this.props.routes}
            params={this.props.routerParams}
          />
        </div>
        <div styleName="title">
          {realCategoryName}
        </div>
        <ProductsList
          sorting={this.props.filters.sorting}
          changeSorting={this.changeSorting}
          list={this.props.list}
          isLoading={!finished}
          loadingBehavior={LoadingBehaviors.ShowWrapper}
          fetchMoreProducts={this.fetchMoreProducts}
          moreAvailable={moreAvailable}
          filterOnClick={this.hideMenuBar}
          filterFor={'sidebar-mobile-checkbox'}
        />
      </div>);
  }

  get body(): Element<any> {
    const { err } = this.props.fetchState;
    if (err) {
      return <ErrorAlerts styleName="products-error" error={err} />;
    }

    return (
      <div>
        <div styleName="dropDown">
          {this.navBar}
        </div>
        <div styleName="facetted-container-mobile">
          <input styleName="sidebar-mobile-checkbox" id="sidebar-mobile-checkbox" type="checkbox" />
          {this.renderMobileSidebar()}
          {this.renderMobileContent()}
        </div>
        <div styleName="facetted-container">
          {this.renderSidebar()}
          {this.renderContent()}
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
