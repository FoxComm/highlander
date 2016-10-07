import React, { PropTypes } from 'react';


const EmptyText = ({label}) => {
  return (
    <span className="fc-content-box__empty-text">
      {label}
    </span>
  );
};

EmptyText.propTypes = {
  label: PropTypes.string.isRequired,
};

export default EmptyText;
