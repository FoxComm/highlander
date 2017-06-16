/** Libs */
import React from 'react';
import PropTypes from 'prop-types';

/**
 * User initials(avatar) box component
 */
const UserInitials = (props) => {
  return (
    <div className="initials" style={getColor(props)}>{getInitials(props)}</div>
  );
};

function getColor(props) {
  const initials = getInitials(props) || '';
  const code = initials.charCodeAt(1);

  let color = '#FFBB3C';
  if (code <= 70) color = '#B989DE';
  else if (code <= 75) color = '#7DA0F3';
  else if (code <= 80) color = '#F45758';
  else if (code <= 85) color = '#DD63B9';
  else if (code <= 90) color = '#47628D';

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
