import _ from 'lodash';

const ProductParagon = requireSource('paragons/product.js');

describe('ProductParagon', function () {
  describe('availableVariants', function () {
    const redValue = {"name":"Red","swatch":"FF0000","skuCodes":[]};
    const greenValue = {"name":"Green","swatch":"00FF00","skuCodes":[]};
    const smallValue = {"name":"S","skuCodes":[]};
    const mediumValue = {"name":"M","skuCodes":[]};
    const largeValue = {"name":"L","skuCodes":[]};

    it('should return array of items when one varinat is defined', function () {
      const variants = [
        {
          "values":[
            redValue,
            greenValue,
          ],
          "attributes":{"name":{"t":"string","v":"Color"}}
        }
      ];
      expect(ProductParagon.availableVariants(variants)).to.be.eql(
        [[redValue], [greenValue]]
      );
    });

    it('should return array of all combinations when multiple varinats are defined', function () {
      const variants = [
        {
          "values": [
            redValue,
            greenValue,
          ],
          "attributes": {"name":{"t":"string","v":"Color"}}
        },
        {
          "values": [
            smallValue, mediumValue, largeValue,
          ],
          "attributes": {"name":{"t":"string","v":"Size"}}
        }
      ];
      expect(ProductParagon.availableVariants(variants)).to.be.eql(
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
      const variants = [
        {
          "values": [
            greenValue,
          ],
          "attributes": {"name":{"t":"string","v":"Color"}}
        },
        {
          "values": [
            smallValue, mediumValue, largeValue,
          ],
          "attributes": {"name":{"t":"string","v":"Size"}}
        }
      ];
      expect(ProductParagon.availableVariants(variants)).to.be.eql(
        [
          [greenValue, smallValue],
          [greenValue, mediumValue],
          [greenValue, largeValue],
        ]
      );
    });
  });

  describe('variantsWithMultipleOptions', function () {
    it('should replace variants with empty value list from array', function () {
      const variants = [
        {
          "values": [],
          "attributes": {"name":{"t":"string","v":"Color"}}
        }
      ];
      expect(ProductParagon.variantsWithMultipleOptions(variants)).to.be.eql([]);
    });
  });
});
