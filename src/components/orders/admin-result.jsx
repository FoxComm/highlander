import React, { PropTypes } from 'react';

const AdminResult = props => {
  const { email, name } = props.model;
  return (
    <div className="fc-grid">
      <div className="fc-col-md-1-2">{name}</div>
      <div className="fc-col-md-1-2">{email}</div>
    </div>
  );
};

AdminResult.propTypes = {
  model: PropTypes.object,
};

export default AdminResult;
