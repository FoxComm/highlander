import React from 'react';
import * as ShallowTestUtils from 'react-shallow-testutils';

describe('CreditCardForm', function() {

  const CreditCardForm = requireComponent('credit-cards/card-form.jsx').WrappedComponent;

  let form;

  const defaultProps = {
    onChange: () => {},
    onSubmit: () => {},
    onCancel: () => {},
    customerId: Math.floor(Math.random() * 10) + 1,
    card: {}
  };

  const card = {
    address: {
      id: Math.floor(Math.random() * 10) + 1
    }
  };

  afterEach(function() {
    if (form) {
      form.unmount();
      form = null;
    }
  });

  xit('should contain New Credit Card header when creating new card', function *() {
    form = shallowRender(
      <CreditCardForm isNew={ true } {...defaultProps} />
    );
    expect(form.props.children, 'to contain', 'New Credit Card');
  });

  it('should be draggable when property is false', function *() {
    form = shallowRender(
      <CreditCardForm isNew={ false } card={ card } {...defaultProps} />
    );
    expect(ShallowTestUtils.findAllWithType(form, 'header')).to.be.empty;
  });

});
