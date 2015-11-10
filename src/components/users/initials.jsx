'use strict';

import React, { PropTypes } from 'react';

export default class UserInitials extends React.Component {
  initials() {
    let user = this.props.model;
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`;
  }

  tooltip() {
    let user = this.props.model;
    return (
      <div className="fc-tooltip fc-tooltip-left">
        <div className="fc-strong">{`${user.firstName} ${user.lastName}`}</div>
        {user.email && (<div>{`${user.email}`}</div>)}
      </div>
    );
  }

  render() {
    return (
      <div className="initials fc-with-tooltip">
        {this.initials()}
        {this.tooltip()}
      </div>
    );
  }
}

UserInitials.propTypes = {
  model: PropTypes.object
};
