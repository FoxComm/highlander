import types from '../../paragons/customer-groups/types';
import {
  equal,
  notEqual,
  oneOf,
  notOneOf,
  match,
  notMatch,
  greater,
  less,
  between,
} from './operators';

const criterions = [
  {
    type: types.string,
    input: {
      default: 'text',
      [oneOf]: 'textOneOf',
      [notOneOf]: 'textOneOf',
    },
    field: 'name',
    label: 'Name',
  },
  {
    type: types.string,
    input: {
      default: 'text',
      [oneOf]: 'textOneOf',
      [notOneOf]: 'textOneOf',
    },
    field: 'email',
    label: 'Email',
  },
  {
    type: types.number,
    input: {
      default: 'currency',
      [oneOf]: 'currencyOneOf',
      [notOneOf]: 'currencyOneOf',
      [between]: 'currencyRange',
    },
    field: 'revenue',
    label: 'Total Sales',
  },
  {
    type: types.enum,
    input: {
      default: 'choice',
      config: {
        choices: [
          [false, 'active'],
          [true, 'inactive'],
        ],
      }
    },
    operators: [
      equal,
      notEqual,
    ],
    field: 'isDisabled',
    label: 'Account Activitity Status',
  },
  {
    type: types.enum,
    input: {
      default: 'choice',
      config: {
        choices: [
          [true, 'on blacklist'],
          [false, 'not on blacklist'],
        ],
      }
    },
    operators: [
      equal,
      notEqual,
    ],
    field: 'isBlacklisted',
    label: 'Account Blacklist Status',
  },
  {
    type: types.string,
    input: {
      default: 'text',
      [equal]: 'stateLookup',
      [notEqual]: 'stateLookup',
      [oneOf]: 'stateLookupOneOf',
      [notOneOf]: 'stateLookupOneOf',
      config: {
        storePath: 'groups.shippingState',
      },
    },
    field: 'shippingAddresses.region',
    label: 'Shipping State',
  },
  {
    type: types.string,
    input: {
      default: 'text',
      [equal]: 'stateLookup',
      [notEqual]: 'stateLookup',
      [oneOf]: 'stateLookupOneOf',
      [notOneOf]: 'stateLookupOneOf',
      config: {
        storePath: 'groups.billingState',
      },
    },
    field: 'billingAddresses.region',
    label: 'Billing State',
  },
  {
    type: types.string,
    input: {
      default: 'text',
      [equal]: 'cityLookup',
      [notEqual]: 'cityLookup',
      [oneOf]: 'cityLookupOneOf',
      [notOneOf]: 'cityLookupOneOf',
      config: {
        storePath: 'groups.shippingCity',
      },
    },
    field: 'shippingAddresses.city',
    label: 'Shipping City',
  },
  {
    type: types.string,
    input: {
      default: 'text',
      [equal]: 'cityLookup',
      [notEqual]: 'cityLookup',
      [oneOf]: 'cityLookupOneOf',
      [notOneOf]: 'cityLookupOneOf',
      config: {
        storePath: 'groups.billingCity',
      },
    },
    field: 'billingAddresses.city',
    label: 'Billing City',
  },
  {
    type: types.number,
    operators: [
      equal,
      notEqual,
      oneOf,
      notOneOf,
      match,
      notMatch,
    ],
    input: {
      default: 'number',
      [oneOf]: 'numberOneOf',
      [notOneOf]: 'numberOneOf',
    },
    field: 'shippingAddresses.zip',
    label: 'Shipping Zip',
  },
  {
    type: types.number,
    operators: [
      equal,
      notEqual,
      oneOf,
      notOneOf,
      match,
      notMatch,
    ],
    input: {
      default: 'number',
      [oneOf]: 'numberOneOf',
      [notOneOf]: 'numberOneOf',
    },
    field: 'billingAddresses.zip',
    label: 'Billing Zip',
  },
  {
    type: types.date,
    input: {
      default: 'date',
      [oneOf]: 'dateOneOf',
      [notOneOf]: 'dateOneOf',
      [between]: 'dateRange',
    },
    field: 'joinedAt',
    label: 'Date Joined',
  },
];

export default criterions;
