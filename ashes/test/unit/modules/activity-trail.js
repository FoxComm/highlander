describe('Activity Trail module', function() {
  const { processActivities, mergeActivities } = requireSource('modules/activity-trail.js');
  const { default: types, derivedTypes } = requireSource('components/activity-trail/activities/base/types.js');

  const examples = [
    {
      original: [
        {
          kind: types.CART_LINE_ITEMS_UPDATED_QUANTITIES,
          data: {
            oldQuantities: { sku1: 1, sku2: 1 },
            newQuantities: { sku1: 0 },
          },
        },
      ],
      expected: [
        {
          kind: derivedTypes.CART_LINE_ITEMS_REMOVED_SKU,
          data: {
            difference: 1,
            skuName: 'sku1',
          },
        },
      ],
    },
    {
      original: [
        {
          kind: types.CART_LINE_ITEMS_UPDATED_QUANTITIES,
          data: {
            oldQuantities: { sku1: 1, sku2: 1 },
            newQuantities: { sku2: 4 },
          },
        },
      ],
      expected: [
        {
          kind: derivedTypes.CART_LINE_ITEMS_ADDED_SKU,
          data: {
            difference: 3,
            skuName: 'sku2',
          },
        },
      ],
    },
    {
      original: [
        {
          kind: types.CART_LINE_ITEMS_UPDATED_QUANTITIES,
          data: {
            oldQuantities: { sku1: 1, sku2: 1 },
            newQuantities: { sku1: 2 },
          },
        },
        {
          kind: types.CART_LINE_ITEMS_UPDATED_QUANTITIES,
          data: {
            oldQuantities: { sku1: 2, sku2: 1 },
            newQuantities: { sku1: 3 },
          },
        },
      ],
      expected: [
        {
          kind: derivedTypes.CART_LINE_ITEMS_ADDED_SKU,
          data: {
            difference: 1,
            skuName: 'sku1',
          },
        },
        {
          kind: derivedTypes.CART_LINE_ITEMS_ADDED_SKU,
          data: {
            difference: 1,
            skuName: 'sku1',
          },
        },
      ],
    },
    {
      original: [
        {
          kind: types.CART_LINE_ITEMS_UPDATED_QUANTITIES,
          data: {
            oldQuantities: { 'SKU-BRO': 1, 'SKU-TRL': 1 },
            newQuantities: { 'SKU-BRO': 0, 'SKU-TRL': 1 },
          },
        },
      ],
      expected: [
        {
          kind: derivedTypes.CART_LINE_ITEMS_REMOVED_SKU,
          data: {
            difference: 1,
            skuName: 'SKU-BRO',
          },
        },
      ],
    },
  ];

  examples.map((ex, i) => {
    it(`should match example ${i}`, () => {
      const processed = processActivities(ex.original);

      expect(processed).to.deep.equal(ex.expected);
    });
  });

  it('should correctly merge processed activities', () => {
    const data = [
      {
        id: 1841,
        kind: types.CART_LINE_ITEMS_UPDATED_QUANTITIES,
        data: {
          oldQuantities: { 'SKU-BRO': 1, 'SKU-TRL': 0 },
          newQuantities: { 'SKU-BRO': 0, 'SKU-TRL': 1 },
        },
      },
    ];

    const processed = mergeActivities([], processActivities(data));

    expect(processed).to.have.lengthOf(2);
  });
});
