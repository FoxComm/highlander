
import React, { PropTypes } from 'react';

export default class Alert extends React.Component {
  static propTypes = {
    type: PropTypes.oneOf([
      'error', 'success', 'warning'
    ]).isRequired
  };


  render() {
    return (
      <div className={`fc-alert is-alert-${this.props.type}`}>
        <i className={`icon-${this.props.type}`}></i>
        {this.props.children}
      </div>
    );
  }
}