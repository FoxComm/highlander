/* @flow */

import invoke from 'lodash/invoke';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

import styles from './youtube.css';

let loadYT;

type Props = {
  YTid: string,
  width: string,
  height: string,
  fadedAtStart: boolean,
  className?: string,
  playButtonClassName?: string,
};

type State = {
  apiLoaded: boolean,
  playing: boolean,
};

export default class YouTube extends Component {
  props: Props;

  state: State = {
    apiLoaded: false,
    playing: false,
  };

  _playerObject: any;
  _playerElement: Element<*>;

  componentDidMount() {
    if (!loadYT) {
      loadYT = new Promise(resolve => {
        const tag = document.createElement('script');
        tag.src = 'https://www.youtube.com/iframe_api';
        const firstScriptTag = document.getElementsByTagName('script')[0];
        invoke(firstScriptTag, 'parentNode.insertBefore', tag, firstScriptTag);
        window.onYouTubeIframeAPIReady = () => resolve(window.YT);
      });
    }

    loadYT.then((YT) => {
      this.setState({ apiLoaded: true }, () => {
        this._playerObject = new YT.Player(this._playerElement, {
          height: this.props.height,
          width: this.props.width,
          videoId: this.props.YTid,
        });
      });
    });
  }

  @autobind
  play() {
    this.setState({ playing: true }, () => this._playerObject.playVideo());
  }

  @autobind
  pause() {
    this._playerObject.pauseVideo();
  }

  render() {
    const cls = classNames(styles.youtube, {
      [styles._loaded]: this.state.apiLoaded,
      [styles._playing]: this.state.playing,
      [styles._faded]: this.props.fadedAtStart,
    }, this.props.className);

    const playButtonCls = classNames(styles.play, this.props.playButtonClassName);

    return (
      <div className={cls}>
        <div
          styleName="player"
          ref={(r) => {
            this._playerElement = r;
          }}
        />
        <button className={playButtonCls} onClick={this.play} />
      </div>
    );
  }
}
