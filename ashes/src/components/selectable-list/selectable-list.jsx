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
  className?: string,
  visible: boolean,
  items: Array<ItemType>,
  onSelect?: (itemIds: Array<number>, event: SyntheticEvent) => any,
  onBlur?: Function,
  selectedItemIds: Array<number>,
  renderItem: (item: ItemType) => Element<*>,
  actionTitle: string,
  popup: boolean,
  emptyMessage?: Element<*>|string,
};

type State = {
  selectedItems: { [id: string|number]: boolean },
  cachedItemIds: Array<number>,
};

export default class SelectableList extends Component {
  props: Props;

  static defaultProps = {
    renderItem: (item: ItemType) => {
      const title = item.name || '';
      return <SelectableItem title={title} id={item.id} />;
    },
    actionTitle: 'Choose',
    selectedItemIds: [],
    popup: true,
  };

  state: State = {
    selectedItems: this.indexItemIds(this.props.selectedItemIds),
    cachedItemIds: this.props.selectedItemIds,
  };

  indexItemIds(ids: Array<number>): { [id: string|number]: boolean } {
    return _.reduce(ids, (result, id) => {
      result[id] = true;
      return result;
    }, {});
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.state.cachedItemIds != nextProps.selectedItemIds) {
      this.setState({
        selectedItems: this.indexItemIds(nextProps.selectedItemIds),
        cachedItemIds: nextProps.selectedItemIds,
      });
    }
  }

  @autobind
  handleItemToggle(id: number) {
    const { selectedItems } = this.state;
    selectedItems[id] = selectedItems[id] ? false : true;

    this.setState({
      selectedItems,
    });
  }

  get items(): Array<Element<*>> {
    const { props } = this;

    return _.map(props.items, item => {
      const itemElement = props.renderItem(item);

      return React.cloneElement(itemElement, {
        onToggle: this.handleItemToggle,
        checked: this.state.selectedItems[item.id],
        key: item.id,
      });
    });
  }

  get selectedIds(): Array<number> {
    return _.reduce(this.state.selectedItems, (keys: Array<number>, selected: boolean, id: string) => {
      if (selected) keys = [...keys, Number(id)];
      return keys;
    }, []);
  }

  @autobind
  handleChooseClick(event: SyntheticEvent): void {
    event.stopPropagation();
    event.preventDefault();
    if (this.props.onSelect) {
      this.props.onSelect(this.selectedIds, event);
    }
    this.clearState();
  };

  selectedItemsMap(): Object {
    return _.reduce(this.props.items, (itemsMap: Object, item: ItemType) => {
      if (this.state.selectedItems[item.id]) {
        itemsMap[item.id] = item;
      }
      return itemsMap;
    }, {});
  }

  @autobind
  clearState() {
    this.setState({
      selectedItems: {},
    });
  }

  @autobind
  handleBlur() {
    if (this.props.onBlur) {
      this.props.onBlur();
    }
  }

  get defaultFooter(): Element<*> {
    return (
      <PrimaryButton
        styleName="choose-button"
        onClick={this.handleChooseClick}
        disabled={this.selectedIds.length === 0}>
        {this.props.actionTitle}
      </PrimaryButton>
    );
  }

  get content() {
    const { props } = this;
    if (_.isEmpty(props.items) && props.emptyMessage) {
      return (
        <div styleName="blank-state">{props.emptyMessage}</div>
      );
    }
    return [
      <ul styleName="items-list" key="items-list">
        {this.items}
      </ul>,
      <div styleName="footer" key="footer">
        {props.children || this.defaultFooter}
      </div>
    ];
  }

  render() {
    const { props } = this;

    const className = classNames(props.className, {
      '_open': props.visible,
      [styles.popup]: props.popup,
      [styles['visible-state']]: props.popup,
    });

    return (
      <div>
        {props.popup && <Overlay shown={props.visible} onClick={this.handleBlur} />}
        <div styleName="selectable-list" className={className}>
          {this.content}
        </div>
      </div>
    );
  }
}
