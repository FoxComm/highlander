// @flow

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import Typeahead from 'components/typeahead/typeahead';
import PilledInput from 'components/pilled-search/pilled-input';
import CustomerRow from './customer-row';

import styles from './customers.css';

type Props = {
  hideOnBlur: boolean,
  maxUsers: number,
  suggestState: string,
  suggested: Array<Customer>,
  suggestCustomers: Function,
  onSelect: Function,
};

type State = {
  term: string,
  selected: Array<Customer>,
};

export default class GroupsTypeahead extends Component {
  static defaultProps = {
    hideOnBlur: false,
    maxUsers: Infinity,
  };

  props: Props;
  state: State = {
    selected: [],
    term: '',
  };

  componentWillUpdate(nextProps: Props, nextState: State) {
    if (nextState.selected != this.state.selected) {
      this.props.onSelect(nextState.selected);
    }
  }

  setTerm(term: string) {
    this.setState({
      term,
    });
  }

  @autobind
  deselectItem(index: number) {
    const selected = [].slice.call(this.state.selected);
    selected.splice(index, 1);
    this.setState({
      selected,
    });
  }

  @autobind
  handleSelectItem(item: Object, event: Object) {
    if (_.find(this.state.selected, {id: item.id})) {
      event.preventHiding();
    } else {
      this.setState({
        term: '',
        selected: [
          ...this.state.selected,
          item,
        ],
      });
    }
  }

  groupPill(customer: Object) {
    return `${customer.name} : ${customer.id}`;
  }

  get pilledInput() {
    const { state, props } = this;
    const pills = state.selected.map(this.groupPill);

    return (
      <PilledInput
        solid={true}
        autoFocus={true}
        value={state.term}
        disabled={state.selected.length >= props.maxUsers}
        onChange={({target}) => this.setTerm(target.value)}
        pills={pills}
        icon={null}
        onPillClose={(name, index) => this.deselectItem(index)}
      />
    );
  }

  get placeholder(): string {
    return 'Customer name or email';
  }

  render() {
    const { props } = this;

    return (
      <div styleName="root">
        <div styleName="label">
          <label>Customers</label>
        </div>
        <Typeahead
          className="_no-search-icon"
          isFetching={_.get(props.suggestState, 'inProgress', false)}
          fetchItems={props.suggestCustomers}
          minQueryLength={2}
          component={CustomerRow}
          items={props.suggested}
          name="groupsSelect"
          placeholder={this.placeholder}
          inputElement={this.pilledInput}
          hideOnBlur={props.hideOnBlur}
          onItemSelected={this.handleSelectItem}
        />
      </div>
    );
  }
}
