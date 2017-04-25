//libs
import _ from 'lodash';
import { connect } from 'react-redux';

//components
import { Input, getDefault, isValid } from '../inputs/lookup';
import { Label } from '../labels/lookup';

const connected = getState => connect(state => ({
  data: _.mapValues(getState(state), ({id,title}) => ({id, label: title}))
}));

export default function(path) {
  return {
    Input: connected(state => _.get(state, `${path}`))(Input),
    getDefault,
    isValid,
    Label: connected(state => _.get(state, `${path}`))(Label),
  };
}
