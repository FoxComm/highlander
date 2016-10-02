
import _ from 'lodash';

const Variants = requireSource('paragons/variants.js');
const Products = requireSource('paragons/product.js');

function makeSkus(count) {
  const result = [];
  let i = 0;
  while (count--) {
    const sku = Products.createEmptySku();
    sku.feCode = `sku${i++}`;
    result.push(sku);
  }
  return result;
}

describe.only('Variants', function () {
  context('#autoAssignVariants', () => {
    it('grow1', () => {
      const skus = makeSkus(2);

      const variants = [
        {
          values: [
            {
              name: 'L',
              skuCodes: [skus[0].feCode]
            },
            {
              name: 'S',
              skuCodes: [skus[1].feCode]
            }
          ]
        }, {
          values: [
            {
              name: 'green',
              skuCodes: []
            }
          ]
        }
      ];

      const newVariants = Variants.autoAssignVariants(skus, variants).variants;
      expect(newVariants).to.deep.equal(
        [
          {
            values: [
              {
                name: 'L',
                skuCodes: [skus[0].feCode]
              },
              {
                name: 'S',
                skuCodes: [skus[1].feCode]
              }
            ]
          }, {
            values: [
              {
                name: 'green',
                skuCodes: [skus[0].feCode, skus[1].feCode]
              }
            ]
          }
        ]
      );
    });

    it('grow2', () => {
      const skus = makeSkus(2);

      const variants = [
        {
          values: [
            {
              name: 'S',
              skuCodes: [skus[1].feCode]
            },
            {
              name: 'L',
              skuCodes: [skus[0].feCode]
            },
          ]
        }, {
          values: [
            {
              name: 'green',
              skuCodes: [skus[0].feCode, skus[1].feCode]
            }
          ]
        }, {
          values: [
            {
              name: 'male',
              skuCodes: []
            }, {
              name: 'female',
              skuCodes: []
            }
          ]
        }
      ];

      const newVariants = Variants.autoAssignVariants(skus, variants).variants;
    });

    it('decrease1', () => {
      const skus = makeSkus(5);
      const variants = [
        {
          values: [
            {
              name: 'L',
              skuCodes: [skus[0].feCode, skus[2].feCode, skus[4].feCode]
            },
            {
              name: 'S',
              skuCodes: [skus[1].feCode, skus[3].feCode]
            }
          ]
        }, {
          values: [
            {
              name: 'green',
              skuCodes: [skus[0].feCode, skus[1].feCode, skus[2].feCode, skus[3].feCode]
            }
          ]
        }
      ];

      const result = Variants.autoAssignVariants(skus, variants);
      console.log(JSON.stringify(result.variants, null, 2));
      console.log(result.skus);
    });
  });
});
