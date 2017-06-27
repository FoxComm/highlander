//libs
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import { autobind, debounce } from 'core-decorators';

//helpers
import { prefix } from 'lib/text-utils';

//components
import Typeahead from 'components/typeahead/typeahead';
import PilledInput from 'components/pilled-search/pilled-input';
import propTypes from '../widgets/propTypes';
import styles from './typeahead.css';

const TypeaheadRow = (props) => {
  const { name } = props.model;
  return (
    <div styleName="item">
      <div styleName="item-name">
        {name}
      </div>
    </div>
  );
};


export class Input extends Component {

  static propTypes = {
    ...propTypes,
    isFetchingProducts: PropTypes.bool,
    data: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.any,
      name: PropTypes.string,
    })),
  };

  constructor(props) {
    super(props);
    this.state = {
      term: '',
      focused: false,
    };
  }

  @autobind
  pilledInput() {
    const { state, props } = this;
    const pills = (props.value == '' || !props.value) ? [] : [props.value];

    return (
      <PilledInput
        solid={true}
        value={state.term}
        disabled={this.props.value != '' && this.props.value != null}
        onChange={({target}) => this.setTerm(target.value)}
        pills={pills}
        icon={null}
        onPillClose={(name, index) => this.deselectItem(index)}
      />
    );
  }

  @autobind
  setTerm(term) {
    this.suggestItems(term);
    this.setState({
      term,
      focused: true,
    });
  }

  @debounce(400)
  suggestItems(value){
    if (value.length < 3) return null;
    return this.props.suggestItems(value);
  }

  get suggestedList(){
    const suggestions = _.filter(this.props.data, (item) => {
      return _.includes(item.name.toLowerCase(), this.state.term.toLowerCase());
    });
    return suggestions;
  }

  @autobind
  deselectItem(index) {
    this.props.changeValue('');
  }

  @autobind
  handleSelectItem(item, event) {
    this.setState({
      term: '',
      focused: false,
    });
    this.props.changeValue(item.name);

  }

  @autobind
  updateFocus(item, event) {
    this.setState({
      focused: false,
    });
  }

  render () {
    const prefixed = prefix(this.props.className);
    const value = this.props.value;
    const item = _.find(this.props.data, {name: value});
    const isFetching = ((this.props.value == ''
      || this.props.value == null) && this.state.focused) ? this.props.isFetchingProducts : false;
    return (
      <Typeahead
        styleName={'typeahead'}
        component={TypeaheadRow}
        hideOnBlur={true}
        items={isFetching ? [] : this.props.data}
        isFetching={isFetching}
        name="queryBuilderTypeahead"
        onBlur={this.updateFocus}
        hideOnBlur={true}
        minQueryLength={3}
        inputElement={this.pilledInput()}
        onItemSelected={this.handleSelectItem}
        placeholder={'Search..'} />
    );
  }
}


export const getDefault = () => '';

export const isValid = value => Boolean(value);
