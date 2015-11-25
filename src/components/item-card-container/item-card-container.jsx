
import React, { PropTypes } from 'react';

export default class ItemCardContainer extends React.Component {

  static propTypes = {
    className: PropTypes.string,
    leftControls: PropTypes.node,
    rightControls: PropTypes.node,
    children: PropTypes.node
  };

  get cartClassName() {
    return `fc-card-container ${this.props.className}`;
  }

  render() {
    return (
      <li className={ this.cartClassName }>
        <div>
          { this.props.leftControls }
        </div>
        <div className="fc-address-controls">
          { this.props.rightControls }
        </div>
        <div className="fc-card-container-content">
          { this.props.children }
        </div>
      </li>
    );
  }
}
