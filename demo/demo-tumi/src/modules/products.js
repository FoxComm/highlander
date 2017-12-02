/* @flow */

import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcommerce/wings';
import {
  addPriceFilter,
  addTaxonomyFilter,
  addTaxonomiesAggregation,
  addMatchQuery,
  addMustNotFilter, defaultSearch, termFilter, termsFilter, addCategoryFilter, addTermFilter, addTermsFilter,
} from 'lib/elastic';
import _ from 'lodash';
import { api } from 'lib/api';
import { assoc } from 'sprout-data';

import type { Facet } from 'types/facets';

export type Product = {
  id: number;
  context: string,
  title: string;
  description: string,
  images: ?Array<string>,
  currency: string,
  productId: number,
  salePrice: string,
  scope: string,
  skus: Array<string>,
  tags: Array<string>,
  albums: ?Array<Object> | Object,
};

export const MAX_RESULTS = 1000;
export const PAGE_SIZE = 30;
const context = process.env.STOREFRONT_CONTEXT || 'default';
export const GIFT_CARD_TAG = 'GIFT-CARD';

export const resetSearch = createAction('PRODUCTS_RESET_SEARCH');

type QueryOpts = {
  sorting?: { direction: number, field: string },
  toLoad: number,
  from: number,
  ignoreGiftCards?: boolean,
}

function apiCall(categoryNames: ?Array<string>,
                 selectedFacets: Object,
                 searchTerm: ?string,
                 {
                   sorting,
                   toLoad,
                   from = 0,
                   ignoreGiftCards = true,
                 }: QueryOpts = {}): Promise<*> {
  let payload = defaultSearch(String(context));

  _.forEach(_.compact(categoryNames), (cat) => {
    if (cat !== 'ALL' && cat !== GIFT_CARD_TAG) {
      payload = addCategoryFilter(payload, cat.toUpperCase());
    } else if (cat === GIFT_CARD_TAG) {
      const tagTerm = termFilter('tags', cat.toUpperCase());
      payload = addTermFilter(payload, tagTerm);
    }
  });
  if (searchTerm) {
    payload = addMatchQuery(payload, searchTerm);
  }

  if (ignoreGiftCards) {
    const giftCardTerm = termFilter('tags', GIFT_CARD_TAG);
    payload = addMustNotFilter(payload, giftCardTerm);
  }

  if (sorting) {
    const order = sorting.direction === -1 ? 'desc' : 'asc';
    payload.sort = [{ [sorting.field]: { order } }];
  }

  payload = addTaxonomiesAggregation(payload);

  _.forEach(selectedFacets, (values: Array<string>, facet: string) => {
    if (!_.isEmpty(values)) {
      payload = facet == 'PRICE'
        ? addPriceFilter(payload, values)
        : addTaxonomyFilter(payload, facet, values);
    }
  });

  const url = `/search/public/products_catalog_view/_search?size=${toLoad}&from=${from}`;
  return this.api.post(url, payload);
}

export function searchGiftCards() {
  const [sorting, selectedFacets, toLoad] = [null, {}, MAX_RESULTS];
  return apiCall.call(
    { api },
    [GIFT_CARD_TAG],
    sorting,
    selectedFacets,
    toLoad,
    0,
    { ignoreGiftCards: false }
  );
}

function searchRecentlyViewed() {
  const viewed = localStorage.getItem('viewed');
  const viewedArray = JSON.parse(viewed) || [];
  const url = '/search/public/products_catalog_view/_search?size=10';
  let payload = defaultSearch(String(context));

  payload = addTermsFilter(payload, termsFilter('slug', viewedArray));

  return this.api.post(url, payload);
}

const _fetchProducts = createAsyncActions('products', apiCall);
const _fetchRecentlyViewed = createAsyncActions('viewed', searchRecentlyViewed);

export const fetch = _fetchProducts.perform;
export const fetchRecentlyViewed = _fetchRecentlyViewed.perform;

const initialState = {
  list: [],
  facets: [],
  total: 0,
  viewed: [],
};

function determineFacetKind(f: string): string {
  if (f.includes('COLOR')) return 'color';
  else if (f.includes('SIZE')) return 'circle';
  return 'checkbox';
}

function titleCase(t) {
  return _.startCase(_.toLower(t));
}

export const fancyColors = {
  BEIGE: '#BD815D',
  BLACK: '#000000',
  BLUE: '#0B5AB9',
  BROWN: '#6D3D23',
  GREEN: '#3D8458',
  GREY: '#999999',
  METALLIC: '#BCC6CC',
  ORANGE: '#FF5F01',
  PURPLE: '#9D4170',
  YELLOW: '#FFD249',
  PINK: '#EF3C66',
  WHITE: '#FFFFFF',
  'NO COLOR': '#FFFFFF',
};

export function mapFacetValue(v: string, kind: string): string | Object {
  let value = v;
  if (kind == 'color') {
    const color = (v in fancyColors) ? fancyColors[v] : _.toLower(v).replace(/\s/g, '');
    value = { color, value: v };
  }

  return value;
}

function mapAggregationsToFacets(aggregations): Array<Facet> {
  return _.map(aggregations, (a) => {
    const kind = determineFacetKind(a.key);
    const buckets = _.get(a, 'taxon.buckets', []);
    const values = _.uniqBy(_.map(buckets, (t) => {
      return {
        label: titleCase(t.key),
        value: mapFacetValue(t.key, kind),
        count: t.doc_count,
      };
    }), (v) => {
      return kind == 'color' ? v.value.color : v.label;
    });

    return {
      key: a.key,
      name: titleCase(a.key),
      kind,
      values,
    };
  });
}

function priceLabel(from: ?number, to: ?number): string {
  if (from && to) {
    return `$${from / 100} - $${to / 100}`;
  } else if (from) {
    return `$${from / 100}+`;
  } else if (to) {
     return `$0 - $${to / 100}`;
  }

  return '';
}

function mapPriceAggregationsToFacets(response = []): Array<Facet> {
  const aggregations = _.get(response, 'aggregations.priceRanges.buckets', []);

  const prices = aggregations.reduce((acc, priceAgg) => {
    const { doc_count, key, from, to } = priceAgg;

    if (doc_count < 1) {
      return acc;
    }

    return [...acc, {
      count: doc_count,
      label: priceLabel(from, to),
      value: key,
      selected: false,
    }];
  }, []);
 
  if (prices.length == 0) {
    return [];
  }

  return [{
    key: 'PRICE',
    name: 'Price',
    kind: 'price',
    values: prices,
  }];
}

const reducer = createReducer({
  [_fetchProducts.succeeded]: (state, response) => {
    const payloadResult = response.result;
    const aggregations = _.isNil(response.aggregations)
      ? []
      : _.get(response, 'aggregations.taxonomies.taxonomy.buckets', []);
    const list = _.isEmpty(payloadResult) ? [] : payloadResult;
    const total = _.get(response, 'pagination.total', 0);

    const facetsFromAggregations = mapAggregationsToFacets(aggregations);
    const priceFacet = mapPriceAggregationsToFacets(response);

    return {
      ...state,
      list,
      facets: [...facetsFromAggregations, ...priceFacet],
      total,
    };
  },
  [_fetchRecentlyViewed.succeeded]: (state, response) => assoc(state, 'viewed', response.result),
  [resetSearch]: () => initialState,
}, initialState);

export default reducer;
