import React, { PropTypes } from 'react';

const RadioButton = props => {
  const { isSelected, children, ...rest } = props;

  return (
    <div className='fc-form-field'>
      <input type='radio' {...rest} />
      {children}
    </div>
  );
};

RadioButton.propTypes = {
  isSelected: PropTypes.bool,
  children: PropTypes.node
};

export default RadioButton;
