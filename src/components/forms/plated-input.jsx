import React, { PropTypes } from 'react';
import classNames from 'classnames';

const PlatedInput = props => {
  const {
    icon, plate, inputClass, className, inputName, inputType, direction, value, inputValuePretty, ...rest
  } = props;

  const baseClass = `fc-${direction}-input`;

  const vInputClass = classNames(`${baseClass}__input-field`, inputClass);
  const vInputName = inputValuePretty ? `${inputName}Pretty` : inputName;
  const vInputValue = inputValuePretty || value;

  const visibleInput = (
    <input
      className={vInputClass}
      name={vInputName}
      type={inputType}
      value={vInputValue}
      {...rest} />
  );

  const hiddenInput = inputValuePretty
    ? <input name={inputName} type="hidden" value={value} />
    : null;


  const inputs = [hiddenInput, visibleInput];
  const plateInner = plate !== void 0 ? <span>{plate}</span> : <i className={`icon-${icon}`} />;
  const plateWrapper = (
    <div className={`${baseClass}__icon`}>
      {plateInner}
    </div>
  );

  const content = direction == 'append' ? [inputs, plateWrapper] : [plateWrapper, inputs];

  return (
    <div className={classNames(baseClass, className)}>
      {content}
    </div>
  );
};

PlatedInput.propTypes = {
  icon: PropTypes.string.isRequired,
  direction: PropTypes.oneOf(['append', 'prepend']),
  inputClass: PropTypes.string,
  inputName: PropTypes.string.isRequired,
  inputType: PropTypes.string,
  plate: PropTypes.node,
  value: PropTypes.string.isRequired,
  inputValuePretty: PropTypes.string,
  className: PropTypes.string,
};

PlatedInput.defaultTypes = {
  inputClass: '',
  inputType: 'text',
  direction: 'prepend',
  inputValuePretty: null
};

export default PlatedInput;
