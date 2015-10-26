'use strict';

import React from 'react';
import TestUtils from 'react-addons-test-utils';
import ReactDOM from 'react-dom';

describe.only('ContentBox', function() {
  let ContentBox = requireComponent('content-box/content-box.jsx');

  let contentBox;

  afterEach(function() {
    if (contentBox) {
      contentBox.unmount();
      contentBox = null;
    }
  });

  it('should render container with correct title', function *() {
    const title = 'Customer Info';
    contentBox = shallowRender(
      <ContentBox title={ title } className="" />
    );

    expect(contentBox, 'to contain',
      <div className="fc-col-md-2-3 fc-title">{title}</div>
    );
  });

  it('should render container with correct class', function *() {
    const className = 'test-class';
    contentBox = shallowRender(
      <ContentBox title="" className={ className } />
    );

    expect(contentBox.props.className).to.be.equal(`${className} fc-content-box`);
  });

  it('should render container with action block when provided', function *() {
    const actionBlock = 'Actions!';
    contentBox = shallowRender(
      <ContentBox title="" className="" actionBlock={ actionBlock }/>
    );

    expect(contentBox, 'to contain',
      <div className="fc-col-md-1-3 fc-controls">
        {actionBlock}
      </div>
    );
  });

  it('should not render action block by default', function *() {
    contentBox = shallowRender(
      <ContentBox title="" className="" />
    );

    expect(contentBox, 'to contain exactly',
      <div className="fc-col-md-1-3 fc-controls"></div>
    );
  });
});
