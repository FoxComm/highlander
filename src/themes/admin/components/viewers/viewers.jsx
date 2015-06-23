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
      viewers: []
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
    this.setState({viewers: viewers});
  }

  render() {
    let viewers = this.state.viewers;

    return (
      <ul className="viewers">
        {viewers.map((viewer) => {
          return <li key={viewer.id}><UserInitials model={viewer}/></li>;
        })}
      </ul>
    );
  }
}

Viewers.propTypes = {
  model: React.PropTypes.string,
  modelId: React.PropTypes.string
};
