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
  disabled?: boolean,
  className?: string,
  onImageClick?: Function,
};

export default class ImageCard extends Component {

  props: Props;

  static defaultProps = {
    actions: [],
    onImageClick: () => {},
  };

  shouldComponentUpdate({ src: nextSrc, disabled: nextDisabled }) {
    const { src, disabled } = this.props;

    return src !== nextSrc || disabled !== nextDisabled;
  }

  get actions(): ?Element<*> {
    const { actions } = this.props;

    if (!actions.length) {
      return null;
    }

    return (
      <div className={s.actions}>
        {actions.map(({ name, handler }) => {
          return <i className={`icon-${name}`} onClick={handler} onMouseDown={this.prevent} key={name} />;
        })}
      </div>
    );
  }

  prevent(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
  }

  render() {
    const { id, src, className, disabled } = this.props;
    const cls = classNames(s.card, s.image, className, { [s.disabled]: disabled });

    return (
      <div className={cls} onClick={this.props.onImageClick}>
        <Image id={id} src={src} size="cover" />
        {this.actions}
      </div>
    );
  }
}
