import React, { PropTypes } from 'react';

const UserInitials = props => {
  let {firstName, lastName, name, email} = props;
  if (!firstName && !lastName) {
    if (name) {
      [firstName, lastName] = name.split(/\s+/);
    } else {
      throw new Error('UserInitials: at least firstName,lastName or name are required');
    }
  }

  const fullName = name || `${firstName} ${lastName}`;

  return (
    <div className="initials fc-with-tooltip">
      { `${firstName.charAt(0)}${lastName.charAt(0)}` }
      <div className="fc-tooltip fc-tooltip-left">
        <div className="fc-strong">{ fullName }</div>
        {email && (<div>{ email }</div>)}
      </div>
    </div>
  );
};

UserInitials.propTypes = {
  firstName: PropTypes.string,
  lastName: PropTypes.string,
  name: PropTypes.string,
  email: PropTypes.string,
};

export default UserInitials;
