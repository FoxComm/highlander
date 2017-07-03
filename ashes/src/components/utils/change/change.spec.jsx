import React from 'react';
import { shallow } from 'enzyme';

import Change from './change';

describe('Change', function() {
  it('should render Change component', function() {
    const change = shallow(<Change value={0} />);

    expect(change.find('span').hasClass('change')).to.be.true;
  });

  it('should render positive value with proper styling', function() {
    const change = shallow(<Change value={10} />);

    expect(change.hasClass('positive')).to.be.true;
    expect(change.text()).to.equal('10');
  });

  it('should render negative value with proper styling', function() {
    const change = shallow(<Change value={-10} />);

    expect(change.hasClass('negative')).to.be.true;
    expect(change.text()).to.equal('10');
  });

  it('should render zero value with proper styling', function() {
    const change = shallow(<Change value={0} />);

    expect(change.hasClass('negative')).to.be.false;
    expect(change.hasClass('positive')).to.be.false;
    expect(change.hasClass('change')).to.be.true;
  });
});
