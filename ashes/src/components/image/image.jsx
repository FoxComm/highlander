/* @flow */

// styles
import s from './image.css';

// libs
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';

// components
import Spinner from 'components/core/spinner';
import ProductImage from 'components/imgix/product-image';
import Transition from 'react-transition-group/CSSTransitionGroup';

type Props = {
  id: number,
  src: string,
  size?: 'cover' | 'contain',
};

type State = {
  ready: boolean,
  error: boolean,
  src?: string,
};

export default class ImageLoader extends Component {
  props: Props;

  state: State = {
    ready: true,
    error: false,
  };

  img: ?Image;
  showTransition: boolean = true;

  componentDidMount(): void {
    this.createImage();

    setTimeout(() => {
      if (this.img) {
        this.setState({ ready: false });
      }
    }, 100);
  }

  componentDidUpdate(): void {
    if (!this.state.ready && !this.img) {
      this.createImage();
    }
  }

  componentWillUnmount() {
    this.img = null;
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.src != nextProps.src) {
      this.createImage(nextProps.src);
    }
  }

  createImage(src: string = this.props.src): void {
    this.img = new Image();
    this.img.onload = this.handleLoad;
    this.img.onerror = this.handleLoad;
    this.img.src = src;
  }

  @autobind
  destroyImage(): void {
    this.img = null;
  }

  @autobind
  handleLoad(): void {
    if (!this.img) {
      return;
    }

    this.showTransition = !this.state.ready;

    this.setState(
      {
        ready: true,
        src: this.img.src,
        error: !this.img.width && !this.img.height,
      },
      this.destroyImage
    );
  }

  get loader(): ?Element<*> {
    return !this.state.ready ? <Spinner key="spinner" size="m" /> : null;
  }

  get image(): ?Element<any> {
    // Why ProductImage???
    let img = <ProductImage src={this.state.src} width={286} height={286} />;

    if (this.props.size && this.state.src) {
      let styles = {
        backgroundSize: this.props.size,
        backgroundImage: `url('${this.state.src}')`,
      };

      img = <div style={styles} className={s.bgImage} />;
    }

    return this.state.ready ? img : null;
  }

  wrapIfTransition(img: ?Element<any>): ?Element<any> {
    if (this.showTransition) {
      return (
        <Transition
          key="image-transition"
          component="div"
          className={s.transition}
          transitionName="image"
          transitionAppear={true}
          transitionLeave={false}
          transitionEnterTimeout={500}
          transitionAppearTimeout={500}
        >
          {img}
        </Transition>
      );
    }

    return img;
  }

  render() {
    const className = classNames(s.image, {
      [s.error]: this.state.error,
    });

    return (
      <div className={className}>
        {this.loader}
        {this.wrapIfTransition(this.image)}
      </div>
    );
  }
}
