//libs
import _ from 'lodash';
import { connect } from 'react-redux';

//components
import { Input, getDefault, isValid } from '../inputs/lookup';
import { Label } from '../labels/lookup';

const connected = getState => connect(state => ({
  data: _.mapValues(getState(state), ({id,name}) => ({id, label: name}))
}));

export default function() {
  return {
    Input: connected(state => state.regions)(Input),
    getDefault,
    isValid,
    Label: connected(state => state.regions)(Label),
  };
}
