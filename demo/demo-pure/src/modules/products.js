/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
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
export const PAGE_SIZE = 20;
const context = process.env.STOREFRONT_CONTEXT || 'default';
export const GIFT_CARD_TAG = 'GIFT-CARD';

function apiCall(
  categoryNames: ?Array<string>,
  sorting: ?{ direction: number, field: string },
  selectedFacets: Object,
  loaded: number,
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

  const promise = this.api.post(`/search/public/products_catalog_view/_search?size=${loaded}`, payload);

  const chained = promise.then((response) => {
    return {
      payload: response,
      selectedFacets,
    };
  });
  chained.abort = promise.abort;
  return chained;
}

export function searchGiftCards() {
  return apiCall.call({ api }, [GIFT_CARD_TAG], null, {}, MAX_RESULTS, { ignoreGiftCards: false });
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

const fancyColors = {
  'Adi Blue': 'blue',
  Aluminum: 'lightgrey',
  'Antique Brass': 'gold',
  'Azure Turquoise': 'turquoise',
  BLACK: 'black',
  BLK: 'black',
  'BORANG,WHITE': 'white',
  'Bahia Lt Blue': 'blue',
  'Base Green': 'green',
  Basketball: 'brown',
  Black: 'black',
  'Black Melange': 'black',
  'Black White': 'grey',
  'Blast Emerald': 'emerald',
  'Blaze Orange': 'orange',
  Bliss: 'lightpink',
  'Bliss Coral': 'coral',
  Blue: 'blue',
  'Blue Glow': 'blue',
  'Blue Solid': 'blue',
  Bluebird: 'blue',
  'Bold Blue': 'blue',
  'Bold Onix': 'darkgrey',
  'Bold Orange': 'orange',
  'Bold Pink': 'pink',
  'Bold Red': 'red',
  'Bright Cyan': 'cyan',
  'Bright Green': 'green',
  'Bright Orange': 'orange',
  'Bright Pink': 'pink',
  'Bright Red': 'red',
  'Bright Royal': 'royalblue',
  'Bright Yellow': 'yellow',
  Brown: 'brown',
  Carbon: 'darkgrey',
  'Carbon Metallic': 'darkgrey',
  Cardboard: 'tan',
  Cardinal: 'red',
  'Cargo Brown': 'tan',
  Chalk: 'charcoalsolidgrey',
  'Chalk White': 'white',
  'Charcoal Solid Grey': 'charcoalsolidgrey',
  'Chelsea Blue': 'blue',
  'Chicago Bulls': 'red',
  'Chill Black Melange': 'blackmelange',
  'Chill Ray Red': 'red',
  'Chill Utility Blue': 'blue',
  'Clay Brown': 'lightbrown',
  'Clear Aqua': 'aqua',
  'Clear Blue': 'blue',
  'Clear Brown': 'brown',
  'Clear Brown Melange': 'simplebrown',
  'Clear Grey': 'grey',
  'Clear Light Pink': 'pink',
  'Clear Onix': 'lightgrey',
  'Cleveland Cavaliers': 'darkred',
  Cobalt: 'royalblue',
  'Collegiate Burgundy': 'burgundy',
  'Collegiate Gold': 'gold',
  'Collegiate Green': 'green',
  'Collegiate Navy': 'navy',
  'Collegiate Navy Melange': 'navy',
  'Collegiate Orange': 'orange',
  'Collegiate Purple': 'purple',
  'Collegiate Red': 'red',
  'Collegiate Royal': 'royalblue',
  'Collegiate Royal Melange': 'royalblue',
  'Colored Heather': 'lightbrown',
  'Columbia Blue': 'blue',
  'Core Black': 'black',
  'Core Blue': 'blue',
  'Core Heather': 'lightbrown',
  'Core Pink': 'pink',
  'Core Red': 'red',
  'Core White': 'white',
  'Craft Chili': 'red',
  'Craft Green': 'green',
  'Craft Red': 'red',
  'Cream White': 'white',
  'Crew Yellow': 'yellow',
  'Crystal White': 'white',
  Customized: 'white',
  Cyan: 'cyan',
  'Dark Brown': 'darkbrown',
  'Dark Burgundy': 'darkred',
  'Dark Green': 'darkgreen',
  'Dark Grey Heather': 'brown',
  'Dark Indigo': 'indigo',
  'Dark Marine': 'darkmarine',
  'Dark Navy': 'darknavy',
  'Dark Onix': 'darkgrey',
  'Dark Purple': 'darkpurple',
  'Dark Red': 'darkred',
  'Dark Shale': 'darkgrey',
  'Dc Red': 'red',
  'Deep Sea': 'darkblue',
  'Deepest Space': 'black',
  'Dusk Pink': 'lightpink',
  'Dust Purple': 'lightpurple',
  'Earth Green': 'green',
  'Easy Blue': 'lightblue',
  'Easy Coral': 'lightcoral',
  'Easy Green': 'lightgreen',
  'Easy Mint': 'mintcream',
  'Easy Orange': 'lightorange',
  'Easy Pink': 'lightpink',
  Ecru: 'ecru',
  Electricity: 'yellow',
  Energy: 'orange',
  'Energy Blue': 'navy',
  'Energy Green': 'darkgreen',
  'Eqt Orange': 'orange',
  'Eqt Pink': 'pink',
  'Eqt Yellow': 'yellow',
  'Fcb True Red': 'red',
  'Flash Red': 'red',
  'Glow Orange': 'orange',
  Gold: 'gold',
  'Gold Metallic': 'goldmetallic',
  'Gold Solid': 'gold',
  'Golden State Warriors': 'gorl',
  Granite: 'grey',
  Green: 'green',
  Grey: 'grey',
  Gunmetal: 'metalicsilver',
  'Haze Coral': 'coral',
  Hemp: 'brown',
  'Houston Rockets': 'red',
  'Hyper Green': 'green',
  'Ice Green': 'lightgreen',
  'Ice Grey': 'lightgrey',
  'Ice Mint': 'lightaquea',
  'Ice Purple': 'lightpurple',
  'Ice Yellow': 'lightyellow',
  Infrared: 'red',
  'Intense Blue': 'blue',
  'La Lakers': 'purple',
  Lead: 'black',
  'Legacy White': 'antiquewhite',
  'Legend Ink': 'black',
  'Lemon Peel': 'yellow',
  'Light Aqua': 'aqua',
  'Light Blue': 'lightblue',
  'Light Brown': 'lightbrown',
  'Light Granite': 'lightgrey',
  'Light Grey': 'lightgrey',
  'Light Grey Heather': 'lightgrey',
  'Light Maroon': 'maroon',
  'Light Onix': 'lightgrey',
  'Light Orange': 'orange',
  'Light Orchid': 'lightpurple',
  'Light Pink': 'lightpink',
  'Light Red': 'lightred',
  'Light Scarlet': 'lightred',
  'Light Sky': 'lightblue',
  'Light Solid Grey': 'grey',
  Linen: 'linen',
  'Linen Green': 'linengreen',
  'Linen Khaki': 'khaki',
  Maroon: 'maroon',
  'Master Blue': 'blue',
  'Matte Gold': 'gold',
  'Matte Silver': 'silver',
  'Medium Grey': 'grey',
  'Medium Grey Heather': 'grey',
  'Medium Lead': 'black',
  Mesa: 'brown',
  'Metallic Gold': 'gold',
  'Metallic Silver': 'silver',
  'Miami Heat': 'darkvioletred',
  'Mid Grey': 'grey',
  'Midnight Grey': 'grey',
  'Mineral Blue': 'blue',
  Mint: 'mintcream',
  'Multi Solid Grey': 'grey',
  'Mustang Brown': 'lightbrown',
  'Mystery Blue': 'lightblue',
  'Mystery Green': 'lightgreen',
  'Mystery Red': 'lightred',
  'Natural Khaki': 'khaki',
  'Neo Iron': 'silver',
  'Neon Blue': 'blue',
  'Neon Green': 'green',
  'Neon Orange': 'orange',
  'Neon Yellow': 'yellow',
  'New Navy': 'navy',
  'New York Knicks': 'orangered',
  Night: 'darkgrey',
  'Night Cargo': 'khaki',
  'Night Grey': 'darkgrey',
  'Night Indigo': 'indigo',
  'Night Marine': 'darkmarine',
  'Night Navy': 'darknavy',
  'Night Red': 'darkred',
  'Night Sky': 'darkblue',
  'Noble Ink': 'black',
  Ocean: 'lightblue',
  'Off White': 'offwhite',
  'Olive Cargo': 'olivedrab',
  Onix: 'grey',
  Orange: 'orange',
  'Pearl Grey': 'lightgrey',
  'Pink Buzz': 'pink',
  'Platinum Metallic': 'silver',
  'Portland Timbers': 'green',
  'Portland Trail Blazers': 'burgundy',
  'Powder Blue': 'blue',
  'Power Purple': 'purple',
  'Power Red': 'red',
  Purple: 'purple',
  'Radiant Gold': 'gold',
  'Rave Green': 'green',
  'Raw Lime': 'lime',
  'Raw Purple': 'purple',
  'Ray Blue': 'blue',
  'Ray Pink': 'pink',
  'Ray Purple': 'purple',
  'Ray Red': 'red',
  'Real Red': 'red',
  Red: 'red',
  'Red Bulls': 'red',
  'Red Solid': 'red',
  Reflective: 'silver',
  'Regal Purple': 'purple',
  Royal: 'royalblue',
  'Running White': 'white',
  'Running White Ftw': 'white',
  'Samba Blue': 'blue',
  'San Antonio Spurs': 'black',
  Scarlet: 'red',
  'Semi Night Flash': 'black',
  'Semi Solar Green': 'green',
  'Semi Solar Slime': 'green',
  Sesame: 'tan',
  'Sharp Grey': 'grey',
  'Shock Blue': 'blue',
  'Shock Pink': 'pink',
  'Shock Pink,Black': 'darkpink',
  'Shock Purple': 'darkpurple',
  'Shock Red': 'darkred',
  'Shock Slime': 'darkyellow',
  Silver: 'silver',
  'Silver Metallic': 'silver',
  'Simple Brown': 'brown',
  'Smoke Blue': 'blue',
  'Soft Powder': 'white',
  'Solar Blue': 'blue',
  'Solar Gold': 'gold',
  'Solar Green': 'green',
  'Solar Green Melange': 'green',
  'Solar Lime': 'lime',
  'Solar Orange': 'orange',
  'Solar Pink': 'pink',
  'Solar Red': 'red',
  'Solar Yellow': 'yellow',
  'Solid Grey': 'grey',
  'St Nomad Yellow': 'yellow',
  'St Pale Nude': 'tan',
  Stone: 'brown',
  'Sub Green': 'green',
  Sun: 'yellow',
  'Sun Glow': 'yellow',
  Sunshine: 'yellow',
  'Super Blush': 'lightpink',
  'Super Purple': 'purple',
  'Tactile Blue': 'blue',
  'Tactile Green': 'green',
  'Tactile Orange': 'orange',
  'Tactile Pink': 'pink',
  'Tech Earth': 'brown',
  'Tech Green': 'green',
  'Tech Grey Metallic': 'silver',
  'Tech Ink': 'black',
  'Tech Steel': 'silver',
  Timber: 'tan',
  'Trace Brown': 'brown',
  'Trace Cargo': 'khaki',
  'Trace Green': 'green',
  'Trace Grey': 'grey',
  'True Blue': 'blue',
  'Twilight Green': 'green',
  'Uniform Blue': 'blue',
  'Unity Blue': 'blue',
  'Unity Ink': 'black',
  'Unity Lime': 'lime',
  'Unity Orange': 'orange',
  'Unity Purple': 'purple',
  'University Red': 'red',
  'Utility Black': 'black',
  'Utility Blue': 'blue',
  'Utility Grey': 'grey',
  'Utility Ivy': 'lightgreen',
  'Vapour Green': 'lightgreen',
  'Vapour Grey': 'lightgrey',
  'Victory Red': 'lightred',
  'Vintage White': 'antiquewhite',
  'Vista Grey': 'grey',
  'Vivid Blue': 'blue',
  'Vivid Red': 'red',
  'Vivid Yellow': 'yellow',
  White: 'white',
  'White Melange': 'white',
  Yellow: 'yellow',
  'Yellow Cab': 'yellow',
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
  [_fetchProducts.succeeded]: (state, action) => {
    const {payload, selectedFacets} = action;
    const payloadResult = payload.result;
    const aggregations = _.isNil(payload.aggregations)
      ? []
      : _.get(payload, 'aggregations.taxonomies.taxonomy.buckets', []);
    const list = _.isEmpty(payloadResult) ? [] : payloadResult;
    const total = _.get(payload, 'pagination.total', 0);

    const queryFacets = mapAggregationsToFacets(aggregations);

    let facets = [];

    // The only time this should be empty is on first call.
    if (_.isEmpty(state.facets)) {
      facets = queryFacets;
    } else {
      // Merge aggregations from quiries into existing state.
      // Keep existinged selected facets and only change unselected ones..
      // This avoids quiries that would return empty results.
      // While also keeping the interface from changing too much.
      const groupedQueyFacets = _.groupBy(queryFacets, 'key');

      facets = _.compact(_.map(state.facets, (v) => {
        if (!_.isEmpty(selectedFacets[v.key])) {
          return v;
        }

        return _.isArray(groupedQueyFacets[v.key]) ? groupedQueyFacets[v.key][0] : null;
      }));
    }

    return {
      ...state,
      list,
      facets,
      total,
    };
  },
}, initialState);

export default reducer;
