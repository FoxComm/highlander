import React from 'react';
import sinon from 'sinon';
import { mount, shallow } from 'enzyme';

import Errors from './errors';
import Alert from 'components/core/alert';

describe('Errors', function() {
  it('should render 2 alerts in Errors', function() {
    const errors = mount(<Errors errors={['error #1', 'error #2']} />);

    expect(errors.find(Alert)).to.have.length(2);
    expect(errors.find(Alert).first().text()).to.equal('error #1');
    expect(errors.find(Alert).last().text()).to.equal('error #2');
  });

  it('should render custom className', function() {
    const errors = shallow(<Errors errors={['error #1', 'error #2']} className="errors" />);

    expect(errors.hasClass('errors')).to.be.true;
  });

  it('should preprocess error text', function() {
    const errors = mount(<Errors errors={['error #1', 'error #2']} sanitizeError={err => err + '!'} />);

    expect(errors.find(Alert)).to.have.length(2);
    expect(errors.find(Alert).first().text()).to.equal('error #1!');
    expect(errors.find(Alert).last().text()).to.equal('error #2!');
  });

  it('should handle closeAction', function() {
    const onClick = sinon.spy();

    const errors = mount(<Errors errors={['error #1', 'error #2']} closeAction={onClick} />);

    errors.find('.close').first().simulate('click');
    errors.find('.close').last().simulate('click');
    expect(onClick.calledTwice).to.be.true;
  });
});
