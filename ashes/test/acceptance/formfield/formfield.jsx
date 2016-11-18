
import React from 'react';
import ReactTestUtils from 'react-addons-test-utils';

describe('FormField', function() {
  const FormField = requireComponent('forms/formfield.jsx');
  let formfield;

  afterEach(function() {
    if (formfield) {
      formfield.unmount();
      formfield = null;
    }
  });

  it('should validate by maxLength and validator constraints', function *() {
    formfield = yield renderIntoDocument(
      <FormField maxLength={5} validator='ascii' label="Lorem Ipsum">
        <input type="text" value="Кошку ела собака"/>
      </FormField>,
      true
    );

    formfield.validate();
    expect(formfield.errors).to.deep.equal(
      [
        'Lorem Ipsum can not be more than 5 characters',
        'Lorem Ipsum must contain only ASCII characters'
      ]
    );
  });

  it('should validate text inputs by required constraint', function *() {
    formfield = yield renderIntoDocument(
      <FormField required label="Lorem Ipsum">
        <input type="text" value=''/>
      </FormField>,
      true
    );

    formfield.validate();
    expect(formfield.errors).to.deep.equal(
      [
        'Lorem Ipsum is a required field'
      ]
    );
  });

  it('should not validate checkbox inputs by required constraint', function *() {
    formfield = yield renderIntoDocument(
      <FormField required label="Lorem Ipsum">
        <input type="checkbox"/>
      </FormField>,
      true
    );

    formfield.validate();
    expect(formfield.errors).to.deep.equal([]);

  });

  it('should attach to input even though if it placed deeply in markup', function *() {
    formfield = yield renderIntoDocument(
      <FormField required label="Lorem Ipsum">
          <div>
            <article>
              <p><input type="tel"/></p>

            </article>
          </div>
      </FormField>,
      true
    );

    const inputNode = ReactTestUtils.findRenderedDOMComponentWithTag(formfield, 'input');

    expect(inputNode).to.equal(formfield.findTargetNode());
  });
});
