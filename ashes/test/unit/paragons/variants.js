
import _ from 'lodash';

const Variants = requireSource('paragons/variants.js');
const Products = requireSource('paragons/product.js');

function makeVariants(count) {
  const result = [];
  let i = 0;
  while (count--) {
    const productVariant = Products.createEmptyProductVariant();
    productVariant.feCode = `sku${i++}`;
    result.push(productVariant);
  }
  return result;
}

describe('Variants', function () {
  context('#autoAssignOptions', () => {
    it('grow1', () => {
      const variants = makeVariants(2);

      const options = [
        {
          values: [
            {
              name: 'L',
              skus: [variants[0].feCode]
            },
            {
              name: 'S',
              skus: [variants[1].feCode]
            }
          ]
        }, {
          values: [
            {
              name: 'green',
              skus: []
            }
          ]
        }
      ];

      const newOptions = Variants.autoAssignOptions({variants}, options).options;
      expect(newOptions).to.deep.equal(
        [
          {
            values: [
              {
                name: 'L',
                skus: [variants[0].feCode]
              },
              {
                name: 'S',
                skus: [variants[1].feCode]
              }
            ]
          }, {
            values: [
              {
                name: 'green',
                skus: [variants[0].feCode, variants[1].feCode]
              }
            ]
          }
        ]
      );
    });

    it('grow2', () => {
      const variants = makeVariants(2);

      const options = [
        {
          values: [
            {
              name: 'S',
              skus: [variants[1].feCode]
            },
            {
              name: 'L',
              skus: [variants[0].feCode]
            },
          ]
        }, {
          values: [
            {
              name: 'green',
              skus: [variants[0].feCode, variants[1].feCode]
            }
          ]
        }, {
          values: [
            {
              name: 'male',
              skus: []
            }, {
              name: 'female',
              skus: []
            }
          ]
        }
      ];

      const newOptions = Variants.autoAssignOptions({variants}, options).options;
      expect(newOptions).to.have.length(3);
      expect(newOptions[0].values[0].skus).to.have.length(2);
      expect(newOptions[0].values[1].skus).to.have.length(2);
      expect(newOptions[1].values[0].skus).to.have.length(4);
      expect(newOptions[2].values[0].skus).to.have.length(2);
      expect(newOptions[2].values[0].skus).to.have.members([variants[0].feCode, variants[1].feCode]);
    });

    it('decrease1', () => {
      const variants = makeVariants(5);
      const options = [
        {
          values: [
            {
              name: 'L',
              skus: [variants[0].feCode, variants[2].feCode, variants[4].feCode]
            },
            {
              name: 'S',
              skus: [variants[1].feCode, variants[3].feCode]
            }
          ]
        }, {
          values: [
            {
              name: 'green',
              skus: [variants[0].feCode, variants[1].feCode, variants[2].feCode, variants[3].feCode]
            }
          ]
        }
      ];

      const result = Variants.autoAssignOptions({variants}, options);
      //console.log(JSON.stringify(result.variants, null, 2));
      expect(result.options).to.have.length(2);
      expect(result.options[0].values[0].skus).to.have.members([variants[0].feCode]);
      expect(result.options[0].values[1].skus).to.have.members([variants[1].feCode]);
      expect(result.options[1].values[0].skus).to.have.members([variants[0].feCode, variants[1].feCode]);
    });

    it('keep one sku if there is no variants', () => {
      const variants = makeVariants(2);
      const product = {variants};

      const newProduct = Variants.autoAssignOptions(product, []);
      expect(newProduct.variants).to.have.length(1);
    });
  });

  const redValue = {'name':'Red','swatch':'FF0000','skus':[]};
  const greenValue = {'name':'Green','swatch':'00FF00','skus':[]};
  const smallValue = {'name':'S','skus':[]};
  const mediumValue = {'name':'M','skus':[]};
  const largeValue = {'name':'L','skus':[]};

  context('#allOptionsValues', function () {
    it('should return array of items when one varinat is defined', function () {
      const options = [
        {
          'values':[
            redValue,
            greenValue,
          ],
          'attributes':{'name':{'t':'string','v':'Color'}}
        }
      ];
      expect(Variants.allOptionsValues(options)).to.be.eql(
        [[redValue], [greenValue]]
      );
    });

    it('should return array of all combinations when multiple varinats are defined', function () {
      const options = [
        {
          'values': [
            redValue,
            greenValue,
          ],
          'attributes': {'name':{'t':'string','v':'Color'}}
        },
        {
          'values': [
            smallValue, mediumValue, largeValue,
          ],
          'attributes': {'name':{'t':'string','v':'Size'}}
        }
      ];
      expect(Variants.allOptionsValues(options)).to.be.eql(
        [
          [redValue, smallValue],
          [redValue, mediumValue],
          [redValue, largeValue],
          [greenValue, smallValue],
          [greenValue, mediumValue],
          [greenValue, largeValue],
        ]
      );
    });

    it('should return array of all combinations when multiple varinats are defined with one option', function () {
      const options = [
        {
          'values': [
            greenValue,
          ],
          'attributes': {'name':{'t':'string','v':'Color'}}
        },
        {
          'values': [
            smallValue, mediumValue, largeValue,
          ],
          'attributes': {'name':{'t':'string','v':'Size'}}
        }
      ];
      expect(Variants.allOptionsValues(options)).to.be.eql(
        [
          [greenValue, smallValue],
          [greenValue, mediumValue],
          [greenValue, largeValue],
        ]
      );
    });
  });

  context('optionsWithMultipleValues', function () {
    it('should replace variants with empty value list from array', function () {
      const options = [
        {
          'values': [],
          'attributes': {'name':{'t':'string','v':'Color'}}
        }
      ];
      expect(Variants.optionsWithMultipleValues(options)).to.be.eql([]);
    });
  });
});
