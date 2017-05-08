//libs
import _ from 'lodash';
import { connect } from 'react-redux';

//components
import { Input, getDefault, isValid } from '../inputs/typeahead';


const connected = (getState, asyncPath, mapActions) => connect(state => ({
  data: getState(state).map(item => {
    const name = item.name ? item.name : item.title;
    return {id: item.id, name: name };
  }),
  isFetchingProducts: _.get(state.asyncActions, `${asyncPath}`, false),
}),mapActions);

export default function(path,asyncPath,mapActions) {
  return {
    Input: connected(state => _.toArray(_.get(state, `${path}`)), asyncPath, mapActions)(Input),
    getDefault,
    isValid,
  };
}
