/* @flow */

// styles
import s from './image-card.css';

// libs
import classNames from 'classnames';
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
  loading: boolean,
  className?: string,
};

export default class ImageCard extends Component {

  props: Props;

  static defaultProps = {
    actions: [],
  };

  get actions(): ?Element<*> {
    const { actions } = this.props;

    if (!actions.length) {
      return null;
    }

    return (
      <div className={s.actions}>
        {actions.map(({ name, handler }) => <i className={`icon-${name}`} onClick={handler} key={name} />)}
      </div>
    );
  }

  render() {
    const { id, src, className } = this.props;

    return (
      <div className={classNames(s.card, s.image, className)}>
        <Image id={id} src={src} size="cover" />
        {this.actions}
      </div>
    );
  }
}
