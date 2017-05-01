//libs
import _ from 'lodash';

//paragons
import types from './types';
import ops from './operators';

//components
import widgets from '../../components/query-builder/widgets';

//modules
import * as suggestProducts from 'modules/products/suggest';
import * as suggestTaxonomies from 'modules/taxonomies/suggest';

const suggestProductsItems = suggestProducts.suggestItems;
const suggestTaxonomiesItems = suggestTaxonomies.suggestItems;

export const limitedCriterions = [
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
      [ops.equal]: widgets.typeahead('products.suggest.products',
        'productSuggest.inProgress',{ suggestItems: suggestProductsItems }),
    },
    operators: [
      ops.equal,
    ],
    field: 'product-title',
    label: 'Product Title',
  },
];

const criterions = [
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
      [ops.equal]: widgets.typeahead('products.suggest.products',
        'productSuggest.inProgress',{ suggestItems: suggestProductsItems }),
      [ops.notEqual]: widgets.typeahead('products.suggest.products',
        'productSuggest.inProgress',{ suggestItems: suggestProductsItems }),
      [ops.oneOf]: widgets.oneOf(widgets.typeahead('products.suggest.products',
        'productSuggest.inProgress',{ suggestItems: suggestProductsItems })),
      [ops.notOneOf]: widgets.oneOf(widgets.typeahead('products.suggest.products',
        'productSuggest.inProgress',{ suggestItems: suggestProductsItems })),
    },
    field: 'product-title',
    label: 'Product Title',
  },
  {
    type: types.number,
    widget: {
      default: widgets.plain('number'),
      [ops.oneOf]: widgets.oneOf(widgets.plain('number')),
      [ops.notOneOf]: widgets.oneOf(widgets.plain('number')),
    },
    operators: [
      ops.equal,
      ops.notEqual,
      ops.oneOf,
      ops.notOneOf,
    ],
    field: 'product-id',
    label: 'Product ID',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
      [ops.equal]: widgets.plain('text'),
      [ops.notEqual]: widgets.plain('text'),
      [ops.oneOf]: widgets.oneOf(widgets.plain('text')),
      [ops.notOneOf]: widgets.oneOf(widgets.plain('text')),
    },
    operators: [
      ops.equal,
      ops.notEqual,
      ops.oneOf,
      ops.notOneOf,
    ],
    field: 'tag',
    label: 'Tag',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
    },
    field: 'category',
    label: 'Category',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
      [ops.equal]: widgets.typeahead('taxonomies.suggest.taxonomies',
        'taxonomySuggest.inProgress',{suggestItems: suggestTaxonomiesItems}),
      [ops.notEqual]: widgets.typeahead('taxonomies.suggest.taxonomies',
        'taxonomySuggest.inProgress',{suggestItems: suggestTaxonomiesItems}),
      [ops.oneOf]: widgets.oneOf(widgets.typeahead('taxonomies.suggest.taxonomies',
        'taxonomySuggest.inProgress',{suggestItems: suggestTaxonomiesItems})),
      [ops.notOneOf]: widgets.oneOf(widgets.typeahead('taxonomies.suggest.taxonomies',
        'taxonomySuggest.inProgress',{suggestItems: suggestTaxonomiesItems})),
    },
    field: 'taxonomy-value',
    label: 'Taxonomy Value',
  },
  {
    type: types.number,
    widget: {
      default: widgets.currency,
      [ops.between]: widgets.range(widgets.currency),
    },
    field: 'price',
    label: 'Price',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
    },
    field: 'sale-status',
    label: 'Sale Status',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
    },
    field: 'option-value',
    label: 'Option Value ',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
    },
    field: 'variant-title',
    label: 'Variant Title',
  },
];

export const getCriterion = field => _.find(criterions, {field: field});

export const getLimitedCriterion = field => _.find(limitedCriterions, {field: field});

export const getOperators = ({operators, type}) => operators ? _.pick(type.operators, operators) : type.operators;

export const getWidget = (criterion, operator) => _.get(criterion.widget, operator, criterion.widget.default);

export default criterions;
