import React, { PropTypes } from 'react';

const initials = (user) => {
  const [firstName, lastName] = user.name.split(' ');
  return `${firstName.charAt(0)}${lastName.charAt(0)}`;
};

const tooltip = (user) => {
  return (
    <div className="fc-tooltip fc-tooltip-left">
      <div className="fc-strong">{ user.name }</div>
      {user.email && (<div>{ user.email }</div>)}
    </div>
  );
};

const UserInitials = (props) => {
  return (
    <div className="initials fc-with-tooltip">
      { initials(props.model) }
      { tooltip(props.model) }
    </div>
  );
};

UserInitials.propTypes = {
  model: PropTypes.object
};

export default UserInitials;
