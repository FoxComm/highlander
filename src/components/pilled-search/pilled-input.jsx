
import _ from 'lodash';
import classNames from 'classnames';
import React, { PropTypes } from 'react';

const formatPill = (pill, idx, props) => {
  return (
    <div
      className="fc-pilled-input__pill"
      key={`pill-${idx}`}
      onClick={() => props.onPillClick(pill, idx)}>
      {pill}
      <a onClick={() => props.onPillClose(pill, idx)}
        className="fc-pilled-search__pill-close">
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

  const { pills = [], ...rest } = props;

  return (
    <div className={classNames('fc-pilled-input', props.className)}>
      <div className="fc-pilled-input__input-container">
        <div className="fc-pilled-input__pills-wrapper">
          {pills.map((pill, idx) => {
            return props.formatPill(pill, idx, props);
          })}
        </div>
        {iconWrapper(props.icon)}
        <div className="fc-pilled-input__input-wrapper">
          <input
            className="fc-pilled-input__input-field"
            type="text"
            {...rest} />
        </div>
      </div>
      {buttonsContainer(props.button)}
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
};

PilledInput.defaultProps = {
  onPillClose: _.noop,
  onPillClick: _.noop,
  formatPill,
  icon: 'search',
};

export default PilledInput;
