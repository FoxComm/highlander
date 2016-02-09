
describe('Activity Trail module', function() {
  const { processActivities } = requireSource('modules/activity-trail.js');
  const {default: types, derivedTypes } = requireSource('components/activity-trail/activities/base/types.js');

  const examples = [
    {
      original: [
        {
          kind: types.ORDER_LINE_ITEMS_UPDATED_QUANTITIES,
          data: {
            oldQuantities: {sku1: 1, sku2: 1},
            newQuantities: {sku1: 0}
          }
        }
      ],
      expected: [
        {
          kind: derivedTypes.ORDER_LINE_ITEMS_REMOVED_SKU,
          data: {
            difference: 1,
            skuName: 'sku1'
          }
        }
      ]
    },
    {
      original: [
        {
          kind: types.ORDER_LINE_ITEMS_UPDATED_QUANTITIES,
          data: {
            oldQuantities: {sku1: 1, sku2: 1},
            newQuantities: {sku2: 4}
          }
        }
      ],
      expected: [
        {
          kind: derivedTypes.ORDER_LINE_ITEMS_ADDED_SKU,
          data: {
            difference: 3,
            skuName: 'sku2'
          }
        }
      ]
    },
    {
      original: [
        {
          kind: types.ORDER_LINE_ITEMS_UPDATED_QUANTITIES,
          data: {
            oldQuantities: {sku1: 1, sku2: 1},
            newQuantities: {sku1: 2}
          }
        },
        {
          kind: types.ORDER_LINE_ITEMS_UPDATED_QUANTITIES,
          data: {
            oldQuantities: {sku1: 2, sku2: 1},
            newQuantities: {sku1: 3}
          }
        }
      ],
      expected: [
        {
          kind: derivedTypes.ORDER_LINE_ITEMS_ADDED_SKU,
          data: {
            difference: 1,
            skuName: 'sku1'
          }
        },
        {
          kind: derivedTypes.ORDER_LINE_ITEMS_ADDED_SKU,
          data: {
            difference: 1,
            skuName: 'sku1'
          }
        },
      ]
    },
  ];

  examples.map((ex, i) => {
    it(`should match example ${i}`, () => {
      const processed = processActivities(ex.original);

      expect(processed).to.deep.equal(ex.expected);
    });
  });
});
