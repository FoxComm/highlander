/* @flow */

// styles
import styles from './accordion.css';

// libs
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';

type Props = {
  children?: Array<Element>|Element;
  controls: Array<Element>|Element;
  title?: string;
  open?: boolean;
}

type State = {
  open: boolean;
}

export default class Accordion extends Component {

  static props: Props;

  static defaultProps = {
    open: true,
  };

  state: State = {
    open: this.props.open,
  };

  componentDidUpdate(): void {
    let maxHeight = 0;
    if (this.state.open) {
      maxHeight = this.refs.content.scrollHeight;
    }

    this.refs.content.style.maxHeight = `${maxHeight}px`;
  }

  @autobind
  toggle(): void {
    this.setState({
      open: !this.state.open,
    });
  }

  get title(): ?Element {
    if (!this.props.title) {
      return null;
    }

    return <div className={styles.title} onClick={this.toggle}><span>{this.props.title}</span></div>;
  }

  get controls(): Element {
    return (
      <div className={styles.controls}>
        <div className={styles.left}>
          <span className={styles.controlItem} onClick={this.toggle}>
            <i className="icon-up"/>
            <i className="icon-down"/>
          </span>
        </div>
        <div className={styles.right}>
          <span className={styles.controlItem}><i className="icon-add" /></span>
          <span className={styles.controlItem}><i className="icon-edit" /></span>
          <span className={styles.controlItem}><i className="icon-trash" /></span>
          {this.props.controls}
        </div>
      </div>
    );
  }

  render(): Element {
    const cls = classNames(styles.accordion, {
      [styles._open]: this.state.open,
    });

    return (
      <div className={cls}>
        <div className={styles.header}>
          {this.title}
          {this.controls}
        </div>
        <div className={styles.content} ref="content">
          {this.props.children}
        </div>
      </div>
    );
  }
}
