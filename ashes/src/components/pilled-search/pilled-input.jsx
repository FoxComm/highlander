// libs
import _ from 'lodash';
import classNames from 'classnames';
import React from 'react';
import PropTypes from 'prop-types';

// components
import Icon from 'components/core/icon';

// paragons
import { INPUT_ATTRS } from 'paragons/common';

// components
import TextInput from 'components/core/text-input';
import { RoundedPill } from 'components/core/rounded-pill';

// styles
import s from './pilled-input.css';

// These aren't actually multiple exported components, but ESLint mistakenly
// thinks that they are.

/* eslint-disable react/no-multi-comp */

const formatPill = (pill, idx, props) => {
  return (
    <RoundedPill
      key={`pill-${idx}`}
      value={idx}
      onClick={() => props.onPillClick(pill, idx)}
      onClose={() => props.onPillClose(pill, idx)}
      className={s.pill}
    >
      {pill.display || pill}
    </RoundedPill>
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
    const cls = classNames('fc-pilled-input__icon-wrapper', {
      _clickable: onIconClick !== PilledInput.defaultProps.onIconClick,
    });

    return (
      <div className={cls} onClick={onIconClick}>
        <Icon name={icon} />
      </div>
    );
  }
};

const PilledInput = props => {
  const { controls, children, className, icon, pills = [], solid, disabled, onIconClick, ...rest } = props;

  const containerClass = classNames('fc-pilled-input__input-container', {
    _solid: solid,
  });

  const inputClass = classNames('fc-pilled-input__input-field', '_no-fc-behaviour', {
    '_solid-input': solid,
  });

  const attrs = _.pick(rest, INPUT_ATTRS);

  const input =
    children || <TextInput className={inputClass} autoFocus={props.autoFocus} disabled={disabled} {...attrs} />;

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
