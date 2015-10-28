'use strict';

import React, { PropTypes } from 'react';

export default class CreditCardBox extends React.Component {

  static propTypes = {
    className: PropTypes.string,
    leftControls: PropTypes.node,
    rightControls: PropTypes.node,
    children: PropTypes.node
  }

  get cartClassName() {
    return `fc-card-container ${this.props.className}`;
  }

  render() {
    return (
      <li className={ this.cartClassName }>
        <div className="fc-card-container-controls fc-grid">
          <div className="fc-col-md-2-3">
            { this.props.leftControls }
          </div>
          <div className="fc-col-md-1-3">
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
