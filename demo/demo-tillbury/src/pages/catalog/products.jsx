/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as actions from 'modules/products';
import { categoryNameToUrl, categoryNameFromUrl } from 'paragons/categories';
import { PAGE_SIZE, MAX_RESULTS } from 'modules/products';
import classNames from 'classnames';
import { update, deepMerge, assoc } from 'sprout-data';
import { browserHistory } from 'lib/history';

// components
import ProductsList, { LoadingBehaviors } from 'components/products-list/products-list';
import Facets from 'components/facets/facets';
import Filters from 'components/filters/filters';
import FilterGroup from 'components/filters/filter-group';
import FilterCheckboxes from 'components/filters/filter-checkboxes';
import FilterColors from 'components/filters/filter-colors';
import Breadcrumbs from 'components/breadcrumbs/breadcrumbs';
import ErrorAlerts from 'ui/alerts/error-alerts';
import Icon from 'ui/icon';
import Button from 'ui/buttons';
import Dropdown from 'components/dropdown/dropdown';
import Pager from 'components/pager/pager';
import ActionLink from 'ui/action-link/action-link';

// styles
import styles from './products.css';

// types
import type { Route } from 'types';
import type { Facet, FacetValue } from 'types/facets';
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

type SelectedFacetsType = {
  [key: string]: Array<string>,
};

type FiltersType = {
  sorting: {
    direction: number,
    field: string,
  },
  toLoad: number,
  from: number,
}

const initialFilterValues: FiltersType = {
  sorting: {
    direction: 1,
    field: 'salePrice',
  },
  from: 0,
  toLoad: PAGE_SIZE,
};

type State = {
  withFilters: boolean,
  facets: Array<Facet>,
};

type ColorValue = {
  color: string,
  value: string,
};

function isFacetValueSelected(facets: ?Array<string>, value: string | ColorValue) {
  if (typeof value !== 'string') return _.includes(facets, value.value);
  return _.includes(facets, value);
}

function markFacetValuesAsSelected(facets: Array<Facet>, selectedFacets: Object): Array<Facet> {
  return _.map(facets, facetItem => ({
    ...facetItem,
    values: _.map(facetItem.values, (facetValueItem: FacetValue) => ({
      ...facetValueItem,
      selected: isFacetValueSelected(selectedFacets[facetItem.key], facetValueItem.value),
    })),
  }));
}

function mergeFacets(prevFacets, nextFacets, selectedFacets) {
  let facets = [];

  // The only time this should be empty is on first call.
  if (_.isEmpty(prevFacets)) {
    facets = nextFacets;
  } else {
    // Merge aggregations from queries into existing state.
    // Keep existing selected facets and only change unselected ones..
    // This avoids queries that would return empty results.
    // While also keeping the interface from changing too much.
    const groupedQueryFacets = _.groupBy(nextFacets, 'key');

    facets = _.compact(_.map(prevFacets, (v) => {
      if (_.isEmpty(groupedQueryFacets) || !_.isEmpty(selectedFacets[v.key])) {
        return v;
      }

      return _.isArray(groupedQueryFacets[v.key]) ? groupedQueryFacets[v.key][0] : null;
    }));
  }

  return markFacetValuesAsSelected(facets, selectedFacets);
}

// redux
const mapStateToProps = (state) => {
  return {
    ...state.products,
    fetchState: _.get(state.asyncActions, 'products', {}),
    categories: state.categories.list,
  };
};

const facetWhitelist = [
  'CATEGORY',
];

const ASC = 1;
const DESC = -1;

class Products extends Component {
  props: Props;
  lastFetch: ?AbortablePromise<*>;
  filters: Filters = initialFilterValues;
  _facetsToBeApplied: ?SelectedFacetsType;

  state: State = {
    withFilters: true,
    facets: mergeFacets([], this.props.facets, this.getSelectedFacets()),
  };

  fetch(props: Props = this.props): void {
    if (this.lastFetch && this.lastFetch.abort) {
      this.lastFetch.abort();
      this.lastFetch = null;
    }
    const categoryNames = this.getCategoryNames(props);
    const filters = this.filters;

    const selectedFacets = this.getSelectedFacets(props);

    this.lastFetch = this.props.fetch(
      categoryNames,
      filters.sorting,
      selectedFacets,
      filters.toLoad,
      filters.from
    );
    this.lastFetch.catch(_.noop);
  }

  getSelectedFacets(props: Props = this.props): {[key: string]: Array<string>} {
    const { query } = props.location;

    return _.reduce(query, (acc, cur, key) => {
      const terms = _.map(_.isString(cur) ? [cur] : cur, value => decodeURIComponent(value).replace(/\+/g, ' '));
      return {
        ...acc,
        [key]: terms,
      };
    }, {});
  }

