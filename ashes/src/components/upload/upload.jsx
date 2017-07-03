/* @flow */

// libs
import _ from 'lodash';
import { autobind, debounce } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';

// styles
import s from './upload.css';

import type { FileInfo } from '../../modules/images';

// components
import Icon from 'components/core/icon';

// styles
import styles from './upload.css';

type Props = {
  children?: Element<*>;
  onDrop: Function;
  className: ?string;
  empty: boolean;
};

type State = {
  dragOverArea: boolean;
  dragOverWindow: boolean;
};

export default class Upload extends Component {

  props: Props;

  static defaultProps = {
    className: '',
  };

  state: State = {
    dragOverArea: false,
    dragOverWindow: false,
  };

  dragCounter: number;

  files: Array<FileInfo> = [];

  _fileInput: HTMLElement;

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
      dragOverWindow: false,
      dragOverArea: false
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
      dragOverWindow: this.dragCounter > 0
    });
  }

  @autobind
  handleDragEnter() {
    this.setState({
      dragOverArea: true,
    });
  }

  @autobind
  handleDragLeave() {
    this.setState({
      dragOverArea: false,
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
    if (this._fileInput) {
      this._fileInput.click();
    }
  }

  get emptyContent() {
    return (
      <div className={s.empty}>
        <Icon name="upload" className={s.icon} /> Drag & Drop to upload
      </div>
    );
  }

  get container(): ?Element<*> {
    const { children, empty } = this.props;

    return empty ? this.emptyContent : children;
  }

  render() {
    const { onDrop, empty } = this.props;
    const className = classNames(s.block, this.props.className, {
      [s.disabled]: !onDrop,
      [s.dragOverArea]: this.state.dragOverArea,
      [s.dragOverWindow]: this.state.dragOverWindow,
      [s.emptyMod]: empty,
    });

    return (
      <div
        className={className}
        onDragOver={this.handleDragOver}
        onDragEnter={this.handleDragEnter}
        onDragLeave={this.handleDragLeave}
        onDrop={this.onDrop}
      >
        <div className={s.container}>
          <input className={s.input} type="file" onChange={this.onDrop} value="" ref={r => this._fileInput = r} />
          {this.container}
        </div>
      </div>
    );
  }
}
