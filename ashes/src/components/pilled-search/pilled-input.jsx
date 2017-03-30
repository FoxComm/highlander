import _ from 'lodash';
import classNames from 'classnames';
import React, { PropTypes } from 'react';

// These aren't actually multiple exported components, but ESLint mistakenly
// thinks that they are.

/* eslint-disable react/no-multi-comp */

const formatPill = (pill, idx, props) => {
  const cls = classNames('fc-pilled-input__pill', {
    'fc-pilled-input__pill_clickable': !!props.onPillClick,
  });

  return (
    <div
      className={cls}
      key={`pill-${idx}`}
      onClick={e => props.onPillClick(pill, idx, e)}>
      <span className="fc-pilled-input__pill-value">{pill}</span>
      <a onClick={() => props.onPillClose(pill, idx)}
         className="fc-pilled-input__pill-close">
        &times;
      </a>
    </div>
  );
};

const controlsContainer = controls => {
  if (controls) {
    return (
      <div className="fc-pilled-input__controls-container">
        {controls}
      </div>
    );
  }
};

const iconWrapper = (icon, onIconClick) => {
  if (icon) {
    return (
      <div className="fc-pilled-input__icon-wrapper" onClick={onIconClick}>
        <i className={`icon-${icon}`} />
      </div>
    );
  }
};

const PilledInput = props => {

  const { controls, children, className, icon, pills = [], solid, disabled, onIconClick, ...rest } = props;

  const containerClass = classNames('fc-pilled-input__input-container', {
    '_solid': solid
  });

  const inputClass = classNames('fc-pilled-input__input-field', '_no-fc-behaviour', {
    '_solid-input': solid
  });

  const input = children || (
      <input
        className={inputClass}
        type="text"
        autoFocus={props.autoFocus}
        disabled={disabled}
        {...rest} />
    );

  return (
    <div className={classNames('fc-pilled-input', className)}>
      <div className={containerClass}>
        <div className="fc-pilled-input__pills-wrapper">
          {pills.map((pill, idx) => {
            return props.formatPill(pill, idx, props);
          })}
          <div className="fc-pilled-input__input-wrapper">
            {input}
            {iconWrapper(icon, onIconClick)}
          </div>
        </div>
        {controlsContainer(controls)}
      </div>
    </div>
  );
};

PilledInput.propTypes = {
  children: PropTypes.node,
  onPillClose: PropTypes.func,
  onPillClick: PropTypes.func,
  onIconClick: PropTypes.func,
  formatPill: PropTypes.func,
  pills: PropTypes.array,
  icon: PropTypes.string,
  controls: PropTypes.node,
  className: PropTypes.string,
  autoFocus: PropTypes.bool,
  solid: PropTypes.bool,
  disabled: PropTypes.bool,
};

PilledInput.defaultProps = {
  onPillClose: _.noop,
  onPillClick: _.noop,
  onIconClick: _.noop,
  formatPill,
  icon: 'search',
  inputMask: '',
  autoFocus: false,
  solid: false,
  disabled: false,
};

export default PilledInput;
