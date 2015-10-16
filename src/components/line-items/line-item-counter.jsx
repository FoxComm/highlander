'use strict';

import React from 'react';
import Counter from '../forms/counter';
import ConfirmModal from '../modal/confirm';
import LineItemActions from '../../actions/line-items';

const confirmOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to delete this item?',
  cancel: 'Cancel',
  proceed: 'Yes, Delete'
};

let LineItemCounter = (props) => {
  return (
    <Counter
      defaultValue='quantity'
      stepAmount={1}
      minValue={0}
      maxValue={1000000}
      model={props.model}
      stepUp={props.stepUp}
      stepDown={props.stepDown} />
  );
}

export default LineItemCounter;
