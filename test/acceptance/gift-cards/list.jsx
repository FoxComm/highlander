'use strict';

require('testdom')('<html><body></body></html>');

const
  React = require('react/addons'),
  TestUtils = React.addons.TestUtils,
  path = require('path');

describe('GiftCards List', function() {
  let
    GiftCards = null,
    giftcards = null;

  before(function() {
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(document.body);
    setTimeout(done);
  });

  it('should have a list of gift-cards', function *() {
  });
});
