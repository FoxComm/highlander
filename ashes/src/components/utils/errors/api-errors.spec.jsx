import React from 'react';
import { mount } from 'enzyme';

import ApiErrors from './api-errors';
import Alert from 'components/core/alert';

describe('ApiErrors', function() {
  it('should render ApiErrors with response object if errors is array', function() {
    const response = {
      response: {
        body: {
          errors: ['error #1', 'error #2'],
        },
      },
    };

    const errors = mount(<ApiErrors response={response} />);

    expect(errors.find(Alert)).to.have.length(2);
    expect(errors.find(Alert).first().text()).to.equal('error #1');
    expect(errors.find(Alert).last().text()).to.equal('error #2');
  });

  it('should render ApiErrors with response object if errors is not array', function() {
    const response = {
      response: {
        body: {
          errors: 'error #1',
        },
      },
    };

    const errors = mount(<ApiErrors response={response} />);

    expect(errors.find(Alert)).to.have.length(1);
    expect(errors.find(Alert).first().text()).to.equal('error #1');
  });

  it('should render ApiErrors with error object', function() {
    const response = new Error('error #1');

    const errors = mount(<ApiErrors response={response} />);

    expect(errors.find(Alert)).to.have.length(1);
    expect(errors.find(Alert).first().text()).to.equal('Error: error #1');
  });
});
