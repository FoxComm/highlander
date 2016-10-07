import React, { PropTypes } from 'react';

const RadioButton = props => {
  const { children, ...rest } = props;

  return (
    <div className="fc-form-field fc-radio">
      <input type="radio" {...rest} />
      {children}
    </div>
  );
};

RadioButton.propTypes = {
  children: PropTypes.node
};

export default RadioButton;
