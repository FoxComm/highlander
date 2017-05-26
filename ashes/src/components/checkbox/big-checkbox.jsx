import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import _ from 'lodash';

import { Checkbox } from './checkbox';

const BigCheckbox = props => {
  const checkboxClass = classNames('fc-big-checkbox__visible-box', {
    '_checked': props.value,
  });

  return (
    <div className="fc-big-checkbox">
      <div className={checkboxClass} onClick={() => props.onToggle(!props.value)} />
      <Checkbox className="fc-big-checkbox__hidden-input" id={props.id} name={props.name} value={props.value} />
    </div>
  );
};

BigCheckbox.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string,
  name: PropTypes.string,
  onToggle: PropTypes.func,
  value: PropTypes.bool,
};

BigCheckbox.defaultProps = {
  label: '',
  onToggle: _.noop,
  value: false,
};

export default BigCheckbox;
