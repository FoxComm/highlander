'use strict';

import React from 'react';

export default class UserInitials extends React.Component {
  initials() {
    let user = this.props.model;
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`;
  }

  tooltip() {
    let user = this.props.model;
    return `${user.firstName} ${user.lastName}\n${user.email}`;
  }
  render() {
    return <div className="initials tooltip-bottom" data-tooltip={this.tooltip()}>{this.initials()}</div>;
  }
}

UserInitials.propTypes = {
  model: React.PropTypes.object
};
