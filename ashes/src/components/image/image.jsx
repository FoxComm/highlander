/* @flow */

// styles
import styles from './image.css';

// libs
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';
import Transition from 'react-addons-css-transition-group';

// components
import WaitAnimation from '../common/wait-animation';

type Props = {
  id: number,
  src: string,
  loader?: string|Element<*>;
}

type State = {
  ready: boolean;
  error: boolean;
  src?: string;
}

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

  // $FlowFixMe: there is no global context, stupid flow
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

    this.setState({
      ready: true,
      src: this.img.src,
      error: !this.img.width && !this.img.height,
    }, this.destroyImage);
  }

  get loader(): ?Element<*> {
    return !this.state.ready ? <WaitAnimation key="loader" size="m" /> : null;
  }

  get image(): ?Element<*> {
    return this.state.ready ? <img src={this.state.src} key={this.props.id} /> : null;
  }

  wrapToTransition(img: ?Element<*>) {
    if (this.showTransition) {
      return (
        <Transition
          key="image-transition"
          component="div"
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
    return (
      <div key="image">{img}</div>
    );
  }

  render() {
    const className = classNames(styles.image, {
      [styles.error]: this.state.error,
    });

    return (
      <div className={className}>
        {[
          this.loader,
          this.wrapToTransition(this.image)
        ]}
      </div>
    );
  }
}
