/** Libs */
import React, { Component, PropTypes } from 'react';

/**
 * User initials(avatar) box component
 */
const UserInitials = props => {
  return (
    <div className="initials">{getInitials(props)}</div>
  );
};

function getInitials({ name, firstName = '', lastName = '' }) {

  if (!firstName && !lastName) {
    if (name) {
      [firstName, lastName] = name.split(/\s+/);
    } else {
      return null;
    }
  }

  return `${firstName.charAt(0)}${lastName.charAt(0)}`;
}

/**
 * UserInitials component expected props types
 */
UserInitials.propTypes = {
  firstName: PropTypes.string,
  lastName: PropTypes.string,
  name: PropTypes.string
};

export default UserInitials;
