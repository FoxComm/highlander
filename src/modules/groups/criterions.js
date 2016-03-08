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
} from './../../paragons/customer-groups/operators';

const criterions = [
  {
    type: types.string,
    input: {
      type: 'plain'
    },
    field: 'name',
    label: 'Name',
  },
  {
    type: types.string,
    input: {
      type: 'plain'
    },
    field: 'email',
    label: 'Email',
  },
  {
    type: types.number,
    input: {
      type: 'currency'
    },
    field: 'revenue',
    label: 'Total Sales',
  },
  {
    type: types.enum,
    input: {
      type: 'dropdown',
    },
    field: 'isDisabled',
    label: 'Account Activitity Status',
    choices: [
      [false, 'active'],
      [true, 'inactive'],
    ],
  },
  {
    type: types.enum,
    input: {
      type: 'dropdown',
    },
    field: 'isBlacklisted',
    label: 'Account Blacklist Status',
    choices: [
      [true, 'on blacklist'],
      [false, 'not on blacklist'],
    ],
  },
  {
    type: types.string,
    input: {
      type: 'stateLookup',
      storePath: 'groups.shippingState',
    },
    field: 'shippingAddresses.region',
    label: 'Shipping State',
  },
  {
    type: types.string,
    input: {
      type: 'stateLookup',
      storePath: 'groups.billingState',
    },
    field: 'billingAddresses.region',
    label: 'Billing State',
  },
  {
    type: types.string,
    input: {
      type: 'cityLookup',
      storePath: 'groups.shippingCity',
    },
    field: 'shippingAddresses.city',
    label: 'Shipping City',
  },
  {
    type: types.string,
    input: {
      type: 'cityLookup',
      storePath: 'groups.billingCity',
    },
    field: 'billingAddresses.city',
    label: 'Billing City',
  },
  {
    type: types.number,
    operators: {
      equal,
      notEqual,
      oneOf,
      notOneOf,
      match,
      notMatch,
    },
    input: {
      type: 'plain'
    },
    field: 'shippingAddresses.zip',
    label: 'Shipping Zip',
  },
  {
    type: types.number,
    operators: {
      equal,
      notEqual,
      oneOf,
      notOneOf,
      match,
      notMatch,
    },
    input: {
      type: 'plain'
    },
    field: 'billingAddresses.zip',
    label: 'Billing Zip',
  },
  {
    type: types.date,
    input: {
      type: 'dateRange'
    },
    field: 'joinedAt',
    label: 'Date Joined',
  },
];

export default criterions;
