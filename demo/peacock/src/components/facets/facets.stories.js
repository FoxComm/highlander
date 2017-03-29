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
    values: [4, 4.5, 5, 5.5, 9, 9.5, 10, 10.5, 14, 14.5].map(v => ({
      value: String(v),
      label: String(v),
      count: 1,
    })),
  },
];

storiesOf('components.Facets', module)
  .add('base', () => {
    return (
      <Facets facets={facets} />
    );
  });
