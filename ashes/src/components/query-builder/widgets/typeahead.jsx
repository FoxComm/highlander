//libs
import _ from 'lodash';
import { connect } from 'react-redux';

//components
import { Input, getDefault, isValid } from '../inputs/typeahead';

//modules
import { suggestProducts } from 'modules/products/suggest';

const mapActions = { suggestProducts };

const connected = (getState, asyncPath) => connect(state => ({
  data: getState(state).map(item => {return {id: item.id, name: item.title};}),
  isFetchingProducts: _.get(state.asyncActions, `${asyncPath}`, false),
}),mapActions);

export default function(path,asyncPath) {
  return {
    Input: connected(state => _.toArray(_.get(state, `${path}`)),asyncPath)(Input),
    getDefault,
    isValid,
  };
}
