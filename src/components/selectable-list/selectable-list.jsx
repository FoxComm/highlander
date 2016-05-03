/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import { PrimaryButton } from '../common/buttons';
import Overlay from '../overlay/overlay';
import SelectableItem from './selectable-item';

import styles from './selectable-list.css';

export type ItemType = {
  name?: string;
  id: number;
}

type Props = {
  visible: bool;
  items: Array<ItemType>;
  onSelect: (itemIds: Array<number>) => any;
  onBlur?: Function;
  selectedItemIds: Array<number>;
  renderItem: (item: ItemType) => Element;
  actionTitle: string;
}

export default class SelectableList extends Component {
  props: Props;

  static defaultProps = {
    renderItem: (item: ItemType) => {
      const title = item.name || '';
      return <SelectableItem title={title} id={item.id} />;
    },
    actionTitle: 'Choose',
  };

  state = {
    selectedItems: this.indexItemIds(this.props.selectedItemIds),
    cachedItemIds: this.props.selectedItemIds,
  };

  indexItemIds(ids) {
    return _.reduce(ids, (result, id) => {
      result[id] = true;
      return result;
    }, {});
  }

  componentWillReceiveProps(nextProps) {
    if (this.state.cachedItemIds != nextProps.selectedItemIds) {
      this.setState({
        selectedItems: this.indexItemIds(nextProps.selectedItemIds),
        cachedItemIds: nextProps.selectedItemIds,
      });
    }
  }

  @autobind
  handleItemSelect(id: number) {
    const { selectedItems } = this.state;
    selectedItems[id] = selectedItems[id] ? false : true;

    this.setState({
      selectedItems,
    });
  }

  get items(): Array<Element> {
    const { props } = this;

    return _.map(props.items, item => {
      const itemElement = props.renderItem(item);

      return React.cloneElement(itemElement, {
        onSelect: this.handleItemSelect,
        checked: this.state.selectedItems[item.id],
      });
    });
  }


  @autobind
  handleChooseClick(): void {
    const keys = _.reduce(this.state.selectedItems, (keys: Array<number>, selected: boolean, id: string) => {
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

  get defaultFooter(): Element {
    return (
      <PrimaryButton styleName="choose-button" onClick={this.handleChooseClick}>
        {this.props.actionTitle}
      </PrimaryButton>
    );
  }

  render() {
    const { props } = this;

    const className = classNames(props.className, {
      '_open': props.visible,
    });

    return (
      <div>
        <Overlay shown={props.visible} onClick={this.handleBlur} />
        <div styleName="selectable-list" className={className}>
          <ul styleName="items-list">
            {this.items}
          </ul>
          <div styleName="footer">
            {props.children || this.defaultFooter}
          </div>
        </div>
      </div>
    );
  }
}
