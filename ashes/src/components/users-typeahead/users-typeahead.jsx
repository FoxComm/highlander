// @flow

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './users-typeahead.css';

// components
import Typeahead from '../typeahead/typeahead';
import PilledInput from '../pilled-search/pilled-input';
import UserRow from './user-row';

type Props = {
  maxUsers?: number,
  label?: string,
  className?: string,
  hideOnBlur?: boolean,
  onSelect: (users: Array<UserType>) => void,
  suggested: Array<UserType>,
  suggestUsers: (term: string) => AbortablePromise,
  suggestState: AsyncState,
}

type State = {
  term: string,
  selected: Array<any>,
}

/**
 * Dump component, doesn't connected to redux store.
 * Requires `suggested`, `suggestUsers`, `suggestState` props for work.
 * For suggesting admins, for example, see `admins-typeahead` component which connect this component to admins view.
 */
export default class UsersTypeahead extends Component {
  static defaultProps = {
    hideOnBlur: false,
    maxUsers: Infinity,
  };

  props: Props;
  state: State = {
    selected: [],
    term: '',
  };

  setTerm(term: string) {
    this.setState({
      term,
    });
  }

  componentWillUpdate(nextProps: Props, nextState: State) {
    if (nextState.selected != this.state.selected) {
      this.props.onSelect(nextState.selected);
    }
  }

  @autobind
  getUsername(user: UserType) {
    return user.name ? user.name : `${user.firstName} ${user.lastName}`;
  }

  deselectItem(index: number) {
    const selected = [].slice.call(this.state.selected);
    selected.splice(index, 1);
    this.setState({
      selected,
    });
  }

  @autobind
  handleSelectItem(item, event) {
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

  get pilledInput() {
    const { state, props } = this;
    const pills = state.selected.map(this.getUsername);

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
    return this.state.selected.length > this.props.maxUsers ? '' : 'Name or email...';
  }

  render() {
    const { props } = this;

    return (
      <div styleName="root">
        <div styleName="label">
          <label>{props.label}</label>
        </div>
        <Typeahead
          className="_no-search-icon"
          isFetching={_.get(props.suggestState, 'inProgress', false)}
          fetchItems={props.suggestUsers}
          minQueryLength={2}
          component={UserRow}
          items={props.suggested}
          name="usersSelect"
          placeholder={this.placeholder}
          inputElement={this.pilledInput}
          hideOnBlur={props.hideOnBlur}
          onItemSelected={this.handleSelectItem}
        />
      </div>
    );
  }
}


