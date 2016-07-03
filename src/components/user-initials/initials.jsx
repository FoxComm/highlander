/** Libs */
import React, { Component, PropTypes } from 'react';

/**
 * User initials(avatar) box component
 */
const UserInitials = props => {
  return (
    <div className="initials" style={getColor(props)}>{getInitials(props)}</div>
  );
};

function getColor(props) {
  const initials = getInitials(props) || '';
  const code = initials.charCodeAt(1);

  console.log(code);
  const color = (() => {
    if (code <= 70) return '#B989DE';
    if (code > 70 && code <= 75) return '#7DA0F3';
    if (code > 75 && code <= 80) return '#F45758';
    if (code > 80 && code <= 85) return '#DD63B9';
    if (code > 85 && code <= 90) return '#47628D';
    return '#FFBB3C';
  })();

  return {
    background: color
  };
}

function getInitials({ name, firstName = '', lastName = '' }) {

  if (!firstName && !lastName) {
    if (name) {
      [firstName, lastName] = name.split(/\s+/);
    } else {
      return null;
    }
  }
  if (!lastName) {
    return firstName.charAt(0);
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
