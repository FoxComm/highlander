import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

const PrependInput = props => {
  const { 
    icon, inputClass, inputName, inputType, inputValue, inputValuePretty, ...rest 
  } = props;

  const vInputClass = classNames('fc-prepend-input__input-field', inputClass);
  const vInputName = inputValuePretty ? `${inputName}Pretty` : inputName;
  const vInputValue = inputValuePretty || inputValue;

  const visibleInput = (
    <input 
      className={vInputClass}
      name={vInputName}
      type={inputType}
      value={vInputValue}
      {...rest} />
  );

  const hiddenInput = inputValuePretty
    ? <input name={inputName} type="hidden" value={inputValue} />
    : null;

  return (
    <div className="fc-prepend-input">
      <div className="fc-prepend-input__icon">
        <i className={`icon-${icon}`} />
      </div>
      {hiddenInput}
      {visibleInput}
    </div>
  );
};

PrependInput.propTypes = {
  icon: PropTypes.string.isRequired,
  inputClass: PropTypes.string,
  inputName: PropTypes.string.isRequired,
  inputType: PropTypes.string,
  inputValue: PropTypes.string.isRequired,
  inputValuePretty: PropTypes.string
};

PrependInput.defaultTypes = {
  inputClass: '',
  inputType: 'text',
  inputValuePretty: null
};

export default PrependInput;
