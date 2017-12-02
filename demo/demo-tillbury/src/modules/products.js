/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcommerce/wings';
import {
  addTaxonomyFilter,
  addTaxonomiesAggregation,
  addMustNotFilter, defaultSearch, termFilter, addCategoryFilter, addTermFilter,
} from 'lib/elastic';
import _ from 'lodash';
import { api } from 'lib/api';

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

function apiCall(
  categoryNames: ?Array<string>,
  sorting: ?{ direction: number, field: string },
  selectedFacets: Object,
  toLoad: number,
  from: number = 0,
  { ignoreGiftCards = true } = {}): Promise<*> {
  let payload = defaultSearch(String(context));

  _.forEach(_.compact(categoryNames), (cat) => {
    if (cat !== 'ALL' && cat !== GIFT_CARD_TAG) {
      payload = addCategoryFilter(payload, cat.toUpperCase());
    } else if (cat === GIFT_CARD_TAG) {
      const tagTerm = termFilter('tags', cat.toUpperCase());
      payload = addTermFilter(payload, tagTerm);
    }
  });

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
      payload = addTaxonomyFilter(payload, facet, values);
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

const _fetchProducts = createAsyncActions('products', apiCall);
export const fetch = _fetchProducts.perform;

const initialState = {
  list: [],
  facets: [],
  total: 0,
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

export function mapFacetValue(v: string, kind: string): string|Object {
  let value = v;
  if (kind == 'color') {
    const color = (v in fancyColors) ? fancyColors[v] : _.toLower(v).replace(/\s/g, '');
    value = {color, value: v};
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
    }), (v) => { return kind == 'color' ? v.value.color : v.label; });

    return {
      key: a.key,
      name: titleCase(a.key),
      kind,
      values,
    };
  });
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

    return {
      ...state,
      list,
      facets: facetsFromAggregations,
      total,
    };
  },
}, initialState);

export default reducer;
