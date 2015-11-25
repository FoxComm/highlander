import React, { PropTypes } from 'react';

function formatValue(value, base) {
  const val = (value/base).toFixed(2);
  const fract = val.slice(-2);
  if (fract == '00') {
    return val.slice(0, -3);
  }
  return val;
}

const CurrencyInput = (props) => {
  const value = formatValue(props.value, props.base);
  return (
    <div className="fc-input-group">
      <div className="fc-input-prepend"><i className="icon-usd"/></div>
      <input onChange={(e) => {
                const value = e.target.value;
                if (props.onChange) props.onChange(value * props.base);
                }} type="number"
             value={value} defaultValue={props.defaultValue} step={props.step} min={props.min}/>
    </div>
  )
};

CurrencyInput.propTypes = {
  base: PropTypes.number,
  min: PropTypes.number,
  value: PropTypes.number,
  step: PropTypes.number,
  onChange: PropTypes.func,
  defaultValue: PropTypes.number
};

CurrencyInput.defaultProps = {
  base: 100,
  value: 0,
  min: 0,
  step: 1
};

export default CurrencyInput;
