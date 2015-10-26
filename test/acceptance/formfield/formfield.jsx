'use strict';

const _ = require('lodash');
const React = require('react');
const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');
const path = require('path');

describe('FormField', function() {
  const FormField = requireComponent('forms/formfield.jsx');

  it('should validate by maxLength and validator constraints', function(cb) {
    const formfield = ReactDOM.render(
      <FormField maxLength={5} validator='ascii' label="Lorem Ipsum">
        <input type="text" value="Кошку ела собака"/>
      </FormField>
      , container, later(function() {
        formfield.validate();
        expect(formfield.state.errors).to.deep.equal(
          [
            'Lorem Ipsum can not be more than 5 characters',
            'Lorem Ipsum must contain only ASCII characters'
          ]
        );

        cb();
      }));
  });

  it('should validate text inputs by required constraint', function(cb) {
    const formfield = ReactDOM.render(
      <FormField required label="Lorem Ipsum">
        <input type="text" value=''/>
      </FormField>
      , container, later(function() {
        formfield.validate();
        expect(formfield.state.errors).to.deep.equal(
          [
           'Lorem Ipsum is required field'
          ]
        );

        cb();
      }));
  });

  it('should not validate checkbox inputs by required constraint', function(cb) {
    const formfield = ReactDOM.render(
      <FormField required label="Lorem Ipsum">
        <input type="checkbox"/>
      </FormField>
      , container, later(function() {
        formfield.validate();
        expect(formfield.state.errors).to.deep.equal([]);

        cb();
      }));
  });

  it('should attach to input even though if it placed deeply in markup', function(cb) {
    const formfield = ReactDOM.render(
      <FormField required label="Lorem Ipsum">
          <div>
            <article>
              <p><input type="tel"/></p>

            </article>
          </div>
      </FormField>
      , container, later(function() {
        const inputNode = TestUtils.findRenderedDOMComponentWithTag(formfield, 'input');

        expect(inputNode).to.equal(formfield.getInputNode());
        cb();
      }));
  });
});
