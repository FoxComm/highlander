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
  children?: Element<*>;
  onDrop: Function;
  className: ?string;
  empty: boolean;
}

type State = {
  dragActive: boolean;
  dragPossible: boolean;
}

export default class Upload extends Component {

  props: Props;

  static defaultProps = {
    className: '',
  };

  state: State = {
    dragActive: false,
    dragPossible: false,
  };

  dragCounter: number;

  files: Array<FileInfo> = [];

  componentDidMount() {
    this.dragCounter = 0;
    document.addEventListener('dragenter', this.increaseDragCounter, true);
    document.addEventListener('dragleave', this.decreaseDragCounter, true);
    document.addEventListener('drop', this.decreaseDragCounter, true);
  }

  componentWillUnmount() {
    document.removeEventListener('dragenter', this.increaseDragCounter, true);
    document.removeEventListener('dragleave', this.decreaseDragCounter, true);
    document.addEventListener('drop', this.decreaseDragCounter, true);
  }

  resetDragging() {
    this.setState({
      dragPossible: false,
      dragActive: false
    });
  }

  @autobind
  increaseDragCounter(): void {
    this.dragCounter += 1;
    this.updateDragPossibility();
  }

  @autobind
  decreaseDragCounter(): void {
    this.dragCounter -= 1;
    this.updateDragPossibility();
  }

  updateDragPossibility() {
    this.setState({
      dragPossible: this.dragCounter > 0
    });
  }

  @autobind
  handleDragEnter() {
    this.setState({
      dragActive: true,
    });
  }

  @autobind
  handleDragLeave() {
    this.setState({
      dragActive: false,
    });
  }

  @autobind
  handleDragOver(e: SyntheticDragEvent): void {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
  }

  @autobind
  onDrop(e: any): void {
    e.preventDefault();

    this.resetDragging();

    if (!this.props.onDrop) {
      return;
    }

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
        key: _.uniqueId('image_'),
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
           onDragOver={this.handleDragOver}
           onDragEnter={this.handleDragEnter}
           onDragLeave={this.handleDragLeave}
           onDrop={this.onDrop}>
        {content}
      </div>
    );
  }

  render() {
    const { onDrop, empty } = this.props;
    const className = classNames(this.props.className, {
      '_disabled': !onDrop,
      '_dragActive': this.state.dragActive,
      '_dragPossible': this.state.dragPossible,
      '_empty': empty,
    });

    return (
      <div styleName="upload" className={className}>
        <input className={styles.input} type="file" onChange={this.onDrop} value="" ref="fileInput" />
        {this.container}
      </div>
    );
  }
}
