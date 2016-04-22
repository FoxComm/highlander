
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
    type: 'itemsSelectPercentOff',
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
          name: 'references',
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
  }
];

export default offers;
