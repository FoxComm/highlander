
import React from 'react';
import WidgetContainer from './widgets/widget-container';

const offers = [
  {
    type: 'orderPercentOff',
    title: 'Percent off order',
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          widget: 'percent',
          template: props => <WidgetContainer>Get {props.children} off your order.</WidgetContainer>
        }
      ]
    ]
  },
  {
    type: 'orderAmountOff',
    title: 'Amount off order',
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          widget: 'currency',
          template: props => <WidgetContainer>Get {props.children} off your order.</WidgetContainer>
        }
      ]
    ]
  },
  {
    type: 'itemPercentOff',
    title: 'Percent off single item',
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          widget: 'percent',
          template: props => <WidgetContainer>Get {props.children} off discounted item.</WidgetContainer>
        }
      ],
      [
        {
          name: 'search',
          widget: 'selectProduct',
          label: 'Discount the item'
        }
      ]
    ]
  },
  {
    type: 'itemAmountOff',
    title: 'Amount off single item',
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          widget: 'currency',
          template: props => <WidgetContainer>Get {props.children} off discounted item.</WidgetContainer>
        }
      ],
      [
        {
          name: 'search',
          widget: 'selectProduct',
          label: 'Discount the items'
        }
      ]
    ]
  },
  {
    type: 'itemsPercentOff',
    title: 'Percent off select items',
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          widget: 'percent',
          template: props => <WidgetContainer>Get {props.children} off discounted items.</WidgetContainer>
        }
      ],
      [
        {
          name: 'search',
          widget: 'selectProducts',
          label: 'Discount the items'
        }
      ]
    ]
  },
  {
    type: 'itemsAmountOff',
    title: 'Amount off select items',
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          widget: 'currency',
          template: props => <WidgetContainer>Get {props.children} off discounted items.</WidgetContainer>
        }
      ],
      [
        {
          name: 'search',
          widget: 'selectProducts',
          label: 'Discount the items'
        }
      ]
    ]
  },
  {
    type: 'freeShipping',
    title: 'Free Shipping',
    content: [
      [{type: 'type'}]
    ]
  },
  {
    type: 'discountedShipping',
    title: 'Discounted shipping',
    content: [
      [
        {type: 'type'},
        {
          name: 'setPrice',
          widget: 'currency',
          template: props => <WidgetContainer>Get {props.children} off shipping.</WidgetContainer>
        }
      ]
    ]
  },
  {
    type: 'setPrice',
    title: 'Set price',
    content: [
      [
        {type: 'type'},
        {
          name: 'setPrice',
          widget: 'currency',
          template: props => <WidgetContainer>Set price to {props.children}</WidgetContainer>
        }
      ]
    ]
  },
];

export default offers;
