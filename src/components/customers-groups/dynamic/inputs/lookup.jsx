//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

//components
import { LookupDropdown } from '../../../lookup';


const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  data: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.any,
    label: PropTypes.string,
  })),
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func,
};

const Input = ({data, value, prefixed, changeValue}) => {
  const item = _.find(data, ({label}) => label === value);

  return (
    <LookupDropdown className={prefixed('lookup')}
                    data={data}
                    value={value && item ? item.id : null}
                    minQueryLength={3}
                    onSelect={({label}) => changeValue(label)} />
  );
};
Input.propTypes = propTypes;

const Label = ({value, prefixed}) => {
  return (
    <div className={prefixed('value')}>
      {value}
    </div>
  );
};
Label.propTypes = propTypes;


const connected = getState => connect(state => ({
  data: _.chain(getState(state)).values().map(({id,name}) => ({id, label: name})).value(),
}));

export default function(type) {
  return {
    Input: connected(state => state.regions)(Input),
    Label: connected(state => state.regions)(Label)
  };
}
