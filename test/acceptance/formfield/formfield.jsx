'use strict';

require('testdom')('<html><body></body></html>');

const _ = require('lodash');
const React = require('react');
const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');
const path = require('path');

describe('FormField', function() {
  let FormField = require(path.resolve('src/components/forms/formfield.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(function(done) {
    document.body.removeChild(container);
    ReactDOM.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should validate by maxLength and validator constraints', function (cb) {
    let formfield = ReactDOM.render(
      <FormField maxLength={5} validator='ascii' label="Lorem Ipsum">
        <input type="text" value="Кошку ела собака"/>
      </FormField>
      , container, later(function() {
        formfield.validate();
        expect(formfield.state.errors).to.deep.equal(
          [
            "Lorem Ipsum can not be more than 255 characters",
            "Lorem Ipsum must contain only ASCII characters"
          ]
        );

        cb();
      }));
  });
});