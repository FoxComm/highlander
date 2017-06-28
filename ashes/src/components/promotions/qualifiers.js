
import React from 'react';
import WidgetContainer from './widgets/widget-container';

const qualifiers = [
  {
    type: 'orderAny',
    title: 'Order - No qualifier',
    content: [
      [{type: 'type'}],
    ]
  },
  {
    type: 'orderTotalAmount',
    title: 'Order - Total amount of order',
    default: {totalAmount: 0},
    validate: {
      totalAmount: {
        validate: (v) => v > 0,
        error: 'amount input is 0',
      },
    },
    content: [
      [
        {type: 'type'},
        {
          name: 'totalAmount',
          widget: 'currency',
          template: props => <WidgetContainer>Spend {props.children} or more.</WidgetContainer>
        }
      ]
    ]
  },
  {
    type: 'orderNumUnits',
    title: 'Order - Number of units in order',
    default: {numUnits: 0},
    validate: {
      numUnits: {
        validate: (v) => v > 0,
        error: 'number of units input is 0',
      },
    },
    content: [
      [
        {type: 'type'},
        {
          name: 'numUnits',
          widget: 'counter',
          template: props => <WidgetContainer>Order {props.children} items or more.</WidgetContainer>
        }
      ]
    ]
  },
  {
    type: 'itemsAny',
    title: 'Items - No qualifier',
    default: {search: []},
    validate: {
      search: {
        validate: (v) => v.length > 0,
        error: 'select products saved search is empty',
      },
    },
    content: [
      [{type: 'type'}],
      [
        {
          name: 'search',
          widget: 'selectProducts',
          label: 'Qualifying items'
        }
      ]
    ]
  },
  {
    type: 'itemsTotalAmount',
    title: 'Items - Total amount of order',
    default: {totalAmount: 0, search: []},
    validate: {
      totalAmount: {
        validate: (v) => v > 0,
        error: 'amount input is 0',
      },
      search: {
        validate: (v) => v.length > 0,
        error: 'select products saved search is empty',
      },
    },
    content: [
      [
        {type: 'type'},
        {
          name: 'totalAmount',
          widget: 'currency',
          template: props => <WidgetContainer>Spend {props.children} or more.</WidgetContainer>
        }
      ],
      [
        {
          name: 'search',
          widget: 'selectProducts',
          label: 'Qualifying items'
        }
      ]
    ]
  },
  {
    type: 'itemsNumUnits',
    title: 'Items - Number of units in order',
    default: {numUnits: 0, search: []},
    validate: {
      numUnits: {
        validate: (v) => v > 0,
        error: 'number of units input is 0',
      },
      search: {
        validate: (v) => v.length > 0,
        error: 'select products saved search is empty',
      },
    },
    content: [
      [
        {type: 'type'},
        {
          name: 'numUnits',
          widget: 'counter',
          template: props => <WidgetContainer>Order {props.children} items or more.</WidgetContainer>
        }
      ],
      [
        {
          name: 'search',
          widget: 'selectProducts',
          label: 'Qualifying items'
        }
      ]
    ]
  },
];

export default qualifiers;
