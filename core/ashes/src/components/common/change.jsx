/** Libs */
import classNames from 'classnames';
import React, { PropTypes } from 'react';

/** Change component to render colored number changes */
const Change = props => {
  const { value } = props;

  const cls = classNames('fc-change', {
    '_positive': value > 0,
    '_negative': value < 0
  });

  return <span className={cls}>{Math.abs(value)}</span>;
};

Change.propTypes = {
  value: PropTypes.number.isRequired
};

export default Change;
