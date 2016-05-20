/* @flow */

// styles
import styles from './image-card.css';

//libs
import _ from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import Image from '../image/image';

export type Action = {
  name: string;
  handler: (e: MouseEvent) => void;
}

type Props = {
  src: string;
  actions?: Array<Action>;
  title: string;
  loading: boolean;
  secondaryTitle?: string;
  className?: string;
};

type State = {
  actionsVisible: boolean;
}

export default class ImageCard extends Component {

  static props: Props;

  state: State = {
    actionsVisible: false,
  };

  overTimeout: ?number = null;

  @autobind
  showActions(): void {
    clearTimeout(this.overTimeout);

    this.setState({
      actionsVisible: true,
    });
  }

  @autobind
  hideActions(): void {
    this.overTimeout = setTimeout(() => {
      this.setState({
        actionsVisible: false,
      });
    }, 100);
  }

  get actions(): ?Element {
    const { actions } = this.props;

    if (_.isEmpty(actions)) {
      return null;
    }

    const cls = classNames(styles.actions, {
      [styles.actionsVisible]: this.state.actionsVisible,
    });

    return (
      <div className={cls} onMouseOver={this.showActions} onMouseOut={this.hideActions}>
        {actions.map(({ name, handler }) => <i className={`icon-${name}`} onMouseDown={handler} key={name} />)}
      </div>
    );
  }

  get description(): ?Element {
    let { title, secondaryTitle, src } = this.props;

    if (!title) {
      title = src;
    }

    return (
      <div className={styles.description}>
        <div className={styles.title}>{title}</div>
        <div className={styles.secondaryTitle}>{secondaryTitle}</div>
      </div>
    );
  }

  render(): Element {
    const { src, loading, className } = this.props;

    const cls = classNames(styles.card, {
      [styles.loading]: loading,
    }, className);

    return (
      <div className={cls}>
        <div className={styles.image} onMouseOver={this.showActions} onMouseOut={this.hideActions}>
          <Image src={src} />
        </div>
        {this.actions}
        {this.description}
      </div>
    );
  }
};
