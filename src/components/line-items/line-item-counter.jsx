import React, { PropTypes } from 'react';
import Counter from '../forms/counter';
import ConfirmModal from '../modal/confirm';
import LineItemActions from '../../actions/line-items';

const LineItemCounter = props => {
  return (
    <Counter
      defaultValue='quantity'
      step={1}
      min={0}
      max={1000000}/>
  );
};

LineItemCounter.propTypes = {
  model: PropTypes.object,
  stepUp: PropTypes.number,
  stepDown: PropTypes.number
};

export default LineItemCounter;
