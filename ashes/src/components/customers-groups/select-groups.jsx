/* @flow weak */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import styles from './select-groups.css';

import RadioButton from 'components/core/radio-button';
import Typeahead from '../typeahead/typeahead';
import PilledInput from '../pilled-search/pilled-input';
import CustomerGroupRow from './customer-group-row';

import { actions } from 'modules/customer-groups/list';

type GroupType = {
  name: string,
  type: string,
  id: number,
};

type Props = {
  updateSelectedIds: (groups: Array<number>) => any;
  groups: Array<GroupType>;
  selectedGroupIds: Array<number>;
  qualifyAll: boolean,
  fetch: () => Promise<*>,
  parent: string,
  qualifyAllChange: Function,
};

type State = {
  term: string,
};


class SelectCustomerGroups extends Component {
  props: Props;

  static defaultProps = {
    parent: '',
  };

  state: State = {
    term: ''
  };

  componentDidMount() {
    this.props.fetch();
  }

  @autobind
  handleChangeQualifier({target}: Object) {
    const isAllQualify = target.getAttribute('name') == 'qualifyAll';
    this.props.qualifyAllChange(isAllQualify);
  }

  get suggestedGroups(){
    const suggestions = _.filter(this.props.groups, (item) => {
      return _.includes(item.name.toLowerCase(), this.state.term.toLowerCase());
    });
    return suggestions;
  }

  get tableColumns(): Array<Object> {
    return [
      { field: 'name', text: 'Customer Group Name' },
      { field: 'type', text: 'Type' },
    ];
  }

  get customersGroups(): ?Element<*> {
    if (this.props.qualifyAll !== false) return null;
    return (<div styleName="root">
          <Typeahead
            className="_no-search-icon"
            isFetching={false}
            isAsync={false}
            component={CustomerGroupRow}
            items={this.suggestedGroups}
            name="customerGroupSelect"
            placeholder={'Add groups...'}
            inputElement={this.pilledInput}
            hideOnBlur={true}
            onItemSelected={this.handleSelectItem}
          />
        </div>);
  }

  @autobind
  setTerm(term: string) {
    this.setState({
      term,
    });
  }

  deselectItem(index: number) {
    const selected = [].slice.call(this.props.selectedGroupIds);
    selected.splice(index, 1);
    this.props.updateSelectedIds(selected);
  }

  @autobind
  handleSelectItem(item, event: Object) {
    if (_.find(this.props.selectedGroupIds, (i) => {return i == item.id;})) {
      event.preventHiding();
    } else {
      this.setState({
        term: '',
      });
      this.props.updateSelectedIds([...this.props.selectedGroupIds, item.id]);
    }
  }

  get pilledInput() {
    const { state, props } = this;
    const pills = props.selectedGroupIds.map((cg) => {
      if (_.find(props.groups, { 'id': cg })) return _.find(props.groups, { 'id': cg }, {}).name;
      return 'loading...';
    });

    return (
      <PilledInput
        solid={true}
        value={state.term}
        disabled={props.groups == null}
        onChange={ value => this.setTerm(value)}
        pills={pills}
        icon={null}
        onPillClose={(name, index) => this.deselectItem(index)}
      />
    );
  }

  render() {
    return (
      <div>
        <RadioButton
          id="qualifyAll"
          name="qualifyAll"
          label="All customers qualify"
          checked={this.props.qualifyAll === true}
          onChange={this.handleChangeQualifier}
        />
        <RadioButton
          id="qualifyGroups"
          name="qualifyGroups"
          label="Select customer groups qualify"
          checked={this.props.qualifyAll === false}
          onChange={this.handleChangeQualifier}
        />
        {this.customersGroups}
      </div>
    );
  }
}

const mapState = state => ({
  groups: _.get(_.invoke(state, 'customerGroups.list.currentSearch'), 'results.rows', [])
});

export default connect(mapState, { fetch: actions.fetch })(SelectCustomerGroups);