  componentWillMount() {
    this.fetch();
  }

  componentWillReceiveProps(nextProps: Props) {
    this.updateFetchFilters(this.filters, nextProps);
    if (this.props.facets != nextProps.facets) {
      this.setState({
        facets: mergeFacets(this.state.facets, nextProps.facets, this.getSelectedFacets(nextProps)),
      });
    }
  }

  getCategoryNames(props: Props = this.props): Array<string> {
    const { categoryName, subCategory, leafCategory } = props.params;
    return [categoryName, subCategory, leafCategory];
  }

  updateFetchFilters(nextFilters: $Shape<FiltersType>, nextProps: ?Props) {
    // navigate in case of facets
    // set value
    let filters = this.filters;
    let newFilters = nextFilters;

    let changedCategoryNames = false;
    if (nextProps) {
      const categoryNames = this.getCategoryNames();
      const nextCategoryNames = this.getCategoryNames(nextProps);

      changedCategoryNames = !_.isEqual(categoryNames, nextCategoryNames);
    }

    let facetsChanged = false;
    if (nextProps) {
      const selectedFacets = this.getSelectedFacets();
      const nextSelectedFacets = this.getSelectedFacets(nextProps);

      facetsChanged = !_.isEqual(selectedFacets, nextSelectedFacets);
    }

    if (facetsChanged) {
      newFilters = assoc(newFilters, 'from', 0);
    }

    if (changedCategoryNames) {
      filters = initialFilterValues;
    } else {
      Object.keys(newFilters).forEach((key) => {
        filters = update(filters, key, deepMerge, newFilters[key]);
      });
    }
    const changedFilters = this.filters !== filters;
    this.filters = filters;

    if (changedFilters || changedCategoryNames || facetsChanged) {
      this.fetch(nextProps);
    }
  }

  @autobind
  changeSorting(field: string, direction: number) {
    const newState = {
      field,
      direction,
    };

    this.updateFetchFilters({
      sorting: newState,
    });
  }

  @autobind
  fetchOtherNumberOfProducts(numberToLoad: number) {
    this.updateFetchFilters({
      toLoad: numberToLoad,
      from: 0,
    });
  }

  @autobind
  fetchMoreProducts() {
    const { toLoad } = this.filters;
    const nextToLoad = toLoad + PAGE_SIZE;
    this.updateFetchFilters({
      toLoad: nextToLoad,
      from: 0,
    });
  }

  @autobind
  categoryName(categoryName: string) {
    return categoryNameFromUrl(categoryName);
  }

