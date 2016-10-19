import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './dropdown.css';
import cx from 'classnames';
import CSSTransitionGroup from 'react-addons-css-transition-group';

type DropdownItem = {
  id: int,
  title: string
};

export default class Dropdown extends Component {
  props: {
    items: Array<DropdownItem>
  }

  static defaultProps = {
    items: [],
  }

  constructor (props) {
    super(props);
    this.state = {
      open: false,
      activeItem: props.items[0],
    };
  }

  @autobind
  toggleDropdown () {
    this.setState({ open: !this.state.open });
  }

  @autobind
  setActiveItem (activeItem) {
    this.setState({ activeItem });
  }

  render () {
    const cls = cx(styles.selector, {
      [styles.open]: this.state.open,
    });

    const items =
      this.props.items.map(item => {
        const itemCls = cx(styles.item, {
          [styles.active]: item.id === this.state.activeItem.id,
        });
        const onClick = this.setActiveItem.bind(this, item);

        return (
          <div className={itemCls} onClick={onClick} key={item.id}>{item.title}</div>
        );
      });

    return (
      <div styleName="dropdown">
        <div className={cls} onClick={this.toggleDropdown}>All</div>

        <CSSTransitionGroup
          transitionName={styles.items}
          transitionEnterTimeout={500}
          transitionLeaveTimeout={300}
        >
          {this.state.open &&

            <div styleName="items">
              <div styleName="top-triangle"></div>
              <div styleName="items-wrap">
                {items}
              </div>
            </div>}
        </CSSTransitionGroup>
      </div>
    );
  }
}
