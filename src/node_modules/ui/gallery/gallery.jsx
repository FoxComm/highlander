/* @flow weak */


import _ from 'lodash';
import React, { Component } from 'react';
import styles from './gallery.css';
import { autobind } from 'core-decorators';

import ReactGestures from 'react-gestures';

type GalleryState = {
  selected: number;
}

type GalleryProps = {
  images: Array<string>;
}

export default class Gallery extends Component {
  props: GalleryProps;
  state: GalleryState;

  constructor(props: GalleryProps, ...args) {
    super(props, ...args);
    this.state = {
      selected: 0,
    };
  }

  @autobind
  nextImage() {
    const selected = this.state.selected;
    const newSelected = selected < (this.size - 1) ? (selected + 1) : selected;
    this.setState({selected: newSelected});
  }

  @autobind
  previousImage() {
    const selected = this.state.selected;
    const newSelected = selected > 0 ? (selected - 1) : selected;
    this.setState({selected: newSelected});
  }

  get size() {
    return _.size(this.props.images);
  }

  get controlls() {
    const currentIdx = this.state.selected;
    const dots = _.map(_.range(0, this.size), (idx) => {
      return <span styleName={idx === currentIdx ? 'dot-selected' : 'dot'} key={idx}>&middot;</span>;
    });
    return (
      <ReactGestures onSwipeLeft={this.nextImage} onSwipeRight={this.previousImage}>
        <div styleName="controlls">
          <div styleName="controlls-previous" onClick={this.previousImage}>
          </div>
          <div styleName="indicator-container">
            <div styleName="indicator">
              {dots}
            </div>
          </div>
          <div styleName="controlls-next" onClick={this.nextImage}>
          </div>
        </div>
      </ReactGestures>
    );
  }

  get imageBlock() {
    const currentIdx = this.state.selected;
    const images = _.map(this.props.images, (url, idx) => {
      return <div styleName="preview-image" key={idx}><img src={url}/></div>;
    });
    const shift = -100 * currentIdx;
    const translate = {transform: `translateX(${shift}%)`};
    return <div styleName="support" style={translate}>{images}</div>;
  }

  render() {
    const multipleImagesPresent = this.size > 1;
    return (
      <div styleName="gallery">
        <div styleName="images-block">
          {this.imageBlock}
        </div>
        {multipleImagesPresent && this.controlls}
      </div>
    );
  }
}
