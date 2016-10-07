import React, { PropTypes } from 'react';

function getStyles(props) {
  return {
    transform: `translateX(${props.left}px)`,
    width: props.width
  };
}

const InkBar = props => {
  return (
    <div className="fc-ink-bar" style={getStyles(props)}></div>
  );
};

InkBar.propTypes = {
  left: PropTypes.number.isRequired,
  width: PropTypes.number.isRequired,
};

export default InkBar;
