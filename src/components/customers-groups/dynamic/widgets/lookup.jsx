//libs
import _ from 'lodash';
import { connect } from 'react-redux';

//components
import { Input, getDefault } from '../inputs/lookup';
import { Label } from '../labels/lookup';


const connected = getState => connect(state => ({
  data: _.chain(getState(state)).values().map(({id,name}) => ({id, label: name})).value(),
}));

export default function(type) {
  return {
    Input: connected(state => state.regions)(Input),
    getDefault,
    Label: connected(state => state.regions)(Label),
  };
}
