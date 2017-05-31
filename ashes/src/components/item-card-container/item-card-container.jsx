
import React from 'react';
import PropTypes from 'prop-types';

export default class ItemCardContainer extends React.Component {

  static propTypes = {
    className: PropTypes.string,
    leftControls: PropTypes.node,
    rightControls: PropTypes.node,
    children: PropTypes.node,
    chooseControl: PropTypes.node,
  };

  get cartClassName() {
    return `fc-card-container ${this.props.className}`;
  }

  get chooseControl() {
    if (this.props.chooseControl) {
      return (
        <div className="fc-card-container__choose-control">
          {this.props.chooseControl}
        </div>
      );
    }
  }

  render() {
    return (
      <li className={ this.cartClassName }>
        <div className="fc-card-container__controls">
          { this.props.leftControls }
          { this.props.rightControls }
        </div>
        <div className="fc-card-container-content">
          { this.props.children }
        </div>
        {this.chooseControl}
      </li>
    );
  }
}
