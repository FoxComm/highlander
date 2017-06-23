/* @flow */

// libs
import React from 'react';
import classNames from 'classnames';

// components
import Icon from 'components/core/icon';

type Props = {
  icon: string,
  direction: 'append' | 'prepend',
  inputClass: string,
  inputName: string,
  inputType: string,
  plate: string,
  value: string | number,
  inputValuePretty: string,
  className: string,
};

const PlatedInput = (props: Props) => {
  const {
    icon,
    plate,
    inputClass,
    className,
    inputName,
    inputType,
    direction,
    value,
    inputValuePretty,
    ...rest
  } = props;

  const baseClass = `fc-${direction}-input`;

  const vInputClass = classNames(`${baseClass}__input-field`, inputClass);
  const vInputName = inputValuePretty ? `${inputName}Pretty` : inputName;
  const vInputValue = inputValuePretty || value;

  const visibleInput = (
    <input className={vInputClass} name={vInputName} type={inputType} value={vInputValue} {...rest} />
  );

  const hiddenInput = inputValuePretty ? <input name={inputName} type="hidden" value={value} /> : null;

  const inputs = [hiddenInput, visibleInput];
  const plateInner = plate !== void 0 ? <span>{plate}</span> : <Icon name={icon} />;
  const plateWrapper = (
    <div className={`${baseClass}__icon`} key="wrapper">
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

PlatedInput.defaultTypes = {
  inputClass: '',
  inputType: 'text',
  direction: 'prepend',
  inputValuePretty: null,
};

export default PlatedInput;
