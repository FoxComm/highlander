/* @flow */

// styles
import s from './image-card.css';

// libs
import classNames from 'classnames';
import React, { Component, Element } from 'react';

// components
import Image from '../image/image';
import Icon from 'components/core/icon';

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
  failed?: boolean,
  className?: string,
  onImageClick?: Function,
};

export default class ImageCard extends Component {
  props: Props;

  static defaultProps = {
    actions: [],
    onImageClick: () => {},
  };

  shouldComponentUpdate({ src: nextSrc, disabled: nextDisabled, failed: nextFailed, loading: nextLoading }: Props) {
    const { src, disabled, failed, loading } = this.props;

    return src !== nextSrc || disabled !== nextDisabled || failed !== nextFailed || loading !== nextLoading;
  }

  get actions(): ?Element<*> {
    const { actions } = this.props;

    if (!actions.length) {
      return null;
    }

    return (
      <div className={s.actions}>
        {actions.map(({ name, handler }) =>
          <Icon name={name} onClick={handler} onClick={handler} onMouseDown={this.prevent} key={name} />
        )}
      </div>
    );
  }

  prevent(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
  }

  render() {
    const { id, src, className, disabled, loading, failed } = this.props;
    const cls = classNames(s.card, s.image, className, {
      [s.disabled]: disabled,
      [s.loading]: loading,
      [s.failed]: failed,
    });

    return (
      <div className={cls} onClick={this.props.onImageClick}>
        <Image id={id} src={src} size="cover" />
        {this.actions}
      </div>
    );
  }
}
