/* @flow */

// styles
import styles from './album-wrapper.css';

// libs
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';

type Action = {
  name: string;
  handler: Function;
}

type Props = {
  children: ?Array<Element>|Element;
  actions: ?Array<Action>;
  title: ?string;
  onSort: (direction: number) => void;
  titleWrapper: ?(title: string) => Element;
  className: ?string;
  contentClassName: ?string;
}

const MOVE_DIRECTION_UP = -1;
const MOVE_DIRECTION_DOWN = 1;

export default class AlbumWrapper extends Component {

  static props: Props;

  static defaultProps = {
    title: '',
    actions: [],
  };

  @autobind
  handleMoveUp() {
    this.props.onSort(MOVE_DIRECTION_UP);
  }

  @autobind
  handleMoveDown() {
    this.props.onSort(MOVE_DIRECTION_DOWN);
  }

  get title(): ?Element {
    const { title, titleWrapper } = this.props;

    return (
      <div className={styles.title}>
        <div className={styles.titleWrapper}>
          <span>{titleWrapper ? titleWrapper(title) : title}</span>
        </div>
      </div>
    );
  }

  get controls(): Element {
    return (
      <div className={styles.controls}>
        <div className={styles.left}>
          <div className={styles.controlMove}>
            <span className={styles.controlItem}>
              <i className="icon-up" onClick={this.handleMoveUp} />
            </span>
            <span className={styles.controlItem}>
              <i className="icon-down" onClick={this.handleMoveDown} />
            </span>
          </div>
        </div>
        <div className={styles.right}>
          {this.props.actions.map(({ name, handler }) => {
            return (
              <span className={styles.controlItem} key={name}>
                <i className={`icon-${name}`} onClick={handler} />
              </span>
            );
          })}
        </div>
      </div>
    );
  }

  render(): Element {
    const { className, contentClassName } = this.props;

    const cls = classNames(styles.accordion, className);

    return (
      <div className={cls}>
        <div className={styles.header}>
          {this.title}
          {this.controls}
        </div>
        <div className={classNames(styles.content, contentClassName)} ref="content">
          {this.props.children}
        </div>
      </div>
    );
  }
}
