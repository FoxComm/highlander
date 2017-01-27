/**
 * @flow
 */

import React, { Element, PropTypes } from 'react';

type Props = {
  children?: Element,
  onClickUp: () => void,
  onClickDown: () => void,
};

const DateTimeCounter = (props: Props): Element => {
  const { children, onClickUp, onClickDown } = props;
  return (
    <div className="fc-date-time-picker__counter">
      <button className="fc-date-time-picker__counter-button" onClick={onClickUp}>
        <i className="icon-up" />
      </button>
      {children}
      <button className="fc-date-time-picker__counter-button" onClick={onClickDown}>
        <i className="icon-down" />
      </button>
    </div>
  );
};

DateTimeCounter.propTypes = {
  children: PropTypes.node.isRequired,
  onClickUp: PropTypes.func.isRequired,
  onClickDown: PropTypes.func.isRequired,
};

export default DateTimeCounter;
