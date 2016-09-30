
import _ from 'lodash';

const Variants = requireSource('paragons/variants.js');
const Products = requireSource('paragons/product.js');

describe.only('Variants', function () {
  context('#autoAssignVariants', () => {
    it('ex1', () => {
      const skus = [
        Products.createEmptySku(),
        Products.createEmptySku(),
      ];

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

      const newVariants = Variants.autoAssignVariants(skus, variants);
    });

    it('ex3', () => {
      const skus = [
        Products.createEmptySku(),
        Products.createEmptySku(),
        Products.createEmptySku(),
        Products.createEmptySku(),
        Products.createEmptySku(),
        Products.createEmptySku(),
        Products.createEmptySku(),
        Products.createEmptySku(),
      ];

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
              skuCodes: [skus[0].feCode, skus[1].feCode]
            }, {
              name: 'red',
              skuCodes: []
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

      const newVariants = Variants.autoAssignVariants(skus, variants);
      console.log(JSON.stringify(newVariants, null, 2));
    });
  });
});
