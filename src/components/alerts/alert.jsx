
import React, { PropTypes } from 'react';

export default class Alert extends React.Component {

  static ERROR = 'error';
  static WARNING = 'warning';
  static SUCCESS = 'success';

  static propTypes = {
    type: PropTypes.oneOf([
      Alert.ERROR, Alert.WARNING, Alert.SUCCESS
    ]).isRequired,
    children: PropTypes.node
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