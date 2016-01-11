
import _ from 'lodash';
import classNames from 'classnames';
import React, { PropTypes } from 'react';
import InputMask from 'react-input-mask';
import MaskedInput from '../masked-input/masked-input';

// These aren't actually multiple exported components, but ESLint mistakenly
// thinks that they are.

/* eslint-disable react/no-multi-comp */

const formatPill = (pill, idx, props) => {
  return (
    <div
      className="fc-pilled-input__pill"
      key={`pill-${idx}`}
      onClick={() => props.onPillClick(pill, idx)}>
      {pill}
      <a onClick={() => props.onPillClose(pill, idx)}
        className="fc-pilled-input__pill-close">
        &times;
      </a>
    </div>
  );
};

const buttonsContainer = button => {
  if (button) {
    return (
      <div className="fc-pilled-input__btn-container">
        {button}
      </div>
    );
  }
};

const iconWrapper = icon => {
  if (icon) {
    return (
      <div className="fc-pilled-input__icon-wrapper">
        <i className={`icon-${icon}`}></i>
      </div>
    );
  }
};

const PilledInput = props => {

  const { button, className, icon, inputMask, pills = [], ...rest } = props;

  const input = (
    <MaskedInput
      className="fc-pilled-input__input-field _no-fc-behavior"
      type="text"
      mask={_.isEmpty(inputMask) ? '' : inputMask}
      {...rest} />
  );

  return (
    <div className={classNames('fc-pilled-input', className)}>
      <div className="fc-pilled-input__input-container">
        <div className="fc-pilled-input__pills-wrapper">
          {pills.map((pill, idx) => {
            return props.formatPill(pill, idx, props);
          })}
        </div>
        {iconWrapper(icon)}
        <div className="fc-pilled-input__input-wrapper">
          {input}
        </div>
      </div>
      {buttonsContainer(button)}
    </div>
  );
};

PilledInput.propTypes = {
  onPillClose: PropTypes.func,
  onPillClick: PropTypes.func,
  formatPill: PropTypes.func,
  pills: PropTypes.array,
  icon: PropTypes.string,
  button: PropTypes.node,
  className: PropTypes.string,
  inputMask: PropTypes.string,
};

PilledInput.defaultProps = {
  onPillClose: _.noop,
  onPillClick: _.noop,
  formatPill,
  icon: 'search',
  inputMask: 'Order : Tot\\al : = : $99.99',
};

export default PilledInput;
