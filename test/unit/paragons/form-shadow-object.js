import _ from 'lodash';
import nock from 'nock';

const { addAttribute, setAttribute, illuminateAttributes } = requireSource('paragons/form-shadow-object.js');

describe('paragons.formShadowObject', () => {
  const form = {
    '01126c9e24': ['https://aws.amazonaws.com/some.jpg'],
    '1e00e91526': 'Running Shoe',
    '12e19f9811': 'http://foxcommerce.com/products/running-shoe',
    '987bf76112': { currency: 'USD', value: 12900 },
  };

  const shadow = {
    title: {
      ref: '1e00e91526',
      type: 'string'
    },
    images: {
      ref: '01126c9e24',
      type: 'images',
    },
    description: {
      ref: '1e00e91526',
      type: 'richText',
    },
    url: {
      ref: '12e19f9811',
      type: 'string',
    },
    price: {
      ref: '987bf76112',
      type: 'price',
    },
  };

  describe('addAttribute', () => {
    it('should add a new attribute', () => {
      const vendor = 'New Balance';
      const update = addAttribute('vendor', 'string', vendor, form, shadow);

      expect(update.form.vendor).to.be.equal(vendor);
      expect(update.shadow.vendor.type).to.be.equal('string');
      expect(update.shadow.vendor.ref).to.be.equal('vendor');
    });

    it('should not update the attribute if it already exists', () => {
      const title = 'Some running shoes';
      const update = addAttribute('title', 'string', title, form, shadow);

      const newRef = update.shadow.title.ref;
      const oldRef = shadow.title.ref;
      expect(newRef).to.be.equal(oldRef);
      expect(update.form[newRef]).to.be.equal(form[oldRef]);
    });
  });

  describe('setAttribute', () => {
    it('should update a string attribute', () => {
      const url = 'http://foxcommerce.com/p/running-shoe';
      const update = setAttribute('url', 'string', url, form, shadow);

      expect(update.form.url).to.be.equal(url);
      expect(update.shadow.url.ref).to.be.equal('url');
    });

    it('should update a price attribute', () => {
      const price = 9999;
      const update = setAttribute('price', 'price', price, form, shadow);

      expect(update.form.price.currency).to.be.equal('USD');
      expect(update.form.price.value).to.be.equal(price);
      expect(update.shadow.price.ref).to.be.equal('price');
    });

    it('should update an attribute that has the same value as another attribute', () => {
      const title = 'New Balance Running Shoe';
      const update = setAttribute('title', 'string', title, form, shadow);

      expect(update.form.title).to.be.equal(title);
      expect(update.shadow.title.ref).to.be.equal('title');

      const originalRef = shadow.description.ref;
      expect(update.shadow.description.ref).to.be.equal(originalRef);
      expect(update.form[originalRef]).to.be.equal(form[originalRef]);
    });

    it('should add a new attribute', () => {
      const vendor = 'New Balance';
      const update = setAttribute('vendor', 'string', vendor, form, shadow);

      expect(update.form.vendor).to.be.equal(vendor);
      expect(update.shadow.vendor.type).to.be.equal('string');
      expect(update.shadow.vendor.ref).to.be.equal('vendor');
    });
  });

  describe('illuminateAttributes', () => {
    let illuminated = null;
    beforeEach(() => {
      illuminated = illuminateAttributes(form, shadow);
    });

    it('should render a string attribute', () => {
      expect(illuminated.title.label).to.be.equal('title');
      expect(illuminated.title.type).to.be.equal('string');
      expect(illuminated.title.value).to.be.equal('Running Shoe');
    });

    it('should render a price attribute', () => {
      expect(illuminated.price.label).to.be.equal('price');
      expect(illuminated.price.type).to.be.equal('price');
      expect(illuminated.price.value.currency).to.be.equal('USD');
      expect(illuminated.price.value.value).to.be.equal(12900);
    });
  });
});
