
import React from 'react';
import WidgetContainer from './widgets/widget-container';

const defaultDiscount = { discount: 0 };

const offers = [
  {
    type: 'orderPercentOff',
    title: 'Percent off order',
    default: defaultDiscount,
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          value: 0,
          widget: 'percent',
          template: props => <WidgetContainer>Get {props.children} off your order.</WidgetContainer>
        }
      ]
    ]
  },
  {
    type: 'orderAmountOff',
    title: 'Amount off order',
    default: defaultDiscount,
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          value: 0,
          widget: 'currency',
          template: props => <WidgetContainer>Get {props.children} off your order.</WidgetContainer>
        }
      ]
    ]
  },
  {
    type: 'itemPercentOff',
    title: 'Percent off single item',
    default: defaultDiscount,
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          value: 0,
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
    default: defaultDiscount,
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          value: 0,
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
    default: defaultDiscount,
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          value: 0,
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
    default: defaultDiscount,
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
          value: 0,
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
    default: {setPrice: 0},
    content: [
      [
        {type: 'type'},
        {
          name: 'setPrice',
          value: 0,
          widget: 'currency',
          template: props => <WidgetContainer>Get {props.children} off shipping.</WidgetContainer>
        }
      ]
    ]
  },
  {
    type: 'setPrice',
    title: 'Set price',
    default: {setPrice: 0},
    content: [
      [
        {type: 'type'},
        {
          name: 'setPrice',
          value: 0,
          widget: 'currency',
          template: props => <WidgetContainer>Set price to {props.children}</WidgetContainer>
        }
      ]
    ]
  },
];

export default offers;
