import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

const AppendInput = props => {
  const { 
    icon, inputClass, inputName, inputType, inputValue, inputValuePretty, ...rest 
  } = props;

  const vInputClass = classNames('fc-append-input__input-field', inputClass);
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
    <div className="fc-append-input">
      {hiddenInput}
      {visibleInput}
      <div className="fc-append-input__icon">
        <i className={`icon-${icon}`} />
      </div>
    </div>
  );
};

AppendInput.propTypes = {
  icon: PropTypes.string.isRequired,
  inputClass: PropTypes.string,
  inputName: PropTypes.string.isRequired,
  inputType: PropTypes.string,
  inputValue: PropTypes.string.isRequired,
  inputValuePretty: PropTypes.string
};

AppendInput.defaultTypes = {
  inputClass: '',
  inputType: 'text',
  inputValuePretty: null
};

export default AppendInput;
