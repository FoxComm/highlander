import React from 'react';
import { storiesOf } from '@kadira/storybook';
import Facets from './facets';

const facets = [
  {
    key: 'genders',
    name: 'Gender',
    kind: 'checkbox',
    values: [
      {
        label: 'Men',
        value: 'men',
        count: 1,
      },
      {
        label: 'Women',
        value: 'women',
        count: 1,
      },
      {
        label: 'Kids',
        value: 'kids',
        count: 1,
      },
    ],
  }, {
    key: 'size',
    name: 'Shoes',
    kind: 'circle',
    values: [4, 4.5, 5, 5.5, 9, '9.5v - 10.5v', '10.7v - 18.4h', 10.5, 14, 14.5].map(v => ({
      value: String(v),
      label: String(v),
      count: 1,
    })),
  },
  {
    key: 'color',
    name: 'Color',
    kind: 'color',
    values: [
      {
        label: 'Black',
        value: {
          color: 'black',
          value: 'Black',
        },
        count: 66,
      },
      {
        label: 'White',
        value: {
          color: 'white',
          value: 'White',
        },
        count: 31,
      },
      {
        label: 'Blue',
        value: {
          color: 'blue',
          value: 'Blue',
        },
        count: 15,
      },
      {
        label: 'Collegiate Navy',
        value: {
          color: 'navy',
          value: 'Collegiate Navy',
        },
        count: 13,
      },
      {
        label: 'Metallic Silver',
        value: {
          color: 'silver',
          value: 'Metallic Silver',
        },
        count: 10,
      },
      {
        label: 'Grey',
        value: {
          color: 'grey',
          value: 'Grey',
        },
        count: 9,
      },
      {
        label: 'Mystery Blue',
        value: {
          color: 'lightblue',
          value: 'Mystery Blue',
        },
        count: 6,
      },
    ],
  },
  {
    key: 'image',
    name: 'Colors',
    kind: 'image',
    values: [
      {
        label: 'Black',
        value: {
          image: 'http://lorempixel.com/59/63/',
          value: 'black',
        },
        count: 4,
      },
      {
        label: 'Silver',
        value: {
          image: 'http://lorempixel.com/59/63/',
          value: 'silver',
        },
        count: 12,
      },
    ],
  },
];

storiesOf('components.Facets', module)
  .add('base', () => {
    return (
      <Facets facets={facets} />
    );
  });
