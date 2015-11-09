'use strict';

import React, { PropTypes } from 'react';
import Counter from '../forms/counter';
import ConfirmModal from '../modal/confirm';
import LineItemActions from '../../actions/line-items';

const LineItemCounter = props => {
  return (
    <Counter
      defaultValue='quantity'
      stepAmount={1}
      minValue={0}
      maxValue={1000000}
      model={props.model}
      stepUp={props.stepUp}
      stepDown={props.stepDown}/>
  );
};

LineItemCounter.propTypes = {
  model: PropTypes.object,
  stepUp: PropTypes.number,
  stepDown: PropTypes.number
};

export default LineItemCounter;
