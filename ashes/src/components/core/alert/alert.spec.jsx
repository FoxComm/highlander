import React from 'react';
import sinon from 'sinon';
import { shallow, mount } from 'enzyme';

import Alert from './alert';

describe('Alert', function () {

  it('should render success Alert', function () {
    const alert = mount(
      <Alert type={Alert.SUCCESS}>Success</Alert>
    );

    expect(alert.hasClass('alert')).to.be.true;
    expect(alert.hasClass('success')).to.be.true;
    expect(alert.text()).to.equal('Success');
    expect(alert.find('.close')).to.be.empty;
  });

  it('should render warning Alert', function () {
    const alert = mount(
      <Alert type={Alert.WARNING}>Warning</Alert>
    );

    expect(alert.hasClass('warning')).to.be.true;
    expect(alert.text()).to.equal('Warning');
  });

  it('should render error Alert', function () {
    const alert = mount(
      <Alert type={Alert.ERROR}>Error</Alert>
    );

    expect(alert.hasClass('error')).to.be.true;
    expect(alert.text()).to.equal('Error');
  });

  it('should handle close click', function () {
    const onClick = sinon.spy();

    const alert = shallow(
      <Alert type={Alert.ERROR} closeAction={onClick}>Error</Alert>
    );

    expect(alert.find('.close')).not.to.be.empty;

    alert.find('.close').simulate('click');
    expect(onClick.calledOnce).to.be.true;
  });
});
