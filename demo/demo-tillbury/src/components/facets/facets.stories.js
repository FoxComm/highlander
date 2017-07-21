import _ from 'lodash';
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
        selected: false,
      },
      {
        label: 'Women',
        value: 'women',
        count: 1,
        selected: false,
      },
      {
        label: 'Kids',
        value: 'kids',
        count: 1,
        selected: false,
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
      selected: false,
    })),
  },
  {
    key: 'size2',
    name: 'Shoes',
    kind: 'select',
    values: [4, 4.5, 5, 5.5, 9, '9.5v - 10.5v', '10.7v - 18.4h', 10.5, 14, 14.5].map(v => ({
      value: String(v),
      label: String(v),
      count: 1,
      selected: false,
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
        selected: false,
        count: 66,
      },
      {
        label: 'White',
        value: {
          color: 'white',
          value: 'White',
        },
        selected: false,
        count: 31,
      },
      {
        label: 'Blue',
        value: {
          color: 'blue',
          value: 'Blue',
        },
        selected: false,
        count: 15,
      },
      {
        label: 'Collegiate Navy',
        value: {
          color: 'navy',
          value: 'Collegiate Navy',
        },
        selected: false,
        count: 13,
      },
      {
        label: 'Metallic Silver',
        value: {
          color: 'silver',
          value: 'Metallic Silver',
        },
        selected: false,
        count: 10,
      },
      {
        label: 'Grey',
        value: {
          color: 'grey',
          value: 'Grey',
        },
        selected: false,
        count: 9,
      },
      {
        label: 'Mystery Blue',
        value: {
          color: 'lightblue',
          value: 'Mystery Blue',
        },
        selected: false,
        count: 6,
      },
    ],
  },
  {
    key: 'image',
    name: 'Colors',
    kind: 'image',
    showValue: true,
    values: [
      {
        label: 'Black',
        value: {
          image: 'http://lorempixel.com/59/63/',
          value: 'black',
        },
        selected: false,
        count: 4,
      },
      {
        label: 'Silver',
        value: {
          image: 'http://lorempixel.com/59/63/',
          value: 'silver',
        },
        count: 12,
        selected: false,
      },
    ],
  },
];

/* eslint-disable no-param-reassign */

storiesOf('components.Facets', module)
  .add('base', () => {
    const handleSelect = (facet, value, selected) => {
      const facetEntry = _.find(facets, {key: facet});
      _.each(facetEntry.values, (v) => {
        v.selected = false;
      });
      const facetValue = _.find(facetEntry.values, (v) => {
        return _.get(v.value, 'value', v.value) == value;
      });
      facetValue.selected = selected;
    };

    return (
      <Facets facets={facets} onSelect={handleSelect} />
    );
  });
