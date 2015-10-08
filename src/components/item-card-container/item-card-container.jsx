'use strict';

import React, { PropTypes } from 'react';

export default class CreditCardBox extends React.Component {

  static propTypes = {
    className: PropTypes.string,
    leftControls: PropTypes.node,
    rightControls: PropTypes.node,
    children: PropTypes.node
  }

  render() {
    let cartClassName = `fc-card-container ${this.props.className}`;
    return (
      <li className="fc-card-container">
        <div className="fc-card-container-controls fc-grid">
          <div className="fc-col-2-3">
            { this.props.leftControls }
          </div>
          <div className="fc-col-1-3">
            { this.props.rightControls }
          </div>
        </div>
        <div className="fc-card-container-content">
          { this.props.children }
        </div>
      </li>
    );
  }
}
