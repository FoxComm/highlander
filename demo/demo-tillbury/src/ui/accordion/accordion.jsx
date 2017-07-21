/* @flow */

// libs
import React, { Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// styles
import styles from './accordion.css';

type Props = {
  isInitiallyCollapsed: boolean,
  title: string,
  children?: any,
  className?: string,
};

type State = {
  isCollapsed: boolean,
};

class Accordion extends Component {
  props: Props;

  static defaultProps = {
    isInitiallyCollapsed: true,
  };

  state: State = {
    isCollapsed: this.props.isInitiallyCollapsed,
  };

  @autobind
  toggleCollapsed() {
    this.setState({
      isCollapsed: !this.state.isCollapsed,
    });
  }

  render() {
    const modifiers = classNames(this.props.className, {
      [styles._collapsed]: this.state.isCollapsed,
    });

    return (
      <article styleName="accordion" className={modifiers}>
        <div styleName="title" onClick={this.toggleCollapsed}>{this.props.title}</div>
        <div styleName="content">{this.props.children}</div>
      </article>
    );
  }
}

export default Accordion;
