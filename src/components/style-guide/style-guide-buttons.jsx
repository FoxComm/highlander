'use strict';

import React from 'react';

const StyleGuideButtons = props => {
  return (
    <div>
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <h3>Default Button</h3>
          <button className="fc-btn">Default</button>
        </div>
      </div>
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <h3>Primary Button</h3>
          <button className="fc-btn fc-btn-primary">Default</button>
        </div>
      </div>
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <h3>Secondary Button</h3>
          <button className="fc-btn fc-btn-secondary">Default</button>
        </div>
      </div>
    </div>
  );
};

export default StyleGuideButtons;
