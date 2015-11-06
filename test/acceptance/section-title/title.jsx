
import React from 'react';
import TestUtils from 'react-addons-test-utils';
import ShallowTestUtils from 'react-shallow-testutils';

describe('Title', function() {
  const Title = requireComponent('section-title/title.jsx');
  const title = 'Orders';
  const subtitle = '40237';

  it('should contain title text', function *() {
    expect(Title({title, subtitle}), 'to contain', title);
  });

  it('should not render subtitle if subtitle is not set', function *() {
    const titleElement = Title({title});

    const subtitleElements = ShallowTestUtils.findAllWithClass(titleElement, 'fc-section-title-subtitle');
    expect(subtitleElements).to.be.empty;
    expect(titleElement.props.children).to.contain(title);
  });

  it('should render subtitle if subtitle is set', function *() {
    const titleElement = Title({title, subtitle});
    const subtitleElements = ShallowTestUtils.findWithClass(titleElement, 'fc-section-title-subtitle');

    expect(subtitleElements.props.children).to.contain(subtitle);
  });

});
