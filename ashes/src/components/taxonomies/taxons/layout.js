/* @flow */

import compact from 'lodash/compact';

export default (taxonomy: Taxonomy) => ({
  main: [
    {
      type: 'group',
      title: 'General',
      content: compact([
        taxonomy.hierarchical ? { type: 'location' } : null,
        {
          type: 'fields',
          fields: {
            canAddProperty: true,
            includeRest: true,
            value: [
              'name',
              'description',
              'colorSwatch',
            ],
          },
        }
      ])
    }
  ],
  aside: [
    {
      type: 'state',
    },
    {
      type: 'watchers',
    },
    {
      type: 'taxonList',
    },
  ]
});
