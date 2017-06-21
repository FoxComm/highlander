
import React from 'react';
import WidgetContainer from './widgets/widget-container';

const defaultDiscount = { discount: 0 };

const offers = [
  {
    type: 'orderPercentOff',
    title: 'Percent off order',
    default: defaultDiscount,
    validate: {
      discount: {
        validate: (v) => v > 0,
        error: 'Percent off order shall be greater than 0.',
      },
    },
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
    default: defaultDiscount,
    validate: {
      discount: {
        validate: (v) => v > 0,
        error: 'Amount off order shall be greater than 0.',
      },
    },
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
    default: defaultDiscount,
    validate: {
      discount: {
        validate: (v) => v > 0,
        error: 'Percent off single item shall be greater than 0.',
      },
      search: {
        validate: (v) => v.length > 0,
        error: 'Percent off single item search is not specified.',
      },
    },
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
    default: defaultDiscount,
    validate: {
      discount: {
        validate: (v) => v > 0,
        error: 'Amount off single item shall be greater than 0.',
      },
      search: {
        validate: (v) => v.length > 0,
        error: 'Amount off single item search is not specified.',
      },
    },
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
    default: defaultDiscount,
    validate: {
      discount: {
        validate: (v) => v > 0,
        error: 'Percent off select items shall be greater than 0.',
      },
      search: {
        validate: (v) => v.length > 0,
        error: 'Percent off select items search is not specified.',
      },
    },
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
    default: defaultDiscount,
    validate: {
      discount: {
        validate: (v) => v > 0,
        error: 'Amount off select items shall be greater than 0.',
      },
      search: {
        validate: (v) => v.length > 0,
        error: 'Amount off select items search is not specified.',
      },
    },
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
    default: {discount: 0},
    validate: {
      discount: {
        validate: (v) => v > 0,
        error: 'Discounted shipping value shall be greater than 0.',
      },
    },
    content: [
      [
        {type: 'type'},
        {
          name: 'discount',
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
    validate: {
      setPrice: {
        validate: (v) => v > 0,
        error: 'Set price value shall be greater than 0.',
      },
    },
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
