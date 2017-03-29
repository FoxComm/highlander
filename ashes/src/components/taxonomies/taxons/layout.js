/* @flow */

import compact from 'lodash/compact';

export default (taxonomy: Taxonomy): ObjectPageLayout => ({
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
  ]
});
