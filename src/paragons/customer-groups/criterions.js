import types from './types';
import ops from './operators';
import widgets from '../../components/customers-groups/dynamic/widgets';


const criterions = [
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
      [ops.oneOf]: widgets.oneOf(widgets.plain('text')),
      [ops.notOneOf]: widgets.oneOf(widgets.plain('text')),
    },
    field: 'name',
    label: 'Name',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
      [ops.oneOf]: widgets.oneOf(widgets.plain('text')),
      [ops.notOneOf]: widgets.oneOf(widgets.plain('text')),
    },
    field: 'email',
    label: 'Email',
  },
  {
    type: types.number,
    widget: {
      default: widgets.currency,
      [ops.between]: widgets.range(widgets.currency),
    },
    field: 'revenue',
    label: 'Total Sales',
  },
  {
    type: types.enum,
    widget: {
      default: widgets.dropdown,
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
    widget: {
      default: widgets.dropdown,
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
    widget: {
      default: widgets.plain('text'),
      [ops.equal]: widgets.lookup('state'),
      [ops.notEqual]: widgets.lookup('state'),
      [ops.oneOf]: widgets.oneOf(widgets.lookup('state')),
      [ops.notOneOf]: widgets.oneOf(widgets.lookup('state')),
      config: {
        storePath: 'groups.shippingState',
      },
    },
    field: 'shippingAddresses.region',
    label: 'Shipping State',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
      [ops.equal]: widgets.lookup('state'),
      [ops.notEqual]: widgets.lookup('state'),
      [ops.oneOf]: widgets.oneOf(widgets.lookup('state')),
      [ops.notOneOf]: widgets.oneOf(widgets.lookup('state')),
      config: {
        storePath: 'groups.billingState',
      },
    },
    field: 'billingAddresses.region',
    label: 'Billing State',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
      [ops.equal]: widgets.lookup('city'),
      [ops.notEqual]: widgets.lookup('city'),
      [ops.oneOf]: widgets.oneOf(widgets.lookup('city')),
      [ops.notOneOf]: widgets.oneOf(widgets.lookup('city')),
      config: {
        storePath: 'groups.shippingCity',
      },
    },
    field: 'shippingAddresses.city',
    label: 'Shipping City',
  },
  {
    type: types.string,
    widget: {
      default: widgets.plain('text'),
      [ops.equal]: widgets.lookup('city'),
      [ops.notEqual]: widgets.lookup('city'),
      [ops.oneOf]: widgets.oneOf(widgets.lookup('city')),
      [ops.notOneOf]: widgets.oneOf(widgets.lookup('city')),
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
    widget: {
      default: widgets.plain('number'),
      [ops.oneOf]: widgets.oneOf(widgets.plain('number')),
      [ops.notOneOf]: widgets.oneOf(widgets.plain('number')),
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
    widget: {
      default: widgets.plain('number'),
      [ops.oneOf]: widgets.oneOf(widgets.plain('number')),
      [ops.notOneOf]: widgets.oneOf(widgets.plain('number')),
    },
    field: 'billingAddresses.zip',
    label: 'Billing Zip',
  },
  {
    type: types.date,
    widget: {
      default: widgets.date,
      [ops.oneOf]: widgets.oneOf(widgets.date),
      [ops.notOneOf]: widgets.oneOf(widgets.date),
      [ops.between]: widgets.range(widgets.date),
    },
    field: 'joinedAt',
    label: 'Date Joined',
  },
];

export default criterions;
