import React from 'react';

const RadioButton = props => {
  const { isSelected, children, ...rest } = props;

  return (
    <div className='fc-form-field'>
      <input type='radio' {...rest} />
      {children}
    </div>
  );
};

export default RadioButton;
