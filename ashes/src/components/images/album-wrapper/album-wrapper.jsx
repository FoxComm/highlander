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
  actions: Array<Action>;
  title: string;
  position: number;
  albumsCount: number;
  onSort: (direction: number) => void;
  titleWrapper?: (title: string) => Element<*>;
  className?: string;
  contentClassName: ?string;
  children?: Array<Element<*>>|Element<*>;
}

const MOVE_DIRECTION_UP = -1;
const MOVE_DIRECTION_DOWN = 1;

export default class AlbumWrapper extends Component {

  props: Props;

  static defaultProps = {
    title: '',
    actions: [],
  };

  @autobind
  handleMoveUp(): void {
    if (this.isFirstAlbum) {
      return;
    }

    this.props.onSort(MOVE_DIRECTION_UP);
  }

  @autobind
  handleMoveDown(): void {
    if (this.isLastAlbum) {
      return;
    }

    this.props.onSort(MOVE_DIRECTION_DOWN);
  }

  get isFirstAlbum(): boolean {
    return this.props.position === 0;
  }

  get isLastAlbum(): boolean {
    return this.props.position === this.props.albumsCount - 1;
  }

  get title() {
    const { title, titleWrapper } = this.props;

    return (
      <div className={styles.title}>
        <div className={styles.titleWrapper}>
          <span>{titleWrapper ? titleWrapper(title) : title}</span>
        </div>
      </div>
    );
  }

  get controls() {
    const moveUpCsl = classNames(styles.controlItem, { '_disabled': this.isFirstAlbum });
    const moveDownCsl = classNames(styles.controlItem, { '_disabled': this.isLastAlbum });

    return (
      <div className={styles.controls}>
        <div className={styles.left}>
          <div className={styles.controlMove}>
            <span className={moveUpCsl}>
              <i className="icon-up" onClick={this.handleMoveUp} />
            </span>
            <span className={moveDownCsl}>
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

  render() {
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
