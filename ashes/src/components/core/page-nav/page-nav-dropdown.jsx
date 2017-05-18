/* @flow */

// libs
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import Transition from 'react-transition-group/CSSTransitionGroup';

// styles
import s from './page-nav.css';

type Props = {
  title: string,
  children: Array<Element<any>>,
  className?: string,
};

type State = {
  opened: boolean,
};

const transitionProps = {
  component: 'div',
  transitionName: `dd-transition-center`,
  transitionEnterTimeout: 300,
  transitionLeaveTimeout: 300,
};

class NavDropdown extends Component {
  props: Props;

  state: State = {
    opened: false,
  };

  @autobind
  handleMouseOver() {
    this.setState({ opened: true });
  }

  @autobind
  handleMouseOut() {
    this.setState({ opened: false });
  }

  get items() {
    if (!this.state.opened) {
      return null;
    }

    return (
      <ul className={s.dropdown}>
        {this.props.children}
      </ul>
    );
  }

  render() {
    const { title, className } = this.props;
    const cls = classNames(s.parent, s.item, {
      [s.opened]: this.state.opened,
    }, className);

    return (
      <li className={cls} onMouseEnter={this.handleMouseOver} onMouseLeave={this.handleMouseOut}>
        <a>{title}<i className="icon-chevron-down" /></a>

        <Transition {...transitionProps}>
          {this.items}
        </Transition>
      </li>
    );
  }
}

export default NavDropdown;
