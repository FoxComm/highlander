
import React from 'react';
import ReactTestUtils from 'react-addons-test-utils';
import * as ShallowTestUtils from 'react-shallow-testutils';

describe('Title', function() {
  const Title = requireComponent('section-title/title.jsx');
  const title = 'Orders';
  const subtitle = '40237';
  const tag = React.DOM.h2;

  it('should contain title text', function *() {
    expect(Title({title, subtitle, tag}), 'to contain', title);
  });

  it('should not render subtitle if subtitle is not set', function *() {
    const titleElement = Title({title, tag});

    const subtitleElements = ShallowTestUtils.findAllWithClass(titleElement, 'fc-section-title__subtitle');
    expect(subtitleElements).to.be.empty;
    expect(titleElement.props.children).to.contain(title);
  });

  it('should render subtitle if subtitle is set', function *() {
    const titleElement = Title({title, subtitle, tag});
    const subtitleElements = ShallowTestUtils.findWithClass(titleElement, 'fc-section-title__subtitle');

    expect(subtitleElements.props.children).to.contain(subtitle);
  });

});
