import types from './types';
import ops from './operators';
import inputs from '../../components/customers-groups/dynamic/inputs';


const criterions = [
  {
    type: types.string,
    input: {
      default: inputs.plain('text'),
      [ops.oneOf]: inputs.oneOf(inputs.plain('text')),
      [ops.notOneOf]: inputs.oneOf(inputs.plain('text')),
    },
    field: 'name',
    label: 'Name',
  },
  {
    type: types.string,
    input: {
      default: inputs.plain('text'),
      [ops.oneOf]: inputs.oneOf(inputs.plain('text')),
      [ops.notOneOf]: inputs.oneOf(inputs.plain('text')),
    },
    field: 'email',
    label: 'Email',
  },
  {
    type: types.number,
    input: {
      default: inputs.currency,
      [ops.between]: inputs.range(inputs.currency),
    },
    field: 'revenue',
    label: 'Total Sales',
  },
  {
    type: types.enum,
    input: {
      default: inputs.dropdown,
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
      default: inputs.dropdown,
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
      default: inputs.plain('text'),
      [ops.equal]: inputs.lookup('state'),
      [ops.notEqual]: inputs.lookup('state'),
      [ops.oneOf]: inputs.oneOf(inputs.lookup('state')),
      [ops.notOneOf]: inputs.oneOf(inputs.lookup('state')),
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
      default: inputs.plain('text'),
      [ops.equal]: inputs.lookup('state'),
      [ops.notEqual]: inputs.lookup('state'),
      [ops.oneOf]: inputs.oneOf(inputs.lookup('state')),
      [ops.notOneOf]: inputs.oneOf(inputs.lookup('state')),
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
      default: inputs.plain('text'),
      [ops.equal]: inputs.lookup('city'),
      [ops.notEqual]: inputs.lookup('city'),
      [ops.oneOf]: inputs.oneOf(inputs.lookup('city')),
      [ops.notOneOf]: inputs.oneOf(inputs.lookup('city')),
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
      default: inputs.plain('text'),
      [ops.equal]: inputs.lookup('city'),
      [ops.notEqual]: inputs.lookup('city'),
      [ops.oneOf]: inputs.oneOf(inputs.lookup('city')),
      [ops.notOneOf]: inputs.oneOf(inputs.lookup('city')),
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
      default: inputs.plain('number'),
      [ops.oneOf]: inputs.oneOf(inputs.plain('number')),
      [ops.notOneOf]: inputs.oneOf(inputs.plain('number')),
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
      default: inputs.plain('number'),
      [ops.oneOf]: inputs.oneOf(inputs.plain('number')),
      [ops.notOneOf]: inputs.oneOf(inputs.plain('number')),
    },
    field: 'billingAddresses.zip',
    label: 'Billing Zip',
  },
  {
    type: types.date,
    input: {
      default: inputs.date,
      [ops.oneOf]: inputs.oneOf(inputs.date),
      [ops.notOneOf]: inputs.oneOf(inputs.date),
      [ops.between]: inputs.range(inputs.date),
    },
    field: 'joinedAt',
    label: 'Date Joined',
  },
];

export default criterions;
