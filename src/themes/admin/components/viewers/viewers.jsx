'use strict';

import React from 'react';
import UserInitials from '../users/initials';
import ViewerStore from './store';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const
  changeEvent = 'change-viewer-store',
  updateTime  = 15000;

export default class Viewers extends React.Component {
  constructor(props) {
    super(props);
    ViewerStore.uriRoot = `${this.props.model}/${this.props.modelId}`;
    this.onChangeViewerStore = this.onChangeViewerStore.bind(this);
    this.state = {
      viewers: [],
      lockedViewers: []
    };
  }

  componentDidMount() {
    listenTo(changeEvent, this);
    this.onTimeout();
  }

  componentWillUnmount() {
    stopListeningTo(changeEvent, this);
  }

  onTimeout() {
    ViewerStore.fetch();
    setTimeout(() => { this.onTimeout(); }, updateTime);
  }

  onChangeViewerStore(viewers) {
    this.setState({
      viewers: viewers.filter((viewer) => { return viewer.isLocker === false; }),
      lockedViewers: viewers.filter((viewer) => { return viewer.isLocker === true; })
    });
  }

  render() {
    let
      viewers = this.state.viewers,
      lockedViewers = this.state.lockedViewers,
      lockedContent = null;

    let viewerItem = (viewer) => {
      return <li key={viewer.id}><UserInitials model={viewer}/></li>;
    };

    if (lockedViewers.length > 0) {
      lockedContent = (
        <ul>
          <li className="lock"><i className='icon-lock'></i></li>
          {lockedViewers.map(viewerItem)}
        </ul>
      );
    }

    return (
      <div className="viewers">
        <ul>
          {viewers.map(viewerItem)}
        </ul>
        {lockedContent}
      </div>
    );
  }
}

Viewers.propTypes = {
  model: React.PropTypes.string,
  modelId: React.PropTypes.string
};
