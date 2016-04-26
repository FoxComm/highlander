/* @flow */

// styles
import styles from './image.css';

// libs
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';
import Transition from 'react-addons-css-transition-group';

// components
import WaitAnimation from '../../common/wait-animation';

type Props = {
  src: string,
  loader?: string|Element;
}

type State = {
  ready: boolean;
  error: boolean;
}

export default class ImageLoader extends Component {

  props: Props;

  state: State = {
    ready: false,
    error: false,
  };

  img: ?Image;

  componentDidMount(): void {
    this.createImage();
  }

  componentDidUpdate(): void {
    if (!this.state.ready && !this.img) {
      this.createImage();
    }
  }

  createImage(): void {
    this.img = new Image();
    this.img.onload = this.handleLoad;
    this.img.onerror = this.handleLoad;
    this.img.src = this.props.src;
  }

  @autobind
  destroyImage(): void {
    this.img = null;
  }

  @autobind
  handleLoad(): void {
    setTimeout(() => {
      if (!this.img) {
        return;
      }

      this.setState({
        ready: true,
        error: !this.img.width && !this.img.height,
      }, this.destroyImage);
    }, Math.random() * 500);
  }

  get loader(): ?Element {
    return !this.state.ready ? <WaitAnimation size="m" /> : null;
  }

  get image(): ?Element {
    return this.state.ready ? <img src={this.props.src} key={this.props.src} /> : null;
  }

  render(): Element {
    const className = classNames(styles.image, {
      [styles.error]: this.state.error,
    });

    return (
      <div className={className}>
        {this.loader}
        <Transition transitionName="image" transitionEnterTimeout={500} transitionLeaveTimeout={500}>
          {this.image}
        </Transition>
      </div>
    );
  }
}
