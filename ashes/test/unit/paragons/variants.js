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

describe('Variants', function() {
  context('#autoAssignVariants', () => {
    it('grow1', () => {
      const skus = makeSkus(2);

      const variants = [
        {
          values: [
            {
              name: 'L',
              skuCodes: [skus[0].feCode],
            },
            {
              name: 'S',
              skuCodes: [skus[1].feCode],
            },
          ],
        },
        {
          values: [
            {
              name: 'green',
              skuCodes: [],
            },
          ],
        },
      ];

      const newVariants = Variants.autoAssignVariants({ skus }, variants).variants;
      expect(newVariants).to.deep.equal([
        {
          values: [
            {
              name: 'L',
              skuCodes: [skus[0].feCode],
            },
            {
              name: 'S',
              skuCodes: [skus[1].feCode],
            },
          ],
        },
        {
          values: [
            {
              name: 'green',
              skuCodes: [skus[0].feCode, skus[1].feCode],
            },
          ],
        },
      ]);
    });

    it('grow2', () => {
      const skus = makeSkus(2);

      const variants = [
        {
          values: [
            {
              name: 'S',
              skuCodes: [skus[1].feCode],
            },
            {
              name: 'L',
              skuCodes: [skus[0].feCode],
            },
          ],
        },
        {
          values: [
            {
              name: 'green',
              skuCodes: [skus[0].feCode, skus[1].feCode],
            },
          ],
        },
        {
          values: [
            {
              name: 'male',
              skuCodes: [],
            },
            {
              name: 'female',
              skuCodes: [],
            },
          ],
        },
      ];

      const newVariants = Variants.autoAssignVariants({ skus }, variants).variants;
      expect(newVariants).to.have.length(3);
      expect(newVariants[0].values[0].skuCodes).to.have.length(2);
      expect(newVariants[0].values[1].skuCodes).to.have.length(2);
      expect(newVariants[1].values[0].skuCodes).to.have.length(4);
      expect(newVariants[2].values[0].skuCodes).to.have.length(2);
      expect(newVariants[2].values[0].skuCodes).to.have.members([skus[0].feCode, skus[1].feCode]);
    });

    it('decrease1', () => {
      const skus = makeSkus(5);
      const variants = [
        {
          values: [
            {
              name: 'L',
              skuCodes: [skus[0].feCode, skus[2].feCode, skus[4].feCode],
            },
            {
              name: 'S',
              skuCodes: [skus[1].feCode, skus[3].feCode],
            },
          ],
        },
        {
          values: [
            {
              name: 'green',
              skuCodes: [skus[0].feCode, skus[1].feCode, skus[2].feCode, skus[3].feCode],
            },
          ],
        },
      ];

      const result = Variants.autoAssignVariants({ skus }, variants);
      //console.log(JSON.stringify(result.variants, null, 2));
      expect(result.variants).to.have.length(2);
      expect(result.variants[0].values[0].skuCodes).to.have.members([skus[0].feCode]);
      expect(result.variants[0].values[1].skuCodes).to.have.members([skus[1].feCode]);
      expect(result.variants[1].values[0].skuCodes).to.have.members([skus[0].feCode, skus[1].feCode]);
    });

    it('keep one sku if there is no variants', () => {
      const skus = makeSkus(2);
      const product = { skus };

      const newProduct = Variants.autoAssignVariants(product, []);
      expect(newProduct.skus).to.have.length(1);
    });
  });

  const redValue = { name: 'Red', swatch: 'FF0000', skuCodes: [] };
  const greenValue = { name: 'Green', swatch: '00FF00', skuCodes: [] };
  const smallValue = { name: 'S', skuCodes: [] };
  const mediumValue = { name: 'M', skuCodes: [] };
  const largeValue = { name: 'L', skuCodes: [] };

  context('#allVariantsValues', function() {
    it('should return array of items when one varinat is defined', function() {
      const variants = [
        {
          values: [redValue, greenValue],
          attributes: { name: { t: 'string', v: 'Color' } },
        },
      ];
      expect(Variants.allVariantsValues(variants)).to.be.eql([[redValue], [greenValue]]);
    });

    it('should return array of all combinations when multiple varinats are defined', function() {
      const variants = [
        {
          values: [redValue, greenValue],
          attributes: { name: { t: 'string', v: 'Color' } },
        },
        {
          values: [smallValue, mediumValue, largeValue],
          attributes: { name: { t: 'string', v: 'Size' } },
        },
      ];
      expect(Variants.allVariantsValues(variants)).to.be.eql([
        [redValue, smallValue],
        [redValue, mediumValue],
        [redValue, largeValue],
        [greenValue, smallValue],
        [greenValue, mediumValue],
        [greenValue, largeValue],
      ]);
    });

    it('should return array of all combinations when multiple varinats are defined with one option', function() {
      const variants = [
        {
          values: [greenValue],
          attributes: { name: { t: 'string', v: 'Color' } },
        },
        {
          values: [smallValue, mediumValue, largeValue],
          attributes: { name: { t: 'string', v: 'Size' } },
        },
      ];
      expect(Variants.allVariantsValues(variants)).to.be.eql([
        [greenValue, smallValue],
        [greenValue, mediumValue],
        [greenValue, largeValue],
      ]);
    });
  });

  context('variantsWithMultipleOptions', function() {
    it('should replace variants with empty value list from array', function() {
      const variants = [
        {
          values: [],
          attributes: { name: { t: 'string', v: 'Color' } },
        },
      ];
      expect(Variants.variantsWithMultipleOptions(variants)).to.be.eql([]);
    });
  });
});