  @autobind
  newFacetSelectState(facet: string, value: string, selected: boolean) {
    const newSelection = this.getSelectedFacets();
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

  updateFacets(newSelectedFacets) {
    // do optimistic update first
    const optimisticUpdatedFacets = markFacetValuesAsSelected(this.state.facets, newSelectedFacets);
    this.setState({
      facets: optimisticUpdatedFacets,
    });
    browserHistory.push({
      pathname: this.props.location.pathname,
      query: newSelectedFacets,
    });
  }

  @autobind
  onSelectFacet(facet: string, value: string, selected: boolean) {
    const selectedFacets = this.newFacetSelectState(facet, value, selected);

    this.updateFacets(selectedFacets);
  }

  @autobind
  onSelectMobileFacet(facet, value, selected) {
    const selectedFacets = this.newFacetSelectState(facet, value, selected);

    this._facetsToBeApplied = selectedFacets;

    this.hideMobileSidebar();
    this.applyMobileFilters();
  }

  @autobind
  clearAllFacets() {
    this._facetsToBeApplied = {};

    this.hideMobileSidebar();
    this.applyMobileFilters();
  }

  @autobind
  clearFacet(facet: string) {
    const selectedFacets = _.omit(this.getSelectedFacets(), facet);

    this.updateFacets(selectedFacets);
  }

  @autobind
  hideMobileSidebar() {
    /*
     * This is a hack to avoid rewriting mobile filter block controll flow
     * it is controlled by clicks on `label` with `htmlFor` for hidden checkbox
     */
    const hackCheckbox = document.getElementById('closeMobileFiltersHack');
    if (hackCheckbox) {
      hackCheckbox.click();
    }
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
    this.updateFacets(this._facetsToBeApplied);
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

  renderTitle() {
    const realCategoryName = this.determineRealCategoryName();
    const { params } = this.props;
    const { categoryName, subCategory, leafCategory } = params;

    let categorySlug = '';
    if (leafCategory) {
      categorySlug = leafCategory;
    } else if (subCategory) {
      categorySlug = subCategory;
    } else if (categoryName) {
      categorySlug = categoryName;
    }

    const reduceCats = (cats) => {
      const reduced = _.reduce(cats, (acc, cat) => {
        let result = acc;
        if (cat.children) {
          result = result.concat(reduceCats(cat.children));
        }
        result = result.concat([cat]);
        return result;
      }, []);
      return reduced;
    };

    const allCats = reduceCats(this.props.categories);

    const category = _.find(allCats, (c) => {
      return categoryNameToUrl(c.name) == categoryNameToUrl(categorySlug);
    });

    const heading = _.get(category, 'heading', '');
    const image = _.get(category, 'catalogImage', '');
    const name = _.get(category, 'name', '');
    const withoutTitle = _.get(category, 'withoutTitle', false);

    const titleClass = classNames(styles['title-heading'], {
      [styles._black]: image == '',
    });

    return (
      <div styleName="header-block">
        <div styleName="crumbs">
          <Breadcrumbs
            routes={this.props.routes}
            params={this.props.routerParams}
          />
        </div>
        <div styleName="title">
          { image != '' && <img src={image} styleName="title-image" /> }
          { !withoutTitle && <h1 className={titleClass}>{name}</h1> }
        </div>
      </div>
    );
  }

  renderFilters(onSelectFacet = this.onSelectFacet) {
    return (
      <Filters
        filters={this.state.facets}
        onSelectFacet={onSelectFacet}
        onClearFacet={this.clearFacet}
      >
        <FilterGroup label="Product Type" term="producttype">
          <FilterCheckboxes />
        </FilterGroup>
        <FilterGroup label="Color Group" term="colorGroup">
          <FilterColors />
        </FilterGroup>
        <FilterGroup label="Gender" term="gender">
          <FilterCheckboxes />
        </FilterGroup>
        <FilterGroup label="Price" term="price">
          <FilterCheckboxes />
        </FilterGroup>
        <FilterGroup label="Collection" term="collection">
          <FilterCheckboxes />
        </FilterGroup>
        <FilterGroup label="Material" term="material">
          <FilterCheckboxes />
        </FilterGroup>
        <FilterGroup label="Laptop Size" term="laptopSize">
          <FilterCheckboxes />
        </FilterGroup>
        <FilterGroup label="Wheels" term="wheels">
          <FilterCheckboxes />
        </FilterGroup>
        <FilterGroup label="Exclusive Features" term="features">
          <FilterCheckboxes />
        </FilterGroup>
      </Filters>
    );
  }

  renderSidebar() {
    const style = classNames(styles.sidebar, {
      [styles._shrinked]: !this.state.withFilters,
    });

    return (
      <div className={style}>
        {this.renderFilters()}
      </div>
    );
  }

  renderMobileSidebar() {
    return (
      <div styleName="sidebar-mobile">
        <div styleName="sidebar-mobile-overlay" onClick={this.hideMobileSidebar} />
        <div styleName="sidebar-mobile-container">
          <div styleName="sidebar-mobile-filter-header">
            <div styleName="sidebar-mobile-filters">Filters</div>
            <label
              id="closeMobileFiltersHack"
              htmlFor="sidebar-mobile-checkbox"
              styleName="sidebar-mobile-close"
            >
              Close
            </label>
          </div>
          <div styleName="sidebar-mobile-facets-title">
            <div styleName="sidebar-mobile-facets-title-left">
              FILTER
            </div>
            <div styleName="sidebar-mobile-facets-title-right">
              <ActionLink action={this.clearAllFacets} title="Clear All" styleName="clear-all-btn" />
            </div>
          </div>
          {this.renderFilters(this.onSelectMobileFacet)}
        </div>
      </div>
    );
  }

  renderContent() {
    const { finished } = this.props.fetchState;
    const moreAvailable = this.props.list.length !== this.props.total;

    const style = classNames(styles.content, {
      [styles._shrinked]: this.state.withFilters,
    });

    return (
      <div className={style}>
        <ProductsList
          sorting={this.filters.sorting}
          changeSorting={this.changeSorting}
          list={this.props.list}
          isLoading={!finished}
          loadingBehavior={LoadingBehaviors.ShowWrapper}
          fetchMoreProducts={this.fetchMoreProducts}
          moreAvailable={moreAvailable}
        />
      </div>
    );
  }

  renderMobileContent() {
    const moreAvailable = this.props.list.length !== this.props.total;

    const { finished } = this.props.fetchState;
    return (
      <div styleName="content-mobile">
        <ProductsList
          sorting={this.filters.sorting}
          changeSorting={this.changeSorting}
          list={this.props.list}
          isLoading={!finished}
          loadingBehavior={LoadingBehaviors.ShowWrapper}
          fetchMoreProducts={this.fetchMoreProducts}
          moreAvailable={moreAvailable}
          filterOnClick={this.hideMenuBar}
          filterFor={'sidebar-mobile-checkbox'}
        />
      </div>
    );
  }

  get showHideButton() {
    const text = this.state.withFilters
      ? (<div><Icon name="fc-breadcrumbs-arrow" styleName="hide-breadcrumb" />Hide Filters</div>)
      : (<div>Show Filters<Icon name="fc-breadcrumbs-arrow" styleName="show-breadcrumb" /></div>);

    const buttonStyle = classNames(styles['show-hide-search'], {
      [styles._show]: !this.state.withFilters,
      [styles._hide]: this.state.withFilters,
    });

    return (
      <Button className={buttonStyle} onClick={this.toggleSidebar}>
        { text }
      </Button>
    );
  }

  @autobind
  toggleSidebar() {
    this.setState({ withFilters: !this.state.withFilters });
  }

  get sortOrderItems(): Array<Object> {
    return [
/*      { component: 'Newest', onSelect: () => this.changeSorting('createdAt', ASC) }, -- No field in ES */
      { component: 'Name: A to Z', onSelect: () => this.changeSorting('title', ASC) },
      { component: 'Name: Z to A', onSelect: () => this.changeSorting('title', DESC) },
      { component: 'Price: Lowest to Highest', onSelect: () => this.changeSorting('salePrice', ASC) },
      { component: 'Price: Highest to Lowest', onSelect: () => this.changeSorting('salePrice', DESC) },
    ];
  }

  get perPageItems(): Array<Object> {
    return [
      {component: `${PAGE_SIZE} items`, onSelect: () => this.fetchOtherNumberOfProducts(PAGE_SIZE) },
      {component: 'All items', onSelect: () => this.fetchOtherNumberOfProducts(MAX_RESULTS) },
    ];
  }

  @autobind
  setPagerState({ from }: { from: number }) {
    this.updateFetchFilters({
      from,
    });
  }

  @autobind
  listControlls(top: boolean = false) {
    const { toLoad, from } = this.filters;
    const { total } = this.props;
    const count = total != 1 ? `${total} items` : `${total} item`;
    return (
      <div styleName="faceted-controlls">
        <div styleName="search-controlls">
          {top && this.showHideButton}
        </div>
        <div styleName="list-controlls">
          <div styleName="item-qnt">
            {count}
          </div>
          <div styleName="sort-order">
            <Dropdown items={this.sortOrderItems} className={styles['sort-order-dd']} />
          </div>
          <div styleName="per-page">
            <Dropdown items={this.perPageItems} className={styles['per-page-dd']} />
          </div>
          <div styleName="paging">
            <Pager
              className={styles.pager}
              total={total}
              size={toLoad}
              from={from}
              setState={this.setPagerState}
            />
          </div>
        </div>
      </div>
    );
  }


  @autobind
  mobileListControlls() {
    const { total } = this.props;
    const count = total != 1 ? `${total} items` : `${total} item`;
    return (
      <div styleName="mobile-faceted-container">
        <div styleName="list-controlls">
          <div styleName="search-controlls">
            <label
              className={`${styles['show-filters']} ${styles._show}`}
              htmlFor="sidebar-mobile-checkbox"
            >
              Filters
              <Icon name="fc-breadcrumbs-arrow" styleName="show-breadcrumb" />
            </label>
          </div>
          <div styleName="item-qnt">
            {count}
          </div>
          <div styleName="sort-order">
            <Dropdown items={this.sortOrderItems} className={styles['sort-order-dd']} />
          </div>
        </div>
      </div>
    );
  }

  get body(): Element<any> {
    const { err } = this.props.fetchState;
    if (err) {
      return <ErrorAlerts styleName="products-error" error={err} />;
    }

    return (
      <div>
        <div styleName="header-container">
          {this.renderTitle()}
        </div>
        <div styleName="dropDown">
          {this.navBar}
        </div>
        {this.mobileListControlls()}
        <div styleName="facetted-container-mobile">
          <input styleName="sidebar-mobile-checkbox" id="sidebar-mobile-checkbox" type="checkbox" />
          {this.renderMobileSidebar()}
          {this.renderMobileContent()}
        </div>

        <div styleName="facetted-container">
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
