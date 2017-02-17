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
  name: string,
  handler: (e: MouseEvent) => void,
};

type Props = {
  id: number,
  src: string,
  actions: Array<Action>,
  title: string,
  loading: boolean,
  secondaryTitle?: string,
  className?: string,
};

type State = {
  actionsVisible: boolean,
};

export default class ImageCard extends Component {

  props: Props;

  state: State = {
    actionsVisible: false,
  };

  static defaultProps = {
    actions: [],
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

  get actions(): ?Element<*> {
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

  get description(): ?Element<*> {
    let { title, secondaryTitle, src, loading } = this.props;

    if (!title) {
      title = src;
    }

    return (
      <div className={classNames(styles.description, { [styles._loading]: loading })}>
        <div className={styles.title}>{title}</div>
        <div className={styles.secondaryTitle}>{secondaryTitle}</div>
      </div>
    );
  }

  render() {
    const { id, src, className } = this.props;

    return (
      <div className={classNames(styles.card, className)}>
        <div className={styles.image} onMouseOver={this.showActions} onMouseOut={this.hideActions}>
          <Image id={id} src={src} />
        </div>
        {this.actions}
        {this.description}
      </div>
    );
  }
};
