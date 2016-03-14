import types from './types';
import ops from './operators';
import inputs from '../../components/customers-groups/inputs';


const criterions = [
  {
    type: types.string,
    input: {
      default: inputs.plain,
      [ops.oneOf]: inputs.oneOf(inputs.plain),
      [ops.notOneOf]: inputs.oneOf(inputs.plain),
    },
    field: 'name',
    label: 'Name',
  },
  {
    type: types.string,
    input: {
      default: inputs.plain,
      [ops.oneOf]: inputs.plain,
      [ops.notOneOf]: inputs.plain,
    },
    field: 'email',
    label: 'Email',
  },
  {
    type: types.number,
    input: {
      default: 'currency',
      [ops.oneOf]: 'currencyOneOf',
      [ops.notOneOf]: 'currencyOneOf',
      [ops.between]: 'currencyRange',
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
      ops.equal,
      ops.notEqual,
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
      ops.equal,
      ops.notEqual,
    ],
    field: 'isBlacklisted',
    label: 'Account Blacklist Status',
  },
  {
    type: types.string,
    input: {
      default: 'text',
      [ops.equal]: 'stateLookup',
      [ops.notEqual]: 'stateLookup',
      [ops.oneOf]: 'stateLookupOneOf',
      [ops.notOneOf]: 'stateLookupOneOf',
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
      [ops.equal]: 'stateLookup',
      [ops.notEqual]: 'stateLookup',
      [ops.oneOf]: 'stateLookupOneOf',
      [ops.notOneOf]: 'stateLookupOneOf',
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
      [ops.equal]: 'cityLookup',
      [ops.notEqual]: 'cityLookup',
      [ops.oneOf]: 'cityLookupOneOf',
      [ops.notOneOf]: 'cityLookupOneOf',
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
      [ops.equal]: 'cityLookup',
      [ops.notEqual]: 'cityLookup',
      [ops.oneOf]: 'cityLookupOneOf',
      [ops.notOneOf]: 'cityLookupOneOf',
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
      ops.equal,
      ops.notEqual,
      ops.oneOf,
      ops.notOneOf,
      ops.match,
      ops.notMatch,
    ],
    input: {
      default: 'number',
      [ops.oneOf]: 'numberOneOf',
      [ops.notOneOf]: 'numberOneOf',
    },
    field: 'shippingAddresses.zip',
    label: 'Shipping Zip',
  },
  {
    type: types.number,
    operators: [
      ops.equal,
      ops.notEqual,
      ops.oneOf,
      ops.notOneOf,
      ops.match,
      ops.notMatch,
    ],
    input: {
      default: 'number',
      [ops.oneOf]: 'numberOneOf',
      [ops.notOneOf]: 'numberOneOf',
    },
    field: 'billingAddresses.zip',
    label: 'Billing Zip',
  },
  {
    type: types.date,
    input: {
      default: 'date',
      [ops.oneOf]: 'dateOneOf',
      [ops.notOneOf]: 'dateOneOf',
      [ops.between]: 'dateRange',
    },
    field: 'joinedAt',
    label: 'Date Joined',
  },
];

export default criterions;
