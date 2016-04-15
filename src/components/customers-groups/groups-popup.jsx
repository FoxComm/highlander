/* flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import { PrimaryButton } from '../common/buttons';
import { Checkbox } from '../checkbox/checkbox';

import styles from './groups-popup.css';

export type GroupType = {
  name: string;
  type: string;
  id: number;
}

type Props = {
  visible: bool;
  groups: Array<GroupType>;
  onSelect: (groups: Array<number>) => any;
  onBlur?: Function;
  selectedGroupIds: Array<number>;
}

export default class GroupsPopup extends Component {
  props: Props;

  state = {
    selectedGroups: this.indexGroupIds(this.props.selectedGroupIds),
    cachedGroupIds: this.props.selectedGroupIds,
  };

  indexGroupIds(ids) {
    return _.reduce(ids, (result, id) => {
      result[id] = true;
      return result;
    }, {});
  }

  componentWillReceiveProps(nextProps) {
    if (this.state.cachedGroupIds != nextProps.selectedGroupIds) {
      this.setState({
        selectedGroups: this.indexGroupIds(nextProps.selectedGroupIds),
        cachedGroupIds: nextProps.selectedGroupIds,
      });
    }
  }

  toggleGroupSelected(event: Object, id: number) {
    event.stopPropagation();
    event.preventDefault();

    const { selectedGroups } = this.state;
    selectedGroups[id] = selectedGroups[id] ? false : true;

    this.setState({
      selectedGroups,
    });
  }

  get groupItems(): Array<Element> {
    const { props } = this;

    return _.map(props.groups, group => {
      const id = `customer-group-${group.id}`;
      return (
        <li data-id={group.id} styleName="group" onClick={(event) => this.toggleGroupSelected(event, group.id)}>
          <Checkbox id={id} checked={this.state.selectedGroups[group.id]}>
            <strong>{group.name}</strong>
          </Checkbox>
          <span styleName="group-description">• {group.type}</span>
        </li>
      );
    });
  }

  @autobind
  dontPropagate(event: SyntheticEvent): void {
    event.stopPropagation();
  }

  @autobind
  handleAddClick(): void {
    const keys = _.reduce(this.state.selectedGroups, (keys, selected, id) => {
      if (selected) keys = [...keys, Number(id)];
      return keys;
    }, []);
    this.props.onSelect(keys);
  }

  @autobind
  handleBlur() {
    if (this.props.onBlur) {
      this.props.onBlur();
    }
  }

  render() {
    const { props } = this;

    const className = classNames(props.className, {
      '_open': props.visible,
    });

    return (
      <div styleName="groups-popup" className={className} onClick={this.dontPropagate} onBlur={this.handleBlur}>
        <ul styleName="groups-list">
          {this.groupItems}
        </ul>
        <div styleName="controls">
          <PrimaryButton styleName="add-button" onClick={this.handleAddClick}>Add</PrimaryButton>
        </div>
      </div>
    );
  }
}
