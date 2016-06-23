/* @flow */

// styles
import styles from './upload.css';

// libs
import _ from 'lodash';
import { autobind, debounce } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';
import { findDOMNode } from 'react-dom';

import type { FileInfo } from '../../modules/images';

type Props = {
  children?: Element;
  onDrop?: Function;
  className: ?string;
  empty: boolean;
}

type State = {
  dragActive: boolean;
}

export default class Upload extends Component {

  static props: Props;

  static defaultProps: Props = {
    className: '',
  };

  state: State = {
    dragActive: false,
  };

  files: Array<FileInfo> = [];

  componentDidMount() {
    this.dragCounter = 0;
    document.addEventListener('dragenter', this.handleDragEnter);
    document.addEventListener('dragleave', this.handleDragLeave);
  }

  componentWillUnmount() {
    document.removeEventListener('dragenter', this.handleDragEnter);
    document.removeEventListener('dragleave', this.handleDragLeave);
  }

  @autobind
  handleDragEnter(): void {
    this.dragCounter += 1;
    this.updateDragActive();
  }

  @autobind
  handleDragLeave(): void {
    this.dragCounter -= 1;
    this.updateDragActive();
  }

  updateDragActive() {
    this.setState({
      dragActive: this.dragCounter > 0
    });
  }

  @autobind
  onDragOver(e: SyntheticDragEvent): void {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
  }

  @autobind
  onDrop(e: any): void {
    e.preventDefault();

    if (!this.props.onDrop) {
      return;
    }

    this.setState({
      dragActive: false,
    });

    let files;

    if (e.dataTransfer) {
      files = e.dataTransfer.files;
    } else if (e.target) {
      files = e.target.files;
    }

    _.each(files, this.processFile);
  }

  @autobind
  processFile(file: File): void {
    const reader = new FileReader();

    reader.onloadend = () => {
      this.files.push({
        file: file,
        src: reader.result,
        loading: false,
      });

      this.flushFiles();
    };

    reader.readAsDataURL(file);
  }

  @autobind
  @debounce(200)
  flushFiles() {
    this.props.onDrop(this.files);

    this.files = [];
  }

  openUploadDialog() {
    this.refs.fileInput.click();
  }

  get emptyContent() {
    return (
      <div className={styles.empty}>
        <i className="icon-upload" /> Drag & Drop to upload
      </div>
    );
  }

  get container() {
    const { children, empty } = this.props;

    const content = empty ? this.emptyContent : children;

    return (
      <div styleName="container"
           onDragOver={this.onDragOver}
           onDragLeave={this.onDragLeave}
           onDrop={this.onDrop}>
        {content}
      </div>
    );
  }

  render(): Element {
    const { onDrop, empty } = this.props;
    const className = classNames(this.props.className, {
      '_disabled': !onDrop,
      '_dragActive': this.state.dragActive,
      '_empty': empty,
    });

    return (
      <div styleName="upload" className={className}>
        <input className={styles.input} type="file" onChange={this.onDrop} ref="fileInput" />
        {this.container}
      </div>
    );
  }
}
