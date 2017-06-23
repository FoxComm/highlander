
import React from 'react';
import WidgetContainer from './widgets/widget-container';

const qualifiers = [
  {
    type: 'orderAny',
    title: 'Order - No qualifier',
    content: [
      [{type: 'type'}]
    ]
  },
  {
    type: 'orderTotalAmount',
    title: 'Order - Total amount of order',
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
    content: [
      [{type: 'type'}],
      [
        {
          name: 'search',
          widget: 'selectProducts',
          label: 'Items for qualify'
        }
      ]
    ]
  },
  {
    type: 'itemsTotalAmount',
    title: 'Items - Total amount of order',
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
          label: 'Items for qualify'
        }
      ]
    ]
  },
  {
    type: 'itemsNumUnits',
    title: 'Items - Number of units in order',
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
          label: 'Items for qualify'
        }
      ]
    ]
  },
];

export default qualifiers;
