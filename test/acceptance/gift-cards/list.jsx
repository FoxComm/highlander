'use strict';

const React = require('react');
const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');
const path = require('path');

describe('GiftCards List', function() {
  let
    GiftCards = null,
    giftcards = null;

  before(function() {
  });

  afterEach(function(done) {
    ReactDOM.unmountComponentAtNode(document.body);
    setTimeout(done);
  });

  it('should have a list of gift-cards', function *() {
  });
});
